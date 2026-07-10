package com.mycelis.membership;

import com.mycelis.membership.entity.Membership;
import com.mycelis.membership.repository.MembershipRepository;
import com.mycelis.organization.entity.Organization;
import com.mycelis.organization.repository.OrganizationRepository;
import com.mycelis.user.entity.User;
import com.mycelis.user.model.request.CreateUserRequest;
import com.mycelis.user.model.response.CreateUserResponse;
import com.mycelis.user.repository.UserRepository;
import com.mycelis.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies UserServiceImpl.createUser creates the user, organization, and
 * membership rows atomically. @Transactional rolls the whole test back
 * afterward — no test fixture data is left behind in the dev DB.
 */
@SpringBootTest
@Transactional
class UserSignupIntegrationTest {

    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private MembershipRepository membershipRepository;

    @Test
    void createUserCreatesUserOrgAndMembershipAtomically() {
        String uniqueEmail = "signup-test-" + UUID.randomUUID() + "@example.com";
        String orgName = "Signup Test Org " + UUID.randomUUID();

        CreateUserRequest request = new CreateUserRequest(
                "Test",
                "User",
                uniqueEmail,
                "SecurePass123!",
                "PREFER_NOT_TO_SAY",
                "+15555550100",
                orgName
        );

        CreateUserResponse response = userService.createUser(request);

        User savedUser = userRepository.findById(response.id()).orElseThrow();
        assertThat(savedUser.getOrganizationId()).isNotNull();

        Organization org = organizationRepository.findById(savedUser.getOrganizationId()).orElseThrow();
        assertThat(org.getName()).isEqualTo(orgName);
        assertThat(org.getOwnerId()).isEqualTo(savedUser.getId());

        Membership membership = membershipRepository.findByUserIdAndIsPrimaryTrue(savedUser.getId())
                .orElseThrow(() -> new AssertionError("No primary membership created for new user"));
        assertThat(membership.getOrganizationId()).isEqualTo(org.getId());
        assertThat(membership.getRole().name()).isEqualTo("OWNER");
        assertThat(membership.isPrimary()).isTrue();
    }
}
