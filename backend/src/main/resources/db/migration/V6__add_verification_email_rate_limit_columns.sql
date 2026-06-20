ALTER TABLE users
    ADD COLUMN last_verification_email_sent_at TIMESTAMP WITH TIME ZONE NULL,
    ADD COLUMN verification_email_count_today INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN verification_email_count_window_start TIMESTAMP WITH TIME ZONE NULL;

COMMENT ON COLUMN users.last_verification_email_sent_at IS 'When the most recent verification email was sent. Used for cooldown check.';
COMMENT ON COLUMN users.verification_email_count_today IS 'How many verification emails sent in the current 24h window.';
COMMENT ON COLUMN users.verification_email_count_window_start IS 'Start of the current counting window. When 24h elapses, counter resets.';