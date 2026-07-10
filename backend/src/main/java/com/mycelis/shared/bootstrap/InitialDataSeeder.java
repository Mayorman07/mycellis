package com.mycelis.shared.bootstrap;

import com.mycelis.membership.constant.MembershipRole;
import com.mycelis.membership.entity.Membership;
import com.mycelis.membership.repository.MembershipRepository;
import com.mycelis.organization.constant.PlanTier;
import com.mycelis.organization.entity.Organization;
import com.mycelis.organization.repository.OrganizationRepository;
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

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
@Profile({"dev", "local"})
@RequiredArgsConstructor
public class InitialDataSeeder {

    private static final String MYCELLIS_INC_SLUG = "mycellis-inc";

    private final AuthorityRepository authorityRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final MembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;

    // Empty-string defaults (rather than none) so a missing property degrades to a
    // clean skip instead of a hard PlaceholderResolutionException at boot — dev
    // machines without seed vars configured must still be able to boot the app.
    @Value("${mycelis.seed.super-admin.email:}")
    private String adminEmail;
    @Value("${mycelis.seed.super-admin.password:}")
    private String adminPassword;
    @Value("${mycelis.seed.super-admin.first-name:}")
    private String adminFirstName;
    @Value("${mycelis.seed.super-admin.last-name:}")
    private String adminLastName;
    @Value("${mycelis.seed.super-admin.mobile:}")
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

        // Legacy authority-granting role, kept in parallel with the new
        // User.isSuperAdmin flag — see seedSuperAdmin() and the User.roles Javadoc.
        Role superAdminRole = upsertRole("SUPER_ADMIN", Set.of(
                userRead, userWrite, userDelete,
                orgRead, orgWrite, orgManage, billingManage, memberInvite, memberRemove,
                monitorRead, monitorWrite, monitorDelete));

        // --- 3. Founder account ---
        seedSuperAdmin(superAdminRole);

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
        Set<Authority> mutableAuthorities = new HashSet<>(authorities);
        return roleRepository.findByName(name)
                .map(existing -> {
                    existing.setAuthorities(mutableAuthorities);
                    return roleRepository.save(existing);
                })
                .orElseGet(() -> {
                    Role r = Role.builder()
                            .name(name)
                            .systemRole(true)
                            .authorities(mutableAuthorities)
                            .build();
                    return roleRepository.save(r);
                });
    }

    private void seedSuperAdmin(Role superAdminRole) {
        String missingProperty = firstMissingSeedProperty();
        if (missingProperty != null) {
            log.warn("Super admin seed skipped: required property '{}' is missing. " +
                    "This is expected on dev machines not configured for seeding.", missingProperty);
            return;
        }

        // Seeder runs on every startup — idempotency check prevents duplicate
        // super-admins across restarts.
        if (userRepository.findByEmail(adminEmail).isPresent()) {
            log.info("Super admin {} already exists, skipping.", adminEmail);
            return;
        }

        Organization mycellisInc = organizationRepository.findBySlug(MYCELLIS_INC_SLUG)
                .orElseGet(() -> {
                    log.info("Creating Mycellis Inc organization for super admin");
                    // ownerId left unset — nullable (V11) specifically so this row
                    // can be saved before the super-admin user exists. Backfilled
                    // below once the user is created; see V11 migration comment.
                    return organizationRepository.save(Organization.builder()
                            .name("Mycellis Inc")
                            .slug(MYCELLIS_INC_SLUG)
                            .planTier(PlanTier.ENTERPRISE)
                            .build());
                });

        User user = User.builder()
                .firstName(adminFirstName)
                .lastName(adminLastName)
                .email(adminEmail)
                .mobileNumber(adminMobile)
                .encryptedPassword(passwordEncoder.encode(adminPassword))
                .status(Status.ACTIVE)
                .roles(new HashSet<>(Set.of(superAdminRole)))
                .organizationId(mycellisInc.getId())   // deprecated compat column
                .isSuperAdmin(true)
                .build();
        User savedUser = userRepository.save(user);

        // Backfill the deprecated owner_id compat column — only relevant on a
        // fresh-DB bootstrap where Mycellis Inc didn't already have an owner.
        if (mycellisInc.getOwnerId() == null) {
            mycellisInc.setOwnerId(savedUser.getId());
            organizationRepository.save(mycellisInc);
        }

        Membership membership = Membership.builder()
                .userId(savedUser.getId())
                .organizationId(mycellisInc.getId())
                .role(MembershipRole.OWNER)
                .isPrimary(true)
                .build();
        membershipRepository.save(membership);

        log.info("Created super admin: email={}, orgId={}", adminEmail, mycellisInc.getId());
    }

    /**
     * Returns the property key of the first missing required seed value, or null
     * if all are present. first-name/last-name aren't checked — application-dev.properties
     * gives them static defaults, so they're always present in dev.
     */
    private String firstMissingSeedProperty() {
        if (adminEmail == null || adminEmail.isBlank()) return "mycelis.seed.super-admin.email";
        if (adminPassword == null || adminPassword.isBlank()) return "mycelis.seed.super-admin.password";
        if (adminMobile == null || adminMobile.isBlank()) return "mycelis.seed.super-admin.mobile";
        return null;
    }
}
