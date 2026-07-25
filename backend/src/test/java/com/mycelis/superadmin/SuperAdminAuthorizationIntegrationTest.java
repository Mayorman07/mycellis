package com.mycelis.superadmin;

import com.mycelis.IntegrationTestBase;
import com.mycelis.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Validates the super-admin authorization gate (Commit 18): the
 * SuperAdminSecurity bean gating each endpoint, and the GlobalExceptionHandler
 * fix that made @PreAuthorize denials return 403 instead of falling through
 * to a 500 (a pre-existing bug affecting every method-security check, not
 * just these endpoints — this suite is the regression test for it).
 */
class SuperAdminAuthorizationIntegrationTest extends IntegrationTestBase {

    @Test
    void regularUserGetsForbiddenOnOrganizations() throws Exception {
        MockHttpSession session = login(createVerifiedUser("regular-orgs"));

        mockMvc.perform(get("/api/super-admin/organizations").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void regularUserGetsForbiddenOnUsers() throws Exception {
        MockHttpSession session = login(createVerifiedUser("regular-users"));

        mockMvc.perform(get("/api/super-admin/users").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void regularUserGetsForbiddenOnOrgStalks() throws Exception {
        MockHttpSession session = login(createVerifiedUser("regular-stalks"));

        mockMvc.perform(get("/api/super-admin/organizations/{orgId}/stalks", UUID.randomUUID()).session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void superAdminGetsOkOnOrganizations() throws Exception {
        MockHttpSession session = loginAsSuperAdmin("super-orgs");

        mockMvc.perform(get("/api/super-admin/organizations").session(session))
                .andExpect(status().isOk());
    }

    @Test
    void superAdminGetsOkOnUsers() throws Exception {
        MockHttpSession session = loginAsSuperAdmin("super-users");

        mockMvc.perform(get("/api/super-admin/users").session(session))
                .andExpect(status().isOk());
    }

    @Test
    void superAdminGetsOkOnOrgStalks() throws Exception {
        TestUser target = createVerifiedUser("super-stalks-target");
        MockHttpSession session = loginAsSuperAdmin("super-stalks");

        User targetUser = userRepository.findById(target.id()).orElseThrow();

        mockMvc.perform(get("/api/super-admin/organizations/{orgId}/stalks", targetUser.getOrganizationId())
                        .session(session))
                .andExpect(status().isOk());
    }

    @Test
    void unauthenticatedRequestToOrganizationsIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/super-admin/organizations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedRequestToUsersIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/super-admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedRequestToOrgStalksIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/super-admin/organizations/{orgId}/stalks", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    private MockHttpSession loginAsSuperAdmin(String label) throws Exception {
        TestUser user = createVerifiedUser(label);
        User entity = userRepository.findById(user.id()).orElseThrow();
        entity.setSuperAdmin(true);
        userRepository.save(entity);
        return login(user);
    }
}
