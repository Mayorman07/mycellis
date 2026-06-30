-- V9: Track failed login attempts for rate limiting.
--
-- Goal: throttle brute-force and credential-stuffing attacks against /api/auth/login.
-- We key on (email, ip_address) so:
--   - An attacker on one IP can't lock a user out by hammering their email.
--   - A botnet across many IPs is still rate-limited per (email, ip) pair.
--   - We don't reveal whether an email exists (failed attempts are tracked
--     regardless of whether the email maps to a real user).

CREATE TABLE login_attempts (
                                id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                email       VARCHAR(255) NOT NULL,
                                ip_address  VARCHAR(45) NOT NULL,           -- IPv6 max is 45 chars
                                failed_at   TIMESTAMPTZ NOT NULL,
                                CONSTRAINT chk_login_attempts_email_lower CHECK (email = LOWER(email))
);

-- Primary lookup: count failures for a specific (email, ip) within a time window.
CREATE INDEX idx_login_attempts_email_ip_failed_at
    ON login_attempts (email, ip_address, failed_at DESC);

-- Cleanup job: delete rows older than retention window.
CREATE INDEX idx_login_attempts_failed_at
    ON login_attempts (failed_at);