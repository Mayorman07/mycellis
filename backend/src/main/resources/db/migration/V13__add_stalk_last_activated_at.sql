-- V13: Track when a stalk was most recently (re)activated.
--
-- AWAKENING (see ReliabilityState) needs to count pulses "since last
-- activation", not all-time — a stalk resumed from DORMANT should
-- re-enter AWAKENING and accumulate a fresh pulse count, not inherit
-- its pre-pause history. No pause/resume endpoint exists yet (DORMANT
-- is currently unreachable from application code), but this column is
-- the foundation for it: initialized to created_at today, and any
-- future reactivation flow only needs to update this one column.

ALTER TABLE stalks
    ADD COLUMN last_activated_at TIMESTAMPTZ;

UPDATE stalks
SET last_activated_at = created_at
WHERE last_activated_at IS NULL;

ALTER TABLE stalks
    ALTER COLUMN last_activated_at SET NOT NULL,
    ALTER COLUMN last_activated_at SET DEFAULT NOW();
