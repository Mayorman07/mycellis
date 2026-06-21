package com.mycelis.user.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.UUID;

/**
 * Extends Spring's User to carry the authenticated user's database UUID id
 * alongside the standard authentication data (email, password, authorities).
 *
 * <p>Available in controllers via {@code @AuthenticationPrincipal MycelisUserPrincipal principal}.
 * Eliminates the need to look up the id from the database on every authenticated request.</p>
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