package com.mycelis.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Crypto primitives used across the application.
 *
 * <p>Separated from {@link SecurityConfig} to avoid circular dependencies —
 * services like {@code MycelisUserDetailsService} need {@link PasswordEncoder},
 * and {@code SecurityConfig} needs those services. Keeping the encoder in
 * its own config breaks the cycle.</p>
 */
@Configuration
public class CryptoConfig {

    /**
     * BCrypt with cost factor 12. Cost 12 takes ~250ms per hash on modern CPUs —
     * deliberately expensive to slow down offline brute force against leaked hashes.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}