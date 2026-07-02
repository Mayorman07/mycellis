package com.mycelis.user.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.UUID;

/**
 * Extends Spring's {@link User} to carry the authenticated user's database UUID id
 * AND their organization id alongside the standard authentication data.
 *
 * <p>Available in controllers via {@code @AuthenticationPrincipal MycelisUserPrincipal principal}.
 * Use {@link #getId()} for user-scoped operations and {@link #getOrganizationId()} for
 * multi-tenant filtering.</p>
 *
 * <p><b>Naming note:</b> Spring's {@code UserDetails} contract uses "username" as the
 * unique identifier. In Mycelis, that identifier is the user's <b>email</b>. There is
 * no separate username concept. Therefore {@code principal.getUsername()} returns the
 * email address — this is intentional, not a bug. When you see {@code getUsername()}
 * elsewhere in code, mentally read it as "email."</p>
 */
public class MycelisUserPrincipal extends User {

    private final UUID id;
    private final UUID organizationId;

    public MycelisUserPrincipal(
            UUID id,
            UUID organizationId,
            String email,
            String password,
            boolean enabled,
            boolean accountNonExpired,
            boolean credentialsNonExpired,
            boolean accountNonLocked,
            Collection<? extends GrantedAuthority> authorities) {
        super(email, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.id = id;
        this.organizationId = organizationId;
    }

    public UUID getId() {
        return id;
    }

    /**
     * The organization this user belongs to. Every user has an org — no nulls,
     * no exceptions. Super admin belongs to the "Mycellis Inc" system org and
     * gains cross-org access via the {@code SUPER_ADMIN} role, checked at the
     * service layer, not by nulling this field.
     */
    public UUID getOrganizationId() {
        return organizationId;
    }
}