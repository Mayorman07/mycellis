package com.mycelis.user.service;

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

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String ROLE_OWNER = "OWNER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrganizationService organizationService;
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
        dto.setUserId(idGenerator.newUserId());
        dto.setStatus(Status.NEW);

        User user = userMapper.toEntity(dto);
        user.getRoles().add(ownerRole);

        // Generate verification token
        String verificationToken = idGenerator.newVerificationToken();
        user.setVerificationToken(verificationToken);

        User savedUser = userRepository.save(user);

        Organization org = organizationService.createForOwner(
                request.organizationName(), savedUser.getId());

        savedUser.setOrganizationId(org.getId());

        // Publish event — handled AFTER_COMMIT by UserNotificationListener
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
    public UserProfileResponse updateUser(String userId, UpdateUserRequest request) {
        User user = findByUserIdOrThrow(userId);

        if (request.firstName() != null) user.setFirstName(request.firstName());
        if (request.lastName() != null)  user.setLastName(request.lastName());
        if (request.gender() != null)    user.setGender(userMapper.mapGender(request.gender()));
        if (request.mobileNumber() != null) user.setMobileNumber(request.mobileNumber());

        // dirty-checking flushes on commit; no explicit save needed
        return userMapper.toProfileResponse(user);
    }

    // -------------------- READ --------------------

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse viewProfile(String userId) {
        return userMapper.toProfileResponse(findByUserIdOrThrow(userId));
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
    public void deactivateUser(String userId) {
        User user = findByUserIdOrThrow(userId);
        user.setStatus(Status.DEACTIVATED);
        log.info("Deactivated user {}", userId);
    }

    @Override
    @Transactional
    public void deleteUser(String userId) {
        User user = findByUserIdOrThrow(userId);
        userRepository.delete(user);
        log.info("Deleted user {}", userId);
    }

    @Override
    @Transactional
    public void updateLastLoggedIn(String userId) {
        User user = findByUserIdOrThrow(userId);
        user.setLastLoggedIn(Instant.now());
    }

    // -------------------- HELPERS --------------------

    private User findByUserIdOrThrow(String userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}