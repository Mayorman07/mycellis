package com.mycelis.user.service;

import com.mycelis.user.model.request.*;
import com.mycelis.user.model.response.LoginResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse);

    void verifyUser(String token);                          // email verification

    void requestPasswordReset(ForgotPasswordRequest request); // email-based (pre-auth)

    void resetPassword(ResetPasswordRequest request);         // token-based (pre-auth)

    void changePassword(String userId, ChangePasswordRequest request);  // authenticated

    void changeEmail(String userId, ChangeEmailRequest request);// authenticated

    void resendVerification(ResendVerificationRequest request);
}