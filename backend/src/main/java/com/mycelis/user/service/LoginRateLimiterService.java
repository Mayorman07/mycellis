package com.mycelis.user.service;

import com.mycelis.user.entity.LoginAttempt;
import com.mycelis.user.repository.LoginAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Per-(email, ip) login rate limiting. Tracks failed attempts in the database.
 * See {@link #isRateLimited} for the policy.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginRateLimiterService {

    static final int MAX_FAILED_ATTEMPTS = 5;
    static final Duration LOCKOUT_WINDOW = Duration.ofMinutes(15);
    static final Duration RETENTION_WINDOW = Duration.ofHours(24);

    private final LoginAttemptRepository loginAttemptRepository;

    /**
     * Returns true if the (email, ip) pair has hit the threshold within the lockout window.
     */
    @Transactional(readOnly = true)
    public boolean isRateLimited(String email, String ipAddress) {
        Instant windowStart = Instant.now().minus(LOCKOUT_WINDOW);
        long failures = loginAttemptRepository.countRecentFailures(
                normalize(email), ipAddress, windowStart);
        return failures >= MAX_FAILED_ATTEMPTS;
    }

    /**
     * Record a failed attempt. Called whenever credentials fail to verify.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String email, String ipAddress) {
        LoginAttempt attempt = LoginAttempt.builder()
                .email(normalize(email))
                .ipAddress(ipAddress)
                .failedAt(Instant.now())
                .build();
        loginAttemptRepository.save(attempt);
    }

    /**
     * Clear all failures for a (email, ip) pair on successful login.
     * Honest users who fumble-fingered should not retain a stale failure trail.
     */
    @Transactional
    public void clearFailures(String email, String ipAddress) {
        loginAttemptRepository.deleteByEmailAndIp(normalize(email), ipAddress);
    }

    /**
     * Hourly cleanup of stale rows. Keeps the table bounded.
     */
    @Scheduled(fixedDelay = 3_600_000, initialDelay = 60_000)  // 1 hour, after 1 min warmup
    @Transactional
    void purgeStaleAttempts() {
        Instant cutoff = Instant.now().minus(RETENTION_WINDOW);
        int deleted = loginAttemptRepository.deleteOlderThan(cutoff);
        if (deleted > 0) {
            log.info("Purged {} stale login_attempts rows older than {}", deleted, cutoff);
        }
    }

    /**
     * Normalize email for consistent lookups — lowercase, trim.
     */
    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}