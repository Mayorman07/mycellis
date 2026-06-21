package com.mycelis.user.security;

import com.mycelis.user.entity.User;
import com.mycelis.user.constant.Status;
import com.mycelis.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MycelisUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UsernameNotFoundException("No user found with email: " + email));

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
                user.getEmail(),
                user.getEncryptedPassword(),
                enabled,
                true,                  // accountNonExpired — we don't expire accounts
                true,                  // credentialsNonExpired — we don't expire passwords (yet)
                accountNonLocked,
                authorities
        );
    }
}