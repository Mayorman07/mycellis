-- V15: unique index on (organization_id, normalized_url).
--
-- Depends on V14, which added and backfilled the normalized_url column —
-- this migration only adds the index, no column changes.
--
-- A fresh collision dry-run against prod's now-backfilled normalized_url
-- column returned zero rows before this migration was written (Phase 3
-- V15 investigation) — safe to enforce uniqueness.
--
-- The service-layer existsByOrganizationIdAndNormalizedUrl(...) check in
-- StalkServiceImpl stays as the UX layer (returns a 409 with an RFC 7807
-- problem detail before the DB is ever touched). This index is the atomic
-- enforcement layer underneath it — closes the race the service-layer check
-- alone can't (two concurrent creates both passing the existsBy... check
-- before either commits).

CREATE UNIQUE INDEX idx_stalks_org_normalized_url ON stalks (organization_id, normalized_url);
