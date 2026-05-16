-- ==========================================
-- PULSE WRITE QUEUE (High-Throughput Buffer)
-- ==========================================
-- This table acts as a staging area for Virtual Threads to dump results
-- A background worker will batch-insert into the main pulses table

CREATE TABLE pulse_write_queue (
                                   id BIGSERIAL PRIMARY KEY,
                                   stalk_id UUID NOT NULL,
                                   status_code INTEGER NOT NULL,
                                   latency_ms BIGINT NOT NULL,
                                   is_success BOOLEAN NOT NULL,
                                   error_message TEXT,
                                   response_size_bytes BIGINT,
                                   queued_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Index for efficient batch processing
CREATE INDEX idx_queue_queued_at ON pulse_write_queue(queued_at ASC);

-- Grant appropriate permissions (adjust for your setup)
-- GRANT SELECT, INSERT ON pulse_write_queue TO mycelis_app;
-- GRANT SELECT, UPDATE, DELETE ON pulse_write_queue TO mycelis_app;

COMMENT ON TABLE pulse_write_queue IS
    'Temporary buffer for high-throughput pulse ingestion. Background worker flushes to pulses table.';