package com.mycelis.user.service;

import com.mycelis.membership.repository.MembershipRepository;
import com.mycelis.organization.entity.Organization;
import com.mycelis.organization.repository.OrganizationRepository;
import com.mycelis.shared.exception.ResourceNotFoundException;
import com.mycelis.user.entity.Role;
import com.mycelis.user.entity.User;
import com.mycelis.user.model.response.MeResponse;
import com.mycelis.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Assembles the {@link MeResponse} payload for {@code GET /api/me}.
 * Kept as its own service so the endpoint has one clear responsibility and
 * isn't buried in the fatter {@code UserServiceImpl}.
 */
@Service
@RequiredArgsConstructor
public class MeService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final MembershipRepository membershipRepository;

    @Transactional(readOnly = true)
    public MeResponse getMe(UUID userId) {
        User user = userRepository.findByIdWithRolesAndAuthorities(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));

        Organization org = organizationRepository.findById(user.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Organization", user.getOrganizationId().toString()));

        List<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .toList();

        long memberCount = membershipRepository.countByOrganizationId(org.getId());

        return new MeResponse(
                new MeResponse.UserInfo(
                        user.getId(),
                        user.getEmail(),
                        user.getFirstName(),
                        user.getLastName(),
                        roleNames,
                        user.getCreatedAt()
                ),
                new MeResponse.OrganizationInfo(
                        org.getId(),
                        org.getName(),
                        org.getSlug(),
                        org.getPlanTier().name(),
                        memberCount
                )
        );
    }
}