-- V14: add normalized_url to stalks, backfilled from existing url values.
--
-- Normalization rules (mirrored exactly in UrlNormalizer.java, the ongoing
-- source of truth for all NEW writes going forward — this migration's SQL
-- is a one-time snapshot, not kept in sync with that class after this file
-- ships):
--   - lowercase scheme + host
--   - drop default ports (:80 for http, :443 for https)
--   - strip path entirely if it's empty or exactly "/"
--   - strip fragment
--   - keep query string and path case as-is
--
-- Same regex approach already validated via a manual dry-run against the
-- real dev DB (Phase 3 PR #1 investigation) before this migration was written.
-- Validated against dev DB post-cleanup (3 rows remaining after duplicate
-- purge, no collisions).
--
-- This migration does NOT add the (organization_id, normalized_url) unique
-- index — that's V15, added later, only after a fresh collision check
-- against this now-backfilled column confirms it's safe.

-- Step 1: add the new column, nullable for now.
ALTER TABLE stalks
    ADD COLUMN normalized_url VARCHAR(2048);

-- Step 2: backfill from each row's existing url.
UPDATE stalks s
SET normalized_url = computed.normalized_url
FROM (
    SELECT
        id,
        lower(scheme) || '://' || lower(host)
            || CASE
                 WHEN port IS NULL THEN ''
                 WHEN lower(scheme) = 'http'  AND port = '80'  THEN ''
                 WHEN lower(scheme) = 'https' AND port = '443' THEN ''
                 ELSE ':' || port
               END
            || CASE
                 WHEN path IS NULL OR path = '' OR path = '/' THEN ''
                 ELSE path
               END
            || CASE
                 WHEN query IS NULL OR query = '' THEN ''
                 ELSE '?' || query
               END
        AS normalized_url
    FROM (
        SELECT
            id,
            (regexp_match(url, '^(https?)://([^/:?#]+)(?::(\d+))?([^?#]*)?(?:\?([^#]*))?(?:#.*)?$'))[1] AS scheme,
            (regexp_match(url, '^(https?)://([^/:?#]+)(?::(\d+))?([^?#]*)?(?:\?([^#]*))?(?:#.*)?$'))[2] AS host,
            (regexp_match(url, '^(https?)://([^/:?#]+)(?::(\d+))?([^?#]*)?(?:\?([^#]*))?(?:#.*)?$'))[3] AS port,
            (regexp_match(url, '^(https?)://([^/:?#]+)(?::(\d+))?([^?#]*)?(?:\?([^#]*))?(?:#.*)?$'))[4] AS path,
            (regexp_match(url, '^(https?)://([^/:?#]+)(?::(\d+))?([^?#]*)?(?:\?([^#]*))?(?:#.*)?$'))[5] AS query
        FROM stalks
    ) parts
) computed
WHERE s.id = computed.id;

-- Step 3: safety check before locking the column — mirrors V10's pattern.
DO $$
    DECLARE
        null_count INT;
    BEGIN
        SELECT COUNT(*) INTO null_count FROM stalks WHERE normalized_url IS NULL;
        IF null_count > 0 THEN
            RAISE EXCEPTION 'V14 backfill failed: % stalks have no normalized_url (malformed/non-http(s) url?)', null_count;
        END IF;
    END $$;

-- Step 4: lock it in.
ALTER TABLE stalks
    ALTER COLUMN normalized_url SET NOT NULL;
