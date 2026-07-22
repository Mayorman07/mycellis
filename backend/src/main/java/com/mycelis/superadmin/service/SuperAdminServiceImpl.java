package com.mycelis.superadmin.service;

import com.mycelis.membership.entity.Membership;
import com.mycelis.membership.repository.MembershipRepository;
import com.mycelis.monitoring.dto.responses.StalkResponse;
import com.mycelis.monitoring.entity.Stalk;
import com.mycelis.monitoring.repository.StalkRepository;
import com.mycelis.organization.entity.Organization;
import com.mycelis.organization.repository.OrganizationRepository;
import com.mycelis.shared.exception.ResourceNotFoundException;
import com.mycelis.superadmin.dto.SuperAdminOrgDetail;
import com.mycelis.superadmin.dto.SuperAdminOrgSummary;
import com.mycelis.superadmin.dto.SuperAdminUserSummary;
import com.mycelis.user.entity.Role;
import com.mycelis.user.entity.User;
import com.mycelis.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SuperAdminServiceImpl implements SuperAdminService {

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final MembershipRepository membershipRepository;
    private final StalkRepository stalkRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SuperAdminOrgSummary> getAllOrganizations() {
        return organizationRepository.findAllWithCounts().stream()
                .map(this::toOrgSummary)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SuperAdminUserSummary> getAllUsers() {
        List<User> users = userRepository.findAllWithRoles();

        Map<UUID, Membership> primaryMembershipByUserId = membershipRepository.findAllByIsPrimaryTrue().stream()
                .collect(Collectors.toMap(Membership::getUserId, membership -> membership));

        Set<UUID> orgIds = primaryMembershipByUserId.values().stream()
                .map(Membership::getOrganizationId)
                .collect(Collectors.toSet());
        Map<UUID, Organization> orgsById = organizationRepository.findAllById(orgIds).stream()
                .collect(Collectors.toMap(Organization::getId, organization -> organization));

        return users.stream()
                .map(user -> toUserSummary(user, primaryMembershipByUserId.get(user.getId()), orgsById))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StalkResponse> getOrganizationStalks(UUID organizationId) {
        // organizationId here comes straight from the path variable, not the
        // authenticated principal — correct only because this whole service
        // is reachable exclusively via super-admin-gated endpoints.
        return stalkRepository.findByOrganizationId(organizationId, Pageable.unpaged())
                .getContent()
                .stream()
                .map(this::toStalkResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SuperAdminOrgDetail getOrganizationDetail(UUID organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", organizationId.toString()));

        List<Membership> memberships = membershipRepository.findAllByOrganizationId(organizationId);
        Set<UUID> userIds = memberships.stream().map(Membership::getUserId).collect(Collectors.toSet());
        Map<UUID, User> usersById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        List<SuperAdminOrgDetail.MemberSummary> members = memberships.stream()
                .map(membership -> toMemberSummary(membership, usersById.get(membership.getUserId())))
                .toList();

        long stalkCount = stalkRepository.countByOrganizationId(organizationId);

        return new SuperAdminOrgDetail(
                organization.getId(),
                organization.getName(),
                organization.getSlug(),
                organization.getPlanTier().name(),
                memberships.size(),
                stalkCount,
                organization.getCreatedAt(),
                members
        );
    }

    private SuperAdminUserSummary toUserSummary(User user, Membership primary, Map<UUID, Organization> orgsById) {
        Organization org = primary != null ? orgsById.get(primary.getOrganizationId()) : null;
        Set<String> roleNames = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());

        return new SuperAdminUserSummary(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                org != null ? org.getName() : null,
                roleNames,
                user.getStatus().name(),
                user.getCreatedAt()
        );
    }

    private SuperAdminOrgDetail.MemberSummary toMemberSummary(Membership membership, User user) {
        String name = user != null ? user.getFirstName() + " " + user.getLastName() : "Unknown";
        String email = user != null ? user.getEmail() : "";
        return new SuperAdminOrgDetail.MemberSummary(
                membership.getUserId(),
                name,
                email,
                membership.getRole().name(),
                membership.isPrimary()
        );
    }

    private SuperAdminOrgSummary toOrgSummary(Object[] row) {
        return new SuperAdminOrgSummary(
                toUuid(row[0]),
                (String) row[1],
                (String) row[2],
                (String) row[3],
                ((Number) row[5]).longValue(),
                ((Number) row[6]).longValue(),
                toInstant(row[4])
        );
    }

    // Native query result types for id/timestamp columns can vary by
    // driver/version — handle both plausible shapes rather than assume one
    // (same reasoning as StatusServiceImpl's DATE(...) handling).
    private UUID toUuid(Object value) {
        return switch (value) {
            case UUID uuid -> uuid;
            case String s -> UUID.fromString(s);
            default -> throw new IllegalStateException("Unexpected id value type: " + value.getClass());
        };
    }

    private Instant toInstant(Object value) {
        return switch (value) {
            case Instant instant -> instant;
            case java.sql.Timestamp timestamp -> timestamp.toInstant();
            default -> throw new IllegalStateException("Unexpected timestamp value type: " + value.getClass());
        };
    }

    @SuppressWarnings("deprecation")
    private StalkResponse toStalkResponse(Stalk stalk) {
        return StalkResponse.builder()
                .id(stalk.getId())
                .url(stalk.getUrl())
                .nickname(stalk.getNickname())
                .growthIntervalSeconds(stalk.getGrowthIntervalSeconds())
                .timeoutSeconds(stalk.getTimeoutSeconds())
                .currentState(stalk.getCurrentState())
                .reliabilityState(stalk.getReliabilityState())
                .latencyState(stalk.getLatencyState())
                .healthIndex(stalk.getHealthIndex())
                .averageLatencyMs(stalk.getAverageLatencyMs())
                .consecutiveFailures(stalk.getConsecutiveFailures())
                .isActive(stalk.getIsActive())
                .createdAt(stalk.getCreatedAt())
                .build();
    }
}
