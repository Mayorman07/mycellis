package com.mycelis.membership;

import com.mycelis.membership.entity.Membership;
import com.mycelis.membership.repository.MembershipRepository;
import com.mycelis.organization.repository.OrganizationRepository;
import com.mycelis.user.entity.User;
import com.mycelis.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the V11 backfill produced a consistent memberships table from
 * whatever user+org data existed before the migration ran.
 *
 * <p>This does not simulate a truly empty/fresh database — no test DB
 * infrastructure (Testcontainers, H2, application-test.properties) existed
 * in this project before this commit, and introducing one was out of scope.
 * Instead, these tests check the backfill's output invariants against the
 * real dev DB's historical data, which V11 has already migrated by the time
 * this test's application context finishes booting (Flyway runs at startup).</p>
 */
@SpringBootTest
class MembershipMigrationIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private MembershipRepository membershipRepository;

    @Test
    void everyUserWithADeprecatedOrgIdHasExactlyOnePrimaryMembership() {
        List<User> usersWithOrg = userRepository.findAll().stream()
                .filter(u -> u.getOrganizationId() != null)
                .toList();

        assertThat(usersWithOrg).isNotEmpty();

        for (User user : usersWithOrg) {
            Membership primary = membershipRepository.findByUserIdAndIsPrimaryTrue(user.getId())
                    .orElseThrow(() -> new AssertionError(
                            "User " + user.getEmail() + " has organizationId but no primary membership"));

            assertThat(primary.getOrganizationId()).isEqualTo(user.getOrganizationId());
        }
    }

    @Test
    void ownerMembershipRoleMatchesDeprecatedOwnerIdColumn() {
        organizationRepository.findAll().forEach(org -> {
            if (org.getOwnerId() == null) {
                return; // orgs created without a backfilled owner (not part of the V11 backfill)
            }

            Membership ownerMembership = membershipRepository
                    .findByUserIdAndOrganizationId(org.getOwnerId(), org.getId())
                    .orElseThrow(() -> new AssertionError(
                            "Org " + org.getSlug() + "'s deprecated owner has no membership row"));

            assertThat(ownerMembership.getRole().name()).isEqualTo("OWNER");
        });
    }
}
