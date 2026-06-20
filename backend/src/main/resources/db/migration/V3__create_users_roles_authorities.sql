-- =====================================================
-- V3: Users, Roles, Authorities, and join tables
-- =====================================================

-- ----- AUTHORITIES -----
CREATE TABLE authorities (
                             id                  UUID PRIMARY KEY,
                             name                VARCHAR(100) NOT NULL UNIQUE,
                             system_authority    BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_authorities_name ON authorities (name);

-- ----- ROLES -----
CREATE TABLE roles (
                       id              UUID PRIMARY KEY,
                       name            VARCHAR(50) NOT NULL UNIQUE,
                       system_role     BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_roles_name ON roles (name);

-- ----- USERS -----
CREATE TABLE users (
                       id                                  UUID PRIMARY KEY,
                       first_name                          VARCHAR(100) NOT NULL,
                       last_name                           VARCHAR(100) NOT NULL,
                       email                               VARCHAR(255) NOT NULL UNIQUE,
                       user_id                             VARCHAR(36)  NOT NULL UNIQUE,
                       encrypted_password                  VARCHAR(255) NOT NULL,
                       gender                              VARCHAR(20),
                       status                              VARCHAR(20)  NOT NULL,
                       mobile_number                       VARCHAR(20)  NOT NULL,

                       verification_token                  VARCHAR(255),
                       password_reset_token                VARCHAR(255),
                       password_reset_token_expiry_date    TIMESTAMPTZ,
                       last_logged_in                      TIMESTAMPTZ,
                       last_password_reset_date            TIMESTAMPTZ,
                       last_reactivation_email_sent_date   TIMESTAMPTZ,

                       created_at                          TIMESTAMPTZ NOT NULL,
                       updated_at                          TIMESTAMPTZ
);

CREATE INDEX idx_users_email                ON users (email);
CREATE INDEX idx_users_user_id              ON users (user_id);
CREATE INDEX idx_users_status               ON users (status);
CREATE INDEX idx_users_verification_token   ON users (verification_token);
CREATE INDEX idx_users_password_reset_token ON users (password_reset_token);

-- ----- JOIN: users <-> roles -----
CREATE TABLE users_roles (
                             user_id     UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
                             role_id     UUID NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
                             PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_users_roles_role_id ON users_roles (role_id);

-- ----- JOIN: roles <-> authorities -----
CREATE TABLE roles_authorities (
                                   role_id         UUID NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
                                   authority_id    UUID NOT NULL REFERENCES authorities (id) ON DELETE CASCADE,
                                   PRIMARY KEY (role_id, authority_id)
);

CREATE INDEX idx_roles_authorities_authority_id ON roles_authorities (authority_id);