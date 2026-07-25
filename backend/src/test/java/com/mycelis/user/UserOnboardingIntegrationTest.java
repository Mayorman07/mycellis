package com.mycelis.user;

import com.mycelis.IntegrationTestBase;
import com.mycelis.membership.entity.Membership;
import com.mycelis.membership.repository.MembershipRepository;
import com.mycelis.organization.entity.Organization;
import com.mycelis.organization.repository.OrganizationRepository;
import com.mycelis.user.constant.Status;
import com.mycelis.user.entity.User;
import com.mycelis.user.model.request.CreateUserRequest;
import com.mycelis.user.model.request.LoginRequest;
import com.mycelis.user.model.request.VerifyEmailRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Walks the full signup -> verify -> login pipeline through real HTTP calls,
 * checking the DB invariants the memberships refactor (Commit 7) promises at
 * each step.
 *
 * <p>The "login before verification" scenario runs before the verify call,
 * not after — the task's scenario list ordered it after the DB checks that
 * follow the verify call, which is impossible to observe (the account is
 * already ACTIVE by then). Reordered here to the only sequence that can
 * actually exercise that path.</p>
 */
class UserOnboardingIntegrationTest extends IntegrationTestBase {

    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private MembershipRepository membershipRepository;

    @Test
    void fullOnboardingPipelineFromSignupThroughVerifyToLogin() throws Exception {
        String email = uniqueEmail("onboarding");
        String orgName = "Onboarding Org " + UUID.randomUUID();
        CreateUserRequest createRequest = new CreateUserRequest(
                "Ada", "Lovelace", email, DEFAULT_PASSWORD,
                "PREFER_NOT_TO_SAY", "+15555550101", orgName);

        // 1. signup -> 201 with user response
        MvcResult createResult = mockMvc.perform(post("/api/users/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andReturn();
        UUID userId = extractId(createResult);

        // 2. user/org/membership created atomically, OWNER + primary
        User createdUser = userRepository.findById(userId).orElseThrow();
        assertThat(createdUser.getOrganizationId()).isNotNull();

        Organization org = organizationRepository.findById(createdUser.getOrganizationId()).orElseThrow();
        assertThat(org.getName()).isEqualTo(orgName);

        Membership membership = membershipRepository.findByUserIdAndIsPrimaryTrue(userId).orElseThrow();
        assertThat(membership.getOrganizationId()).isEqualTo(org.getId());
        assertThat(membership.getRole().name()).isEqualTo("OWNER");
        assertThat(membership.isPrimary()).isTrue();

        // 3. verification token set, status NEW
        assertThat(createdUser.getVerificationToken()).isNotBlank();
        assertThat(createdUser.getStatus()).isEqualTo(Status.NEW);

        // 7. login before verification -> 403, Account Not Verified
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, DEFAULT_PASSWORD, null))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Account Not Verified"));

        // 4/5. verify with the extracted token -> 204
        String token = createdUser.getVerificationToken();
        mockMvc.perform(post("/api/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerifyEmailRequest(token))))
                .andExpect(status().isNoContent());

        // 6. token cleared, status ACTIVE
        User verifiedUser = userRepository.findById(userId).orElseThrow();
        assertThat(verifiedUser.getVerificationToken()).isNull();
        assertThat(verifiedUser.getStatus()).isEqualTo(Status.ACTIVE);

        // 8. correct login after verification -> 200 + authenticated session
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, DEFAULT_PASSWORD, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();
        assertThat(session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)).isNotNull();

        // 9. wrong password after verification -> 401
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "WrongPassword123!", null))))
                .andExpect(status().isUnauthorized());
    }
}
