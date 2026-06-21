package com.mycelis.shared.identity;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UuidIdGenerator implements IdGenerator {

    private static final String VERIFICATION_PREFIX = "vrf_";
    private static final String PASSWORD_RESET_PREFIX = "pwr_";

    @Override
    public String newVerificationToken() {
        return VERIFICATION_PREFIX + compactUuid();
    }

    @Override
    public String newPasswordResetToken() {
        return PASSWORD_RESET_PREFIX + compactUuid();
    }

    /** UUID with dashes stripped — cleaner inside prefixed tokens. */
    private String compactUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}