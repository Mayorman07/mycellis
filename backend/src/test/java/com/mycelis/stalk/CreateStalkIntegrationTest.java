package com.mycelis.stalk;

import com.mycelis.IntegrationTestBase;
import com.mycelis.monitoring.dto.requests.CreateStalkRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @Test
    void twentyFirstStalkIsRejectedWithQuotaExceeded() throws Exception {
        MockHttpSession session = login(createVerifiedUser("quota-cap"));

        for (int i = 0; i < 20; i++) {
            createStalk(session, validRequest("https://example.com/quota-" + i));
        }

        mockMvc.perform(post("/api/stalks")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("https://example.com/quota-21"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "Free tier includes up to 20 stalks. Delete unused stalks or contact support to upgrade."))
                .andExpect(jsonPath("$.type").value("https://mycellis.dev/errors/stalk-quota-exceeded"));
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
}
