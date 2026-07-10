package com.mycelis.membership.constant;

/**
 * Per-organization role for a membership. Deliberately does NOT include
 * SUPER_ADMIN — that's a user-level system flag ({@code User.isSuperAdmin}),
 * not a per-org role, since a super admin's cross-org access isn't scoped to
 * any single organization.
 */
public enum MembershipRole {
    OWNER,
    ADMIN,
    MEMBER
}
