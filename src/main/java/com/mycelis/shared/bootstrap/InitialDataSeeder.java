package com.mycelis.shared.bootstrap;

import com.mycelis.user.constant.Status;
import com.mycelis.user.entity.Authority;
import com.mycelis.user.entity.Role;
import com.mycelis.user.entity.User;
import com.mycelis.user.repository.AuthorityRepository;
import com.mycelis.user.repository.RoleRepository;
import com.mycelis.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@Profile({"dev", "local"})
@RequiredArgsConstructor
public class InitialDataSeeder {

    private final AuthorityRepository authorityRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${mycelis.seed.super-admin.email}")
    private String adminEmail;
    @Value("${mycelis.seed.super-admin.password}")
    private String adminPassword;
    @Value("${mycelis.seed.super-admin.first-name}")
    private String adminFirstName;
    @Value("${mycelis.seed.super-admin.last-name}")
    private String adminLastName;
    @Value("${mycelis.seed.super-admin.mobile}")
    private String adminMobile;

    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        log.info("Mycelis startup seed: ensuring authorities, roles, and founder account...");

        // --- 1. Authorities ---
        Authority userRead       = upsertAuthority("USER_READ");
        Authority userWrite      = upsertAuthority("USER_WRITE");
        Authority userDelete     = upsertAuthority("USER_DELETE");

        Authority orgRead        = upsertAuthority("ORG_READ");
        Authority orgWrite       = upsertAuthority("ORG_WRITE");
        Authority orgManage      = upsertAuthority("ORG_MANAGE");
        Authority billingManage  = upsertAuthority("BILLING_MANAGE");
        Authority memberInvite   = upsertAuthority("MEMBER_INVITE");
        Authority memberRemove   = upsertAuthority("MEMBER_REMOVE");

        Authority monitorRead    = upsertAuthority("MONITOR_READ");
        Authority monitorWrite   = upsertAuthority("MONITOR_WRITE");
        Authority monitorDelete  = upsertAuthority("MONITOR_DELETE");

        // --- 2. Roles ---
        upsertRole("MEMBER", Set.of(
                monitorRead, monitorWrite));

        upsertRole("OWNER", Set.of(
                orgManage, billingManage, memberInvite, memberRemove,
                monitorRead, monitorWrite, monitorDelete));

        upsertRole("SUPPORT", Set.of(
                userRead, orgRead, monitorRead));

        upsertRole("ADMIN", Set.of(
                userRead, userWrite, orgRead, orgWrite, monitorRead));

        Role superAdmin = upsertRole("SUPER_ADMIN", Set.of(
                userRead, userWrite, userDelete,
                orgRead, orgWrite, orgManage, billingManage, memberInvite, memberRemove,
                monitorRead, monitorWrite, monitorDelete));

        // --- 3. Founder account ---
        seedSuperAdmin(adminFirstName, adminLastName, adminEmail, adminMobile, adminPassword, superAdmin);

        log.info("Mycellis startup seed complete.");
    }

    private Authority upsertAuthority(String name) {
        return authorityRepository.findByName(name)
                .orElseGet(() -> {
                    Authority a = Authority.builder()
                            .name(name)
                            .systemAuthority(true)
                            .build();
                    return authorityRepository.save(a);
                });
    }

    private Role upsertRole(String name, Set<Authority> authorities) {
        return roleRepository.findByName(name)
                .map(existing -> {
                    existing.setAuthorities(authorities);
                    return roleRepository.save(existing);
                })
                .orElseGet(() -> {
                    Role r = Role.builder()
                            .name(name)
                            .systemRole(true)
                            .authorities(authorities)
                            .build();
                    return roleRepository.save(r);
                });
    }

    private void seedSuperAdmin(String firstName, String lastName, String email,
                                String mobile, String password, Role superAdminRole) {
        if (userRepository.findByEmail(email).isPresent()) {
            log.info("Super admin {} already exists, skipping.", email);
            return;
        }

        User user = User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .userId(UUID.randomUUID().toString())
                .mobileNumber(mobile)
                .encryptedPassword(passwordEncoder.encode(password))
                .status(Status.ACTIVE)
                .roles(Set.of(superAdminRole))
                .build();

        userRepository.save(user);
        log.info("Super admin created: {}", email);
    }
}