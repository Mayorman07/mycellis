package com.mycelis.user.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.UUID;

/**
 * Extends Spring's {@link User} to carry the authenticated user's database UUID id
 * alongside the standard authentication data (email, password, authorities).
 *
 * <p>Available in controllers via {@code @AuthenticationPrincipal MycelisUserPrincipal principal}.
 * Use {@link #getId()} for tenant scoping and DB lookups — avoid round-tripping through
 * {@link #getUsername()} which would require another query.</p>
 *
 * <p><b>Naming note:</b> Spring's {@code UserDetails} contract uses "username" as the
 * unique identifier. In Mycelis, that identifier is the user's <b>email</b>. There is
 * no separate username concept. Therefore {@code principal.getUsername()} returns the
 * email address — this is intentional, not a bug. When you see {@code getUsername()}
 * elsewhere in code, mentally read it as "email."</p>
 */
public class MycelisUserPrincipal extends User {

    private final UUID id;

    public MycelisUserPrincipal(
            UUID id,
            String email,
            String password,
            boolean enabled,
            boolean accountNonExpired,
            boolean credentialsNonExpired,
            boolean accountNonLocked,
            Collection<? extends GrantedAuthority> authorities) {
        super(email, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}