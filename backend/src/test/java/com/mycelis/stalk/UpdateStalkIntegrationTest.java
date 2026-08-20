package com.mycelis.stalk;

import com.mycelis.IntegrationTestBase;
import com.mycelis.monitoring.dto.requests.CreateStalkRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UpdateStalkIntegrationTest extends IntegrationTestBase {

    @Test
    void updatingToAnotherStalksUrlIsRejectedWithConflict() throws Exception {
        MockHttpSession session = login(createVerifiedUser("dup-url-update"));
        createStalk(session, validRequest("https://example.com/first"));
        UUID secondId = createStalk(session, validRequest("https://example.com/second"));

        mockMvc.perform(put("/api/stalks/{id}", secondId)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("https://example.com/first"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("A stalk with this URL already exists in your organization"))
                .andExpect(jsonPath("$.type").value("https://mycellis.dev/errors/duplicate-url"));
    }

    @Test
    void updatingAStalkWithItsOwnUnchangedUrlSucceeds() throws Exception {
        MockHttpSession session = login(createVerifiedUser("update-same-url"));
        UUID stalkId = createStalk(session, validRequest("https://example.com/unchanged"));

        CreateStalkRequest request = validRequest("https://example.com/unchanged");
        request.setNickname("Renamed");

        mockMvc.perform(put("/api/stalks/{id}", stalkId)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("Renamed"));
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
