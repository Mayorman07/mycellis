package com.mycelis.user.service;

import com.mycelis.user.model.request.ChangeEmailRequest;
import com.mycelis.user.model.request.ChangePasswordRequest;
import com.mycelis.user.model.request.ForgotPasswordRequest;
import com.mycelis.user.model.request.LoginRequest;
import com.mycelis.user.model.request.ResetPasswordRequest;
import com.mycelis.user.model.response.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    void verifyUser(String token);                          // email verification

    void requestPasswordReset(ForgotPasswordRequest request); // email-based (pre-auth)

    void resetPassword(ResetPasswordRequest request);         // token-based (pre-auth)

    void changePassword(String userId, ChangePasswordRequest request);  // authenticated

    void changeEmail(String userId, ChangeEmailRequest request);        // authenticated
}