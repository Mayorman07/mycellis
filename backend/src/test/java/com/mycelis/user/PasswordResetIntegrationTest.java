package com.mycelis.user;

import com.mycelis.IntegrationTestBase;
import com.mycelis.user.entity.User;
import com.mycelis.user.model.request.ForgotPasswordRequest;
import com.mycelis.user.model.request.LoginRequest;
import com.mycelis.user.model.request.ResetPasswordRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Password reset: anti-enumeration on request, and the full
 * request -> consume-token -> reset -> reuse-fails flow. Scenarios 3-8 from
 * the commit spec are inherently sequential (can't test token reuse without
 * first consuming it), so they're one flow test rather than artificially
 * split into isolated methods that would each have to redo every prior step.
 */
class PasswordResetIntegrationTest extends IntegrationTestBase {

    @Test
    void forgotPasswordWithRegisteredEmailIsAccepted() throws Exception {
        TestUser user = createVerifiedUser("reset-registered");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(user.email()))))
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));
    }

    @Test
    void forgotPasswordWithUnregisteredEmailIsAlsoAcceptedAntiEnumeration() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ForgotPasswordRequest(uniqueEmail("never-registered")))))
                .andExpect(status().isAccepted());
    }

    @Test
    void fullResetFlowChangesPasswordConsumesTokenAndRejectsReuse() throws Exception {
        TestUser user = createVerifiedUser("reset-flow");
        String hashBefore = userRepository.findById(user.id()).orElseThrow().getEncryptedPassword();

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest(user.email()))))
                .andExpect(status().isAccepted());

        User afterRequest = userRepository.findById(user.id()).orElseThrow();
        String token = afterRequest.getPasswordResetToken();
        assertThat(token).isNotBlank();

        String newPassword = "BrandNewPass456!";
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordRequest(token, newPassword))))
                .andExpect(status().isNoContent());

        User afterReset = userRepository.findById(user.id()).orElseThrow();
        assertThat(afterReset.getEncryptedPassword()).isNotEqualTo(hashBefore);
        assertThat(afterReset.getPasswordResetToken()).isNull();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(user.email(), user.password(), null))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(user.email(), newPassword, null))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ResetPasswordRequest(token, "AnotherPass789!"))))
                .andExpect(status().isNotFound());
    }
}
