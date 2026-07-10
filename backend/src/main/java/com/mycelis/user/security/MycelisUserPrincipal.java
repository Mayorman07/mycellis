package com.mycelis.user.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Extends Spring's {@link User} to carry the authenticated user's database UUID id,
 * organization memberships, and super-admin flag alongside the standard authentication data.
 *
 * <p>Available in controllers via {@code @AuthenticationPrincipal MycelisUserPrincipal principal}.
 * Use {@link #getId()} for user-scoped operations and {@link #getPrimaryOrganizationId()} for
 * multi-tenant filtering.</p>
 *
 * <p>Membership data is resolved once, eagerly, at login time (see
 * {@code MycelisUserDetailsService}) and cached on this instance — never re-queried per
 * request. A principal is built once per authentication and reused for the session/request,
 * so this is a one-time cost, not a per-call one.</p>
 *
 * <p><b>Naming note:</b> Spring's {@code UserDetails} contract uses "username" as the
 * unique identifier. In Mycelis, that identifier is the user's <b>email</b>. There is
 * no separate username concept. Therefore {@code principal.getUsername()} returns the
 * email address — this is intentional, not a bug. When you see {@code getUsername()}
 * elsewhere in code, mentally read it as "email."</p>
 */
public class MycelisUserPrincipal extends User {

    private final UUID id;
    private final UUID primaryOrganizationId;
    private final List<UUID> organizationIds;
    private final boolean superAdmin;

    public MycelisUserPrincipal(
            UUID id,
            UUID primaryOrganizationId,
            List<UUID> organizationIds,
            boolean superAdmin,
            String email,
            String password,
            boolean enabled,
            boolean accountNonExpired,
            boolean credentialsNonExpired,
            boolean accountNonLocked,
            Collection<? extends GrantedAuthority> authorities) {
        super(email, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.id = id;
        this.primaryOrganizationId = primaryOrganizationId;
        this.organizationIds = organizationIds;
        this.superAdmin = superAdmin;
    }

    public UUID getId() {
        return id;
    }

    /**
     * The organization this user is primarily scoped to for tenant filtering.
     * Every user has exactly one primary membership — no nulls, no exceptions.
     * Super admin belongs to the "Mycellis Inc" system org and gains cross-org
     * access via {@link #isSuperAdmin()}, checked at the service layer, not by
     * nulling this field.
     */
    public UUID getPrimaryOrganizationId() {
        return primaryOrganizationId;
    }

    /** All organizations this user belongs to. Multi-org membership UI/API is a later day. */
    public List<UUID> getOrganizationIds() {
        return organizationIds;
    }

    public boolean isSuperAdmin() {
        return superAdmin;
    }

    /**
     * @deprecated use {@link #getPrimaryOrganizationId()}. Retained so existing
     * call sites (controllers/services reading a single org id) keep working
     * unchanged during the memberships migration. Removed in the V12 cleanup commit.
     */
    @Deprecated
    public UUID getOrganizationId() {
        return getPrimaryOrganizationId();
    }
}
