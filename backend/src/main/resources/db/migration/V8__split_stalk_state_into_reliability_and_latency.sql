-- V8: Split stalks.current_state into two orthogonal axes.
--
-- Rationale: the single-state model loses information when a stalk is both
-- unreliable AND slow. A 4-quadrant model (HEALTHY|DEGRADED × NORMAL|STRESSED)
-- preserves both signals.
--
-- This migration is part of an expand-then-contract refactor:
--   V8 (this) — add new columns, backfill, keep old column.
--   V9 (later) — drop old current_state column after readers are migrated.

-- 1. Add the new columns. Nullable initially so we can backfill safely.
ALTER TABLE stalks
    ADD COLUMN reliability_state VARCHAR(20),
    ADD COLUMN latency_state     VARCHAR(20);

-- 2. Backfill from the existing single-state column.
UPDATE stalks
SET reliability_state = CASE current_state
                            WHEN 'HEALTHY'  THEN 'HEALTHY'
                            WHEN 'STRESSED' THEN 'HEALTHY'
                            WHEN 'DEGRADED' THEN 'DEGRADED'
                            WHEN 'DORMANT'  THEN 'DORMANT'
                            ELSE 'DEGRADED'   -- defensive fallback for any unknown values
    END,
    latency_state = CASE current_state
                        WHEN 'STRESSED' THEN 'STRESSED'
                        ELSE 'NORMAL'
        END;

-- 3. Enforce NOT NULL now that every row has a value.
ALTER TABLE stalks
    ALTER COLUMN reliability_state SET NOT NULL,
    ALTER COLUMN latency_state     SET NOT NULL;

-- 4. Index reliability_state because the scheduler will filter on it
--    (replacement for the existing current_state filter in findDueForCheck).
CREATE INDEX idx_stalks_reliability_state ON stalks (reliability_state);

-- Note: current_state column is intentionally retained for V8.
-- It will be dropped in a future migration (V9) once application code
-- no longer reads or writes it.