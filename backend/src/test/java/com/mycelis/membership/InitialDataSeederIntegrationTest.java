package com.mycelis.membership;

import com.mycelis.membership.entity.Membership;
import com.mycelis.membership.repository.MembershipRepository;
import com.mycelis.organization.entity.Organization;
import com.mycelis.organization.repository.OrganizationRepository;
import com.mycelis.shared.bootstrap.InitialDataSeeder;
import com.mycelis.user.entity.User;
import com.mycelis.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Verifies InitialDataSeeder produces a valid super-admin + Mycellis Inc org
 * + OWNER/primary membership, and that re-running it is idempotent.
 *
 * <p>The seeder already ran once as part of this test class's application
 * context startup (its {@code @EventListener(ApplicationReadyEvent.class)}
 * fires during boot like any real run). These tests verify its output
 * invariants hold, then explicitly invoke {@link InitialDataSeeder#seed()}
 * a second time to prove idempotency without needing a second app boot.</p>
 *
 * <p>Skipped (via {@code assumeTrue}) when the seed properties aren't
 * configured in the environment running the tests — same graceful-skip
 * behavior as the seeder itself, since not every environment has the
 * super-admin env vars set.</p>
 */
@SpringBootTest
class InitialDataSeederIntegrationTest {

    private static final String MYCELLIS_INC_SLUG = "mycellis-inc";

    @Value("${mycelis.seed.super-admin.email:}")
    private String adminEmail;

    @Autowired
    private InitialDataSeeder initialDataSeeder;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private MembershipRepository membershipRepository;

    @Test
    void seederProducesValidSuperAdminOrgAndMembership() {
        assumeTrue(adminEmail != null && !adminEmail.isBlank(),
                "Skipping: mycelis.seed.super-admin.email not configured in this environment");

        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new AssertionError("Seeder did not create super admin: " + adminEmail));
        assertThat(admin.isSuperAdmin()).isTrue();

        Organization mycellisInc = organizationRepository.findBySlug(MYCELLIS_INC_SLUG)
                .orElseThrow(() -> new AssertionError("Seeder did not create Mycellis Inc org"));
        assertThat(mycellisInc.getPlanTier().name()).isEqualTo("ENTERPRISE");

        Membership membership = membershipRepository.findByUserIdAndIsPrimaryTrue(admin.getId())
                .orElseThrow(() -> new AssertionError("Super admin has no primary membership"));
        assertThat(membership.getOrganizationId()).isEqualTo(mycellisInc.getId());
        assertThat(membership.getRole().name()).isEqualTo("OWNER");
    }

    @Test
    void secondSeedInvocationIsIdempotent() {
        assumeTrue(adminEmail != null && !adminEmail.isBlank(),
                "Skipping: mycelis.seed.super-admin.email not configured in this environment");

        initialDataSeeder.seed(); // simulate a second application boot

        long matchingUsers = userRepository.findAll().stream()
                .filter(u -> adminEmail.equals(u.getEmail()))
                .count();
        assertThat(matchingUsers).isEqualTo(1);

        User admin = userRepository.findByEmail(adminEmail).orElseThrow();
        long primaryMemberships = membershipRepository.findAllByUserId(admin.getId()).stream()
                .filter(Membership::isPrimary)
                .count();
        assertThat(primaryMemberships).isEqualTo(1);
    }
}
