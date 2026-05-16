-- ==========================================
-- STALKS TABLE (The Targets to Monitor)
-- ==========================================
CREATE TABLE stalks (
                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        user_id UUID NOT NULL,  -- For multi-tenancy (even if single-user now, future-proof)
                        url VARCHAR(2048) NOT NULL,
                        nickname VARCHAR(255),  -- User-friendly name for the endpoint

    -- Monitoring Configuration
                        growth_interval_seconds INTEGER NOT NULL DEFAULT 60,  -- Check frequency
                        timeout_seconds INTEGER NOT NULL DEFAULT 30,

    -- State Machine (Biological States)
                        current_state VARCHAR(20) NOT NULL DEFAULT 'DORMANT'
                            CHECK (current_state IN ('HEALTHY', 'STRESSED', 'DEGRADED', 'DORMANT')),

    -- Cached Health Metrics (avoids expensive real-time calculations)
                        health_index DECIMAL(5,2) DEFAULT 0.00,  -- 0-100 sliding window score
                        last_10_success_count INTEGER DEFAULT 0,
                        last_10_avg_latency_ms BIGINT DEFAULT 0,
                        consecutive_failures INTEGER DEFAULT 0,

    -- Scheduler Control
                        next_check_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                        last_checked_at TIMESTAMPTZ,

    -- Metadata
                        is_active BOOLEAN NOT NULL DEFAULT TRUE,
                        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                        updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes for Stalks
CREATE INDEX idx_stalks_next_check ON stalks(next_check_at)
    WHERE is_active = TRUE AND current_state != 'DORMANT';
CREATE INDEX idx_stalks_user_id ON stalks(user_id);
CREATE INDEX idx_stalks_state ON stalks(current_state);
CREATE INDEX idx_stalks_created ON stalks(created_at DESC);

-- Trigger to auto-update updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_stalks_updated_at
    BEFORE UPDATE ON stalks
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();


-- ==========================================
-- PULSES TABLE (Immutable Health Records)
-- ==========================================
CREATE TABLE pulses (
                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                        stalk_id UUID NOT NULL REFERENCES stalks(id) ON DELETE CASCADE,

    -- HTTP Response Data
                        status_code INTEGER NOT NULL,
                        latency_ms BIGINT NOT NULL,  -- Round-trip time in milliseconds
                        is_success BOOLEAN NOT NULL,
                        error_message TEXT,
                        response_size_bytes BIGINT,

    -- Timestamp
                        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
) PARTITION BY RANGE (created_at);

-- Create initial partition (Q2 2026 - adjust based on your launch date)
CREATE TABLE pulses_2026_q2 PARTITION OF pulses
    FOR VALUES FROM ('2026-04-01') TO ('2026-07-01');

-- Indexes for Pulses
CREATE INDEX idx_pulses_stalk_created ON pulses(stalk_id, created_at DESC);
CREATE INDEX idx_pulses_created ON pulses(created_at DESC);
CREATE INDEX idx_pulses_success ON pulses(is_success) WHERE is_success = FALSE;

-- ==========================================
-- ADDITIONAL FUTURE PARTITIONS (Optional)
-- Uncomment as needed
-- ==========================================
-- CREATE TABLE pulses_2026_q3 PARTITION OF pulses
--     FOR VALUES FROM ('2026-07-01') TO ('2026-10-01');