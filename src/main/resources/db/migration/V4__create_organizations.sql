-- =====================================================
-- V4: Organizations + link User to Organization
-- =====================================================

CREATE TABLE organizations (
                               id          UUID PRIMARY KEY,
                               name        VARCHAR(100) NOT NULL,
                               slug        VARCHAR(60)  NOT NULL UNIQUE,
                               plan_tier   VARCHAR(20)  NOT NULL,
                               owner_id    UUID         NOT NULL,
                               created_at  TIMESTAMPTZ  NOT NULL,
                               updated_at  TIMESTAMPTZ
);

CREATE INDEX idx_organizations_slug      ON organizations (slug);
CREATE INDEX idx_organizations_owner_id  ON organizations (owner_id);

-- Foreign key: organizations.owner_id -> users.id
ALTER TABLE organizations
    ADD CONSTRAINT fk_organizations_owner
        FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE RESTRICT;

-- Add organization_id to users (nullable — super admin has no org)
ALTER TABLE users
    ADD COLUMN organization_id UUID;

ALTER TABLE users
    ADD CONSTRAINT fk_users_organization
        FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE SET NULL;

CREATE INDEX idx_users_organization_id ON users (organization_id);