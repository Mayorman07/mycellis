CREATE TABLE stalk (
                       id UUID PRIMARY KEY,
                       url VARCHAR(1024) NOT NULL,
                       nickname VARCHAR(255),
                       growth_interval_seconds INTEGER DEFAULT 60, -- Polling frequency [cite: 49]
                       is_active BOOLEAN DEFAULT TRUE,
                       created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Index for fast lookup during the Heartbeat cycle [cite: 39]
CREATE INDEX idx_stalk_is_active ON stalk(is_active);