-- V12: Minimum viable alerting — DOWN/RECOVERY email alerts per stalk.
--
-- alerts is the audit trail of fired transitions AND the suppression source
-- of truth: AlertEngine treats "most recent alert row for a stalk is type
-- DOWN" as "there is an open incident, don't fire another DOWN yet."

CREATE TABLE alerts (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    stalk_id          UUID NOT NULL REFERENCES stalks(id) ON DELETE CASCADE,
    alert_type        VARCHAR(20) NOT NULL,
    fired_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    delivered_at      TIMESTAMP WITH TIME ZONE NULL,
    downtime_seconds  INTEGER NULL,
    CONSTRAINT chk_alerts_alert_type CHECK (alert_type IN ('DOWN', 'RECOVERY'))
);

-- Primary lookup: "what's the latest alert for this stalk" (suppression + downtime calc).
CREATE INDEX idx_alerts_stalk_id_fired_at ON alerts (stalk_id, fired_at DESC);

COMMENT ON TABLE alerts IS 'Fired DOWN/RECOVERY alert history per stalk; also the suppression source of truth.';
COMMENT ON COLUMN alerts.delivered_at IS 'When the email actually sent. Null if alerts_enabled was false for the recipient at fire time.';
COMMENT ON COLUMN alerts.downtime_seconds IS 'RECOVERY only: seconds between the paired DOWN alert''s fired_at and this row''s fired_at.';

ALTER TABLE users
    ADD COLUMN alert_email VARCHAR(255) NULL,
    ADD COLUMN alerts_enabled BOOLEAN NOT NULL DEFAULT true;

COMMENT ON COLUMN users.alert_email IS 'Optional override recipient for stalk alerts. Falls back to users.email when null.';
COMMENT ON COLUMN users.alerts_enabled IS 'If false, AlertEngine still tracks state transitions but skips sending email.';
