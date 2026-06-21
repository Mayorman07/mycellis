package com.mycelis.shared.identity;

public interface IdGenerator {

    /** Email verification token: vrf_<32 hex chars>. */
    String newVerificationToken();

    /** Password reset token: pwr_<32 hex chars>. */
    String newPasswordResetToken();
}