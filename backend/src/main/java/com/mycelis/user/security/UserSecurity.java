package com.mycelis.user.security;

import com.mycelis.user.entity.User;
import com.mycelis.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component("userSecurity")
@RequiredArgsConstructor
public class UserSecurity {

    private final UserRepository userRepository;

    /**
     * Returns true if the authenticated user is the owner of the profile
     * identified by {@code targetId}. Privilege checks (ADMIN, USER_READ)
     * are handled in @PreAuthorize SpEL, not here.
     */
    @Transactional(readOnly = true)
    public boolean canViewProfile(UUID targetId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserDetails userDetails)) {
            log.warn("Unexpected principal type: {}",
                    principal == null ? "null" : principal.getClass());
            return false;
        }

        String authenticatedEmail = userDetails.getUsername();

        return userRepository.findById(targetId)
                .map(User::getEmail)
                .map(ownerEmail -> ownerEmail.equalsIgnoreCase(authenticatedEmail))
                .orElse(false);
    }
}