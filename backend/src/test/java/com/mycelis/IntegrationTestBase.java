package com.mycelis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycelis.user.entity.User;
import com.mycelis.user.model.request.CreateUserRequest;
import com.mycelis.user.model.request.LoginRequest;
import com.mycelis.user.model.request.VerifyEmailRequest;
import com.mycelis.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Base for HTTP-level integration tests. No Testcontainers/H2 infrastructure
 * exists in this project (see MembershipMigrationIntegrationTest's javadoc,
 * predating this commit) — every prior integration test runs against the
 * real dev Postgres database with @Transactional rollback per test, so this
 * base follows the same convention rather than introducing a second one.
 *
 * <p>The dev profile is active by default (spring.profiles.active=dev), which
 * makes InitialDataSeeder run on every @SpringBootTest context boot and read
 * ${MYCELIS_ADMIN_EMAIL}-family placeholders that have no default — without
 * an override, context startup fails when those OS env vars aren't set. The
 * @DynamicPropertySource below supplies dummy values so the context always
 * boots. Because the seeder runs at ApplicationReadyEvent time (before any
 * @Test's transaction starts), it leaves a one-time, non-rolled-back
 * test-admin@example.test super admin in the real dev DB the first time this
 * suite runs anywhere — idempotent (skipped) on every run after that.</p>
 *
 * <p>mycelis.alerts.scheduling.enabled=false disables AlertEngine's
 * @Scheduled tick for the whole test context — without it, every class
 * extending this base boots a real, ticking AlertEngine against the shared
 * dev DB, firing real DOWN/RECOVERY emails against whatever real stalks
 * happen to have failing pulse history at the time, as a side effect
 * completely unrelated to whatever a given test is actually checking.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public abstract class IntegrationTestBase {

    protected static final String DEFAULT_PASSWORD = "SecurePass123!";

    @Autowired
    protected MockMvc mockMvc;

    // Not @Autowired: no ObjectMapper bean is registered in this app context
    // (spring-boot-starter-web/webmvc here doesn't pull in Jackson
    // autoconfiguration — a separate, pre-existing gap, not something this
    // commit's tests need to fix). Every use here is either writing
    // plain-value request records or reading generic JsonNode trees, so a
    // bare instance is sufficient.
    protected final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    protected UserRepository userRepository;

    @DynamicPropertySource
    static void registerSeedProperties(DynamicPropertyRegistry registry) {
        registry.add("mycelis.seed.super-admin.email", () -> "test-admin@example.test");
        registry.add("mycelis.seed.super-admin.password", () -> "TestAdminPass123!");
        registry.add("mycelis.seed.super-admin.mobile", () -> "+15555559999");
        registry.add("mycelis.alerts.scheduling.enabled", () -> "false");
    }

    protected String uniqueEmail(String label) {
        return label + "-" + UUID.randomUUID() + "@example.test";
    }

    /** Signs up a user + org through the real HTTP endpoint, then verifies the account. */
    protected TestUser createVerifiedUser(String label) throws Exception {
        String email = uniqueEmail(label);
        String orgName = "Org " + label + " " + UUID.randomUUID();

        CreateUserRequest request = new CreateUserRequest(
                "Test", "User", email, DEFAULT_PASSWORD,
                "PREFER_NOT_TO_SAY", "+15555550100", orgName);

        MvcResult result = mockMvc.perform(post("/api/users/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID userId = extractId(result);
        User user = userRepository.findById(userId).orElseThrow();
        String token = user.getVerificationToken();

        mockMvc.perform(post("/api/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyEmailRequest(token))))
                .andExpect(status().isNoContent());

        return new TestUser(userId, email, DEFAULT_PASSWORD, orgName);
    }

    protected MockHttpSession login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password, null))))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    protected MockHttpSession login(TestUser user) throws Exception {
        return login(user.email(), user.password());
    }

    protected UUID extractId(MvcResult result) throws Exception {
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    /** Reads the ids out of a paginated {@code $.content[]} response body. */
    protected List<String> extractContentIds(MvcResult result) throws Exception {
        JsonNode content = objectMapper.readTree(result.getResponse().getContentAsString()).get("content");
        List<String> ids = new ArrayList<>();
        content.forEach(node -> ids.add(node.get("id").asText()));
        return ids;
    }

    protected record TestUser(UUID id, String email, String password, String orgName) {}
}
