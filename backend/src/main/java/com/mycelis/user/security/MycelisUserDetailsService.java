package com.mycelis.user.security;

import com.mycelis.user.entity.User;
import com.mycelis.user.constant.Status;
import com.mycelis.user.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MycelisUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Cached dummy hash used to equalize response time when the lookup misses.
     * Computed once at startup so the bcrypt cost matches real password hashes.
     * See {@link #loadUserByUsername(String)} for the timing-defense rationale.
     */
    private volatile String dummyHash;

    @PostConstruct
    void initDummyHash() {
        // Compute once at startup; bcrypt at cost 12 takes ~500ms so we don't
        // want this on the request path.
        this.dummyHash = passwordEncoder.encode("not-a-real-password");
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailWithRolesAndAuthorities(email).orElse(null);

        if (user == null) {
            // Timing-attack defense (CWE-208): when the user is not found, burn
            // equivalent CPU time hashing a dummy password so the response latency
            // is indistinguishable from a "user found, wrong password" case.
            // Prevents username enumeration via response-time analysis.
            passwordEncoder.matches("not-a-real-password", dummyHash);
            throw new UsernameNotFoundException("No user found with email: " + email);
        }

        Set<GrantedAuthority> authorities = new HashSet<>();
        user.getRoles().forEach(role -> {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));
            role.getAuthorities().forEach(authority ->
                    authorities.add(new SimpleGrantedAuthority(authority.getName())));
        });

        boolean enabled = !(user.getStatus() == Status.NEW
                || user.getStatus() == Status.INACTIVE
                || user.getStatus() == Status.DEACTIVATED);
        boolean accountNonLocked = user.getStatus() != Status.BLOCKED;

        return new MycelisUserPrincipal(
                user.getId(),
                user.getOrganizationId(),
                user.getEmail(),
                user.getEncryptedPassword(),
                enabled,
                true,                  // accountNonExpired
                true,                  // credentialsNonExpired
                accountNonLocked,
                authorities
        );
    }
}