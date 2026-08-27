package com.mycelis.stalk;

import com.mycelis.IntegrationTestBase;
import com.mycelis.monitoring.constant.LatencyState;
import com.mycelis.monitoring.constant.ReliabilityState;
import com.mycelis.monitoring.constant.StalkState;
import com.mycelis.monitoring.dto.requests.CreateStalkRequest;
import com.mycelis.monitoring.entity.Stalk;
import com.mycelis.monitoring.repository.StalkRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Happy-path + validation-boundary coverage for stalk creation.
 *
 * <p>CreateStalkRequest.url only carries @NotBlank/@Size(max=2048) — there is
 * no URL-*format* validator (malformed-but-non-blank strings like "not-a-url"
 * still reach the service layer), so "invalid URL rejected" is tested here as
 * a blank url. SafeUrlValidator, exercised below via the private-URL test,
 * covers a different concern — URLs that are syntactically fine but resolve
 * to an internal/private network target.</p>
 *
 * <p>The timeout ceiling task-described as "> 30 rejected" comes from
 * MonitoringProperties.maxCycleDuration (mycelis.monitoring.max-cycle-duration
 * = PT30S in application.properties), enforced at the service layer — a
 * separate, stricter check than the DTO's own @Max(120).</p>
 */
class CreateStalkIntegrationTest extends IntegrationTestBase {

    @Autowired
    private StalkRepository stalkRepository;

    @Test
    void createStalkReturnsCreatedWithStalkResponse() throws Exception {
        MockHttpSession session = login(createVerifiedUser("create-happy"));

        mockMvc.perform(post("/api/stalks")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("https://example.com/happy-path"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.url").value("https://example.com/happy-path"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.currentState").value("DORMANT"));
    }

    @Test
    void listStalksIncludesTheNewStalk() throws Exception {
        MockHttpSession session = login(createVerifiedUser("create-list"));
        UUID stalkId = createStalk(session, validRequest("https://example.com/list-check"));

        MvcResult result = mockMvc.perform(get("/api/stalks").session(session))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(extractContentIds(result)).contains(stalkId.toString());
    }

    @Test
    void getStalkByIdReturnsCorrectFields() throws Exception {
        MockHttpSession session = login(createVerifiedUser("create-get"));
        CreateStalkRequest request = validRequest("https://example.com/field-check");
        request.setNickname("Field Check");
        request.setGrowthIntervalSeconds(120);
        request.setTimeoutSeconds(15);

        UUID stalkId = createStalk(session, request);

        mockMvc.perform(get("/api/stalks/{id}", stalkId).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://example.com/field-check"))
                .andExpect(jsonPath("$.nickname").value("Field Check"))
                .andExpect(jsonPath("$.growthIntervalSeconds").value(120))
                .andExpect(jsonPath("$.timeoutSeconds").value(15))
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    void blankUrlIsRejected() throws Exception {
        MockHttpSession session = login(createVerifiedUser("create-blank-url"));

        mockMvc.perform(post("/api/stalks")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest(""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void timeoutBelowFiveSecondsIsRejected() throws Exception {
        MockHttpSession session = login(createVerifiedUser("create-timeout-low"));
        CreateStalkRequest request = validRequest("https://timeout-low.example.test");
        request.setTimeoutSeconds(4);

        mockMvc.perform(post("/api/stalks")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    @Test
    void timeoutAboveThirtySecondsIsRejected() throws Exception {
        MockHttpSession session = login(createVerifiedUser("create-timeout-high"));
        CreateStalkRequest request = validRequest("https://timeout-high.example.test");
        request.setTimeoutSeconds(35);

        mockMvc.perform(post("/api/stalks")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    @Test
    void privateUrlIsRejectedWithUnsafeUrlProblemDetail() throws Exception {
        MockHttpSession session = login(createVerifiedUser("create-unsafe-url"));

        mockMvc.perform(post("/api/stalks")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("http://127.0.0.1"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("URLs pointing to internal addresses are not allowed"))
                .andExpect(jsonPath("$.type").value("https://mycellis.dev/errors/unsafe-url"));
    }

    @Test
    void duplicateUrlIsRejectedWithConflict() throws Exception {
        MockHttpSession session = login(createVerifiedUser("dup-url-create"));
        createStalk(session, validRequest("https://example.com/dup-check"));

        // Different case + explicit default port — proves normalization is
        // actually applied, not a literal string match.
        mockMvc.perform(post("/api/stalks")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("HTTPS://EXAMPLE.COM:443/dup-check"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("A stalk with this URL already exists in your organization"))
                .andExpect(jsonPath("$.type").value("https://mycellis.dev/errors/duplicate-url"));
    }

    /**
     * Seeds 19 stalks directly via the repository rather than 19+ HTTP calls.
     * This test verifies quota enforcement, not stalk creation itself, and
     * rapid-fire HTTP POSTs would trip PR #5's 5/minute rate limit long
     * before reaching the 20-stalk boundary — the two features now share the
     * same endpoint. Pattern going forward: an integration test that needs
     * to reach a state faster than rate limits allow should seed setup state
     * via the repository directly, reserving real HTTP calls for the
     * specific behavior under test (here: the 20th succeeds, the 21st 409s).
     */
    @Test
    void twentyFirstStalkIsRejectedWithQuotaExceeded() throws Exception {
        IntegrationTestBase.TestUser testUser = createVerifiedUser("quota-cap");
        MockHttpSession session = login(testUser);
        UUID organizationId = userRepository.findById(testUser.id()).orElseThrow().getOrganizationId();

        for (int i = 0; i < 19; i++) {
            seedStalkDirectly(organizationId, testUser.id(), "https://example.com/quota-seed-" + i);
        }

        // 20th stalk: real HTTP call, well within the rate limit, should succeed.
        createStalk(session, validRequest("https://example.com/quota-20"));

        // 21st stalk: real HTTP call — org is now at its cap, should 409.
        // Only 2 HTTP calls made in this test in total, nowhere near the
        // 5/minute rate limit, so this is unambiguously the quota check.
        mockMvc.perform(post("/api/stalks")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("https://example.com/quota-21"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "Free tier includes up to 20 stalks. Delete unused stalks or contact support to upgrade."))
                .andExpect(jsonPath("$.type").value("https://mycellis.dev/errors/stalk-quota-exceeded"));
    }

    /**
     * Verifies the filter's HTTP-level behavior via MockMvc — but passing
     * here does NOT by itself prove rate limiting works in prod, and it
     * didn't: this test (and Bucket4jRateLimitFilterTest) passed
     * continuously through the entire window PR #5's rate limiter was
     * silently non-functional in production, because neither test path goes
     * through Spring Boot's real generic servlet-filter auto-registration
     * the way a live deployment does. Prod correctness depends on
     * SecurityConfig's bucket4jRateLimitFilterRegistration bean
     * (FilterRegistrationBean with setEnabled(false)) continuing to exist —
     * no test in this suite can catch its removal.
     *
     * <p>The CORS/security header assertions below are a third such
     * prod-only bug's regression test: Bucket4jRateLimitFilter was anchored
     * via {@code .addFilterAfter(..., SecurityContextHolderFilter.class)},
     * placing it before both CorsFilter and HeaderWriterFilter in Spring
     * Security's chain. Since this filter short-circuits on rejection
     * without calling {@code filterChain.doFilter(...)}, and neither of
     * those two filters has a post-processing leg, a 429 response shipped
     * with no CORS headers and no security headers at all — browsers
     * refused to expose the response body to JS. The header values below
     * were captured from this app's actual 201 responses (see
     * SecurityConfig; no custom {@code .headers(...)} customizer exists, so
     * these are Spring Security's stock defaults) and must also appear on
     * the 429. An {@code Origin} header is required on the request for
     * CorsFilter to add its headers at all — real browsers always send one
     * on cross-origin requests.</p>
     */
    @Test
    void sixthStalkCreationWithinAMinuteIsRateLimited() throws Exception {
        MockHttpSession session = login(createVerifiedUser("rate-limit-cap"));

        for (int i = 0; i < 5; i++) {
            createStalk(session, validRequest("https://example.com/rate-limit-" + i));
        }

        mockMvc.perform(post("/api/stalks")
                        .header("Origin", "http://localhost:5173")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("https://example.com/rate-limit-5"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().contentType("application/problem+json;charset=UTF-8"))
                .andExpect(jsonPath("$.detail").value("You've created stalks too quickly."))
                .andExpect(jsonPath("$.type").value("https://mycellis.dev/errors/rate-limit-exceeded"))
                .andExpect(header().exists("Retry-After"))
                // CORS headers — missing these broke the frontend's live countdown UI in prod.
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                // HeaderWriterFilter defaults — mirrors what the 201 responses above include.
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-XSS-Protection", "0"))
                .andExpect(header().string("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate"))
                .andExpect(header().string("Pragma", "no-cache"))
                .andExpect(header().string("Expires", "0"));
    }

    private CreateStalkRequest validRequest(String url) {
        CreateStalkRequest request = new CreateStalkRequest();
        request.setUrl(url);
        request.setGrowthIntervalSeconds(60);
        request.setTimeoutSeconds(10);
        return request;
    }

    private UUID createStalk(MockHttpSession session, CreateStalkRequest request) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/stalks")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return extractId(result);
    }

    /** Bypasses HTTP (and therefore the rate limiter) for tests that just need N pre-existing stalks. */
    private void seedStalkDirectly(UUID organizationId, UUID createdByUserId, String url) {
        Stalk stalk = Stalk.builder()
                .organizationId(organizationId)
                .createdByUserId(createdByUserId)
                .url(url)
                .normalizedUrl(url)
                .growthIntervalSeconds(60)
                .timeoutSeconds(10)
                .currentState(StalkState.DORMANT)
                .reliabilityState(ReliabilityState.AWAKENING)
                .latencyState(LatencyState.NORMAL)
                .healthIndex(0.0)
                .consecutiveFailures(0)
                .isActive(true)
                .nextCheckAt(Instant.now())
                .lastActivatedAt(Instant.now())
                .build();
        stalkRepository.save(stalk);
    }
}
