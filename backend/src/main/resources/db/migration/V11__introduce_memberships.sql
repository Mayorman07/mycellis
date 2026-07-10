-- V11: Introduce memberships as the source of truth for user<->org relationships.
--
-- users.organization_id and organizations.owner_id are relaxed to nullable here.
-- This is required, not optional: as NOT NULL columns with non-deferrable FKs
-- pointing at each other (organizations.owner_id -> users.id,
-- users.organization_id -> organizations.id), there was no valid insert order
-- for a brand-new org+owner pair — whichever row went first needed the other
-- to already exist. Relaxing both to nullable breaks the cycle: create one row
-- with the cross-reference left null, create the second row referencing the
-- first (which now exists), then backfill the first row's reference.
--
-- Both columns stay in the schema and stay populated in parallel by
-- application code for backward compatibility. They are dropped in V12,
-- once memberships has been verified in production.

ALTER TABLE users ALTER COLUMN organization_id DROP NOT NULL;
ALTER TABLE organizations ALTER COLUMN owner_id DROP NOT NULL;

CREATE TABLE memberships (
    id              UUID PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    role            VARCHAR(32) NOT NULL,
    is_primary      BOOLEAN NOT NULL DEFAULT false,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL,
    UNIQUE (user_id, organization_id),
    CONSTRAINT valid_role CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER'))
);

CREATE INDEX idx_memberships_user_id ON memberships(user_id);
CREATE INDEX idx_memberships_org_id ON memberships(organization_id);

-- Enforces "exactly one primary org per user" without a separate application-level check.
CREATE UNIQUE INDEX idx_memberships_one_primary_per_user
    ON memberships(user_id)
    WHERE is_primary = true;

-- SUPER_ADMIN moves from a membership-adjacent role to a user-level system flag.
-- It is not, and never was, a per-organization role — see MembershipRole.
ALTER TABLE users ADD COLUMN is_super_admin BOOLEAN NOT NULL DEFAULT false;

-- Backfill memberships from each existing user's sole org relationship.
-- gen_random_uuid() here is a one-time historical-backfill convenience;
-- application-created rows get their id from Hibernate as usual, matching
-- every other table in this schema (no DB-level default on memberships.id).
INSERT INTO memberships (id, user_id, organization_id, role, is_primary, created_at, updated_at)
SELECT
    gen_random_uuid(),
    u.id,
    u.organization_id,
    CASE WHEN u.id = o.owner_id THEN 'OWNER' ELSE 'MEMBER' END,
    true,
    u.created_at,
    u.created_at
FROM users u
JOIN organizations o ON o.id = u.organization_id
WHERE u.organization_id IS NOT NULL;

-- Backfill is_super_admin. Roles are NOT a Postgres array in this schema —
-- they're a standard users_roles -> roles many-to-many join table.
UPDATE users
SET is_super_admin = true
WHERE EXISTS (
    SELECT 1
    FROM users_roles ur
    JOIN roles r ON r.id = ur.role_id
    WHERE ur.user_id = users.id AND r.name = 'SUPER_ADMIN'
);
