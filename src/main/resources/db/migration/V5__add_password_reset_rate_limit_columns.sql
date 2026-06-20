-- Rate-limit tracking for password reset email requests.
-- Both columns are nullable: existing users have no history yet.

ALTER TABLE users
    ADD COLUMN last_password_reset_email_sent_at TIMESTAMP WITH TIME ZONE NULL,
    ADD COLUMN password_reset_email_count_today INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN password_reset_email_count_window_start TIMESTAMP WITH TIME ZONE NULL;

COMMENT ON COLUMN users.last_password_reset_email_sent_at IS 'When the most recent reset email was sent. Used for cooldown check.';
COMMENT ON COLUMN users.password_reset_email_count_today IS 'How many reset emails sent in the current 24h window.';
COMMENT ON COLUMN users.password_reset_email_count_window_start IS 'Start of the current counting window. When 24h elapses, counter resets.';