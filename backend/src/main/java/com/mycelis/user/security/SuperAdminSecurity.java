package com.mycelis.user.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Slf4j
@Component("superAdminSecurity")
public class SuperAdminSecurity {

    /**
     * Returns true only if the authenticated principal is a super admin.
     *
     * <p>Deliberately an explicit Java method rather than a bare SpEL
     * property expression (e.g. {@code principal.superAdmin}) — the bare
     * form was observed throwing during evaluation for non-super-admin
     * principals, which bypassed {@code RestAccessDeniedHandler} entirely
     * and surfaced as a 500 instead of a clean 403. This method is
     * null-safe and type-checked, so it can only ever return true/false.</p>
     */
    public boolean check(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof MycelisUserPrincipal mycelisUserPrincipal)) {
            log.warn("Unexpected principal type for super-admin check: {}",
                    principal == null ? "null" : principal.getClass());
            return false;
        }

        return mycelisUserPrincipal.isSuperAdmin();
    }
}
