-- V10: Scope stalks to organizations for multi-tenancy.
--
-- Every user belongs to an organization (enforced separately). Therefore, every
-- stalk can and must belong to the creator's organization. Cross-org visibility
-- for admins is handled via role checks in the service layer, NOT by nulling
-- out organization_id.

-- Step 1: add the new column.
ALTER TABLE stalks
    ADD COLUMN organization_id UUID;

-- Step 2: backfill from each stalk's creator's organization.
UPDATE stalks s
SET organization_id = u.organization_id
FROM users u
WHERE s.user_id = u.id;

-- Step 3: safety check before locking the column.
DO $$
    DECLARE
        orphan_count INT;
    BEGIN
        SELECT COUNT(*) INTO orphan_count FROM stalks WHERE organization_id IS NULL;
        IF orphan_count > 0 THEN
            RAISE EXCEPTION 'V10 backfill failed: % stalks have no organization_id', orphan_count;
        END IF;
    END $$;

-- Step 4: lock it in — no more null tenancy anywhere in the system.
ALTER TABLE stalks
    ALTER COLUMN organization_id SET NOT NULL;

-- Step 5: FK for referential integrity.
ALTER TABLE stalks
    ADD CONSTRAINT fk_stalks_organization
        FOREIGN KEY (organization_id) REFERENCES organizations(id);

-- Step 6: rename user_id -> created_by_user_id.
--         It's no longer the tenancy filter, just the audit trail of who created it.
ALTER TABLE stalks
    RENAME COLUMN user_id TO created_by_user_id;

-- Step 7: also lock the User.organization_id column so future signups can't skip org creation.
ALTER TABLE users
    ALTER COLUMN organization_id SET NOT NULL;

ALTER TABLE users
    ADD CONSTRAINT fk_users_organization
        FOREIGN KEY (organization_id) REFERENCES organizations(id);

-- Step 8: indexes for the tenancy filter.
CREATE INDEX idx_stalks_organization_id ON stalks (organization_id);
CREATE INDEX idx_stalks_org_next_check
    ON stalks (organization_id, next_check_at)
    WHERE is_active = true;