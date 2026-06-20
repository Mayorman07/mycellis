package com.mycelis.shared.identity;

public interface IdGenerator {

    /** Public user identifier (UUID, with dashes, no prefix). */
    String newUserId();

    /** Email verification token: vrf_<32 hex chars>. */
    String newVerificationToken();

    /** Password reset token: pwr_<32 hex chars>. */
    String newPasswordResetToken();
}