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
 * no URL-format validator anywhere in the codebase, so "invalid URL rejected"
 * is tested here as a blank url (the actual validation that exists), not a
 * malformed-but-non-blank string like "not-a-url" (which the API accepts as
 * written today; that's a real gap, flagged in the commit report, not one
 * this test suite silently papers over).</p>
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
                        .content(objectMapper.writeValueAsString(validRequest("https://happy-path.example.test"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.url").value("https://happy-path.example.test"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.currentState").value("HEALTHY"));
    }

    @Test
    void listStalksIncludesTheNewStalk() throws Exception {
        MockHttpSession session = login(createVerifiedUser("create-list"));
        UUID stalkId = createStalk(session, validRequest("https://list-check.example.test"));

        MvcResult result = mockMvc.perform(get("/api/stalks").session(session))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(extractContentIds(result)).contains(stalkId.toString());
    }

    @Test
    void getStalkByIdReturnsCorrectFields() throws Exception {
        MockHttpSession session = login(createVerifiedUser("create-get"));
        CreateStalkRequest request = validRequest("https://field-check.example.test");
        request.setNickname("Field Check");
        request.setGrowthIntervalSeconds(120);
        request.setTimeoutSeconds(15);

        UUID stalkId = createStalk(session, request);

        mockMvc.perform(get("/api/stalks/{id}", stalkId).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://field-check.example.test"))
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
