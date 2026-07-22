package com.mycelis.membership.repository;

import com.mycelis.membership.entity.Membership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    Optional<Membership> findByUserIdAndIsPrimaryTrue(UUID userId);

    List<Membership> findAllByUserId(UUID userId);

    Optional<Membership> findByUserIdAndOrganizationId(UUID userId, UUID organizationId);

    boolean existsByUserIdAndOrganizationId(UUID userId, UUID organizationId);

    long countByOrganizationId(UUID organizationId);

    /** Members of one org — super-admin org detail view. */
    List<Membership> findAllByOrganizationId(UUID organizationId);

    /** Every user's primary membership — bulk join source for the super-admin user list. */
    List<Membership> findAllByIsPrimaryTrue();
}
