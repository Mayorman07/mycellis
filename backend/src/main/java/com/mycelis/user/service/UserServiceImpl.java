package com.mycelis.user.service;

import com.mycelis.membership.constant.MembershipRole;
import com.mycelis.membership.entity.Membership;
import com.mycelis.membership.repository.MembershipRepository;
import com.mycelis.organization.entity.Organization;
import com.mycelis.organization.service.OrganizationService;
import com.mycelis.shared.exception.ConflictException;
import com.mycelis.shared.exception.ResourceNotFoundException;
import com.mycelis.shared.identity.IdGenerator;
import com.mycelis.user.constant.Status;
import com.mycelis.user.entity.Role;
import com.mycelis.user.entity.User;
import com.mycelis.user.mapper.UserMapper;
import com.mycelis.user.model.dto.UserDto;
import com.mycelis.user.model.request.CreateUserRequest;
import com.mycelis.user.model.request.UpdateUserRequest;
import com.mycelis.user.model.response.CreateUserResponse;
import com.mycelis.user.model.response.UserProfileResponse;
import com.mycelis.user.repository.RoleRepository;
import com.mycelis.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.mycelis.notification.event.UserCreatedEvent;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String ROLE_OWNER = "OWNER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrganizationService organizationService;
    private final MembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final IdGenerator idGenerator;

    // -------------------- CREATE --------------------

    @Override
    @Transactional
    public CreateUserResponse createUser(CreateUserRequest request) {
        log.info("Creating user with email {}", request.email());

        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new ConflictException("An account with this email already exists");
        }

        Role ownerRole = roleRepository.findByName(ROLE_OWNER)
                .orElseThrow(() -> new IllegalStateException(
                        "OWNER role missing — seed did not run"));

        UserDto dto = userMapper.toDto(request);
        dto.setEncryptedPassword(passwordEncoder.encode(request.password()));
        dto.setPassword(null);
        dto.setStatus(Status.NEW);
        dto.setCreatedAt(Instant.now());

        User user = userMapper.toEntity(dto);
        user.getRoles().add(ownerRole);

        String verificationToken = idGenerator.newVerificationToken();
        user.setVerificationToken(verificationToken);

        // organization_id is left unset here — it's nullable (V11) specifically so
        // this row can be saved before its organization exists. Backfilled below
        // once the org is created, mirroring the same two-phase pattern the seeder
        // uses for the same reason: organizations.owner_id -> users.id and
        // users.organization_id -> organizations.id form a circular non-deferrable
        // FK pair, so neither row can reference the other until it already exists.
        User savedUser = userRepository.save(user);

        Organization org = organizationService.createForOwner(
                request.organizationName(), savedUser.getId());

        // Deprecated compat column — kept in sync in parallel with the membership
        // row below until V12 drops it. Explicit save rather than relying on
        // dirty-checking, so write is unambiguous.
        savedUser.setOrganizationId(org.getId());
        userRepository.save(savedUser);

        // Source of truth going forward.
        Membership membership = Membership.builder()
                .userId(savedUser.getId())
                .organizationId(org.getId())
                .role(MembershipRole.OWNER)
                .isPrimary(true)
                .build();
        membershipRepository.save(membership);

        eventPublisher.publishEvent(new UserCreatedEvent(
                savedUser.getEmail(),
                savedUser.getFirstName(),
                verificationToken
        ));

        return userMapper.toCreateResponse(savedUser);
    }

    // -------------------- UPDATE --------------------

    @Override
    @Transactional
    public UserProfileResponse updateUser(UUID id, UpdateUserRequest request) {
        User user = findByIdOrThrow(id);

        if (request.firstName() != null) user.setFirstName(request.firstName());
        if (request.lastName() != null)  user.setLastName(request.lastName());
        if (request.gender() != null)    user.setGender(userMapper.mapGender(request.gender()));
        if (request.mobileNumber() != null) user.setMobileNumber(request.mobileNumber());

        return userMapper.toProfileResponse(user);
    }

    // -------------------- READ --------------------

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse viewProfile(UUID id) {
        return userMapper.toProfileResponse(findByIdOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserProfileResponse> findAllUsers(Pageable pageable, String keyword) {
        Page<User> page = (keyword == null || keyword.isBlank())
                ? userRepository.findAll(pageable)
                : userRepository.findAllByKeyword(keyword, pageable);
        return page.map(userMapper::toProfileResponse);
    }

    // -------------------- LIFECYCLE --------------------

    @Override
    @Transactional
    public void deactivateUser(UUID id) {
        User user = findByIdOrThrow(id);
        user.setStatus(Status.DEACTIVATED);
        log.info("Deactivated user {}", id);
    }

    @Override
    @Transactional
    public void deleteUser(UUID id) {
        User user = findByIdOrThrow(id);
        userRepository.delete(user);
        log.info("Deleted user {}", id);
    }

    @Override
    @Transactional
    public void updateLastLoggedIn(UUID id) {
        User user = findByIdOrThrow(id);
        user.setLastLoggedIn(Instant.now());
    }

    // -------------------- HELPERS --------------------

    private User findByIdOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id.toString()));
    }
}