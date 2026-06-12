package com.mycelis.shared.bootstrap;

import com.mycelis.user.entity.Authority;
import com.mycelis.user.entity.Role;
import com.mycelis.user.repository.AuthorityRepository;
import com.mycelis.user.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class ProdAuthoritySeeder {

    private final AuthorityRepository authorityRepository;
    private final RoleRepository roleRepository;

    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        log.info("Mycelis [prod] seed: ensuring authorities and roles only...");

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

        upsertRole("MEMBER", Set.of(monitorRead, monitorWrite));

        upsertRole("OWNER", Set.of(
                orgManage, billingManage, memberInvite, memberRemove,
                monitorRead, monitorWrite, monitorDelete));

        upsertRole("SUPPORT", Set.of(userRead, orgRead, monitorRead));

        upsertRole("ADMIN", Set.of(
                userRead, userWrite, orgRead, orgWrite, monitorRead));

        upsertRole("SUPER_ADMIN", Set.of(
                userRead, userWrite, userDelete,
                orgRead, orgWrite, orgManage, billingManage, memberInvite, memberRemove,
                monitorRead, monitorWrite, monitorDelete));

        log.info("Mycelis [prod] seed complete (no user accounts created).");
    }

    private Authority upsertAuthority(String name) {
        return authorityRepository.findByName(name)
                .orElseGet(() -> authorityRepository.save(
                        Authority.builder().name(name).systemAuthority(true).build()));
    }

    private void upsertRole(String name, Set<Authority> authorities) {
        roleRepository.findByName(name)
                .map(existing -> {
                    existing.setAuthorities(authorities);
                    return roleRepository.save(existing);
                })
                .orElseGet(() -> roleRepository.save(
                        Role.builder()
                                .name(name)
                                .systemRole(true)
                                .authorities(authorities)
                                .build()));
    }
}