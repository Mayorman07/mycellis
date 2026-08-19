package com.mycelis.security;

import com.mycelis.IntegrationTestBase;
import com.mycelis.monitoring.dto.requests.CreateStalkRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the tenant-isolation contract the memberships refactor (Commit 7)
 * is supposed to guarantee: a user authenticated into organization A can
 * never read, list, modify, or delete a resource belonging to organization
 * B, even knowing its id. StalkServiceImpl/PulseServiceImpl enforce this by
 * throwing TenantAccessException, which GlobalExceptionHandler maps to 403 —
 * not the 400 "missing stalk" case (that's IllegalArgumentException/
 * ResourceNotFoundException for a genuinely nonexistent id, a different path
 * from a real id belonging to someone else).
 */
class TenantIsolationIntegrationTest extends IntegrationTestBase {

    @Test
    void userBCannotGetUserAsStalkById() throws Exception {
        MockHttpSession sessionA = login(createVerifiedUser("tenant-a-get"));
        MockHttpSession sessionB = login(createVerifiedUser("tenant-b-get"));
        UUID stalkId = createStalk(sessionA, validRequest("https://example.com/tenant-a-get"));

        mockMvc.perform(get("/api/stalks/{id}", stalkId).session(sessionB))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Tenant Access Denied"));
    }

    @Test
    void userBsStalkListDoesNotIncludeUserAsStalk() throws Exception {
        MockHttpSession sessionA = login(createVerifiedUser("tenant-a-list"));
        MockHttpSession sessionB = login(createVerifiedUser("tenant-b-list"));
        UUID stalkId = createStalk(sessionA, validRequest("https://example.com/tenant-a-list"));

        MvcResult result = mockMvc.perform(get("/api/stalks").session(sessionB))
                .andExpect(status().isOk())
                .andReturn();

        List<String> visibleIds = extractContentIds(result);
        assertThat(visibleIds).doesNotContain(stalkId.toString());
    }

    @Test
    void userBCannotUpdateUserAsStalk() throws Exception {
        MockHttpSession sessionA = login(createVerifiedUser("tenant-a-put"));
        MockHttpSession sessionB = login(createVerifiedUser("tenant-b-put"));
        UUID stalkId = createStalk(sessionA, validRequest("https://example.com/tenant-a-put"));

        mockMvc.perform(put("/api/stalks/{id}", stalkId)
                        .session(sessionB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("https://example.com/hijacked"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void userBCannotDeleteUserAsStalk() throws Exception {
        MockHttpSession sessionA = login(createVerifiedUser("tenant-a-delete"));
        MockHttpSession sessionB = login(createVerifiedUser("tenant-b-delete"));
        UUID stalkId = createStalk(sessionA, validRequest("https://example.com/tenant-a-delete"));

        mockMvc.perform(delete("/api/stalks/{id}", stalkId).session(sessionB))
                .andExpect(status().isForbidden());
    }

    @Test
    void userBCannotFetchUserAsPulses() throws Exception {
        MockHttpSession sessionA = login(createVerifiedUser("tenant-a-pulses"));
        MockHttpSession sessionB = login(createVerifiedUser("tenant-b-pulses"));
        UUID stalkId = createStalk(sessionA, validRequest("https://example.com/tenant-a-pulses"));

        mockMvc.perform(get("/api/stalks/{stalkId}/pulses", stalkId).session(sessionB))
                .andExpect(status().isForbidden());
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
