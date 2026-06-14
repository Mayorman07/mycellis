package com.mycelis.user.service;

import com.mycelis.shared.exception.ConflictException;
import com.mycelis.shared.exception.ExpiredTokenException;
import com.mycelis.shared.exception.ResourceNotFoundException;
import com.mycelis.shared.identity.IdGenerator;
import com.mycelis.user.constant.Status;
import com.mycelis.user.entity.Role;
import com.mycelis.user.entity.User;
import com.mycelis.user.model.request.ChangeEmailRequest;
import com.mycelis.user.model.request.ChangePasswordRequest;
import com.mycelis.user.model.request.ForgotPasswordRequest;
import com.mycelis.user.model.request.LoginRequest;
import com.mycelis.user.model.request.ResetPasswordRequest;
import com.mycelis.user.model.response.LoginResponse;
import com.mycelis.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final int PASSWORD_RESET_TOKEN_TTL_HOURS = 1;

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final IdGenerator idGenerator;


    // -------------------- LOGIN --------------------

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        // Spring Security does the heavy lifting: loads user, verifies password,
        // throws BadCredentialsException / DisabledException / LockedException
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        SecurityContextHolder.getContext().setAuthentication(auth);

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        user.setLastLoggedIn(Instant.now());

        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        log.info("Successful login: {}", user.getEmail());
        return new LoginResponse(user.getUserId(), user.getEmail(), roleNames);
    }

    // -------------------- VERIFY EMAIL --------------------

    @Override
    @Transactional
    public void verifyUser(String token) {
        if (token == null || token.isBlank()) {
            throw new ResourceNotFoundException("Verification token", "(empty)");
        }
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Verification token", token));

        user.setStatus(Status.ACTIVE);
        user.setVerificationToken(null);
        log.info("Verified user {}", user.getEmail());
    }

    // -------------------- PASSWORD RESET REQUEST --------------------

    @Override
    @Transactional
    public void requestPasswordReset(ForgotPasswordRequest request) {
        // Always respond identically — never reveal whether email exists
        userRepository.findByEmail(request.email()).ifPresent(user -> {
            String token = idGenerator.newPasswordResetToken();
            user.setPasswordResetToken(token);
            user.setPasswordResetTokenExpiryDate(
                    Instant.now().plus(PASSWORD_RESET_TOKEN_TTL_HOURS, ChronoUnit.HOURS));
            log.info("Generated password reset token for {}", user.getEmail());
            // TODO: enqueue email send (out of scope for tonight)
        });
    }

    // -------------------- PASSWORD RESET PERFORM --------------------

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByPasswordResetToken(request.token())
                .orElseThrow(() -> new ResourceNotFoundException("Reset token", request.token()));

        if (user.getPasswordResetTokenExpiryDate() == null
                || user.getPasswordResetTokenExpiryDate().isBefore(Instant.now())) {
            throw new ExpiredTokenException("Password reset");
        }

        user.setEncryptedPassword(passwordEncoder.encode(request.newPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiryDate(null);
        user.setLastPasswordResetDate(Instant.now());
        log.info("Password reset completed for {}", user.getEmail());
    }

    // -------------------- CHANGE PASSWORD (authenticated) --------------------

    @Override
    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));

        if (!passwordEncoder.matches(request.currentPassword(), user.getEncryptedPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        if (passwordEncoder.matches(request.newPassword(), user.getEncryptedPassword())) {
            throw new ConflictException("New password must differ from current password");
        }

        user.setEncryptedPassword(passwordEncoder.encode(request.newPassword()));
        user.setLastPasswordResetDate(Instant.now());
        log.info("Password changed for {}", user.getEmail());
    }

    // -------------------- CHANGE EMAIL (authenticated) --------------------

    @Override
    @Transactional
    public void changeEmail(String email, ChangeEmailRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));

        if (!passwordEncoder.matches(request.currentPassword(), user.getEncryptedPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }

        if (userRepository.findByEmail(request.newEmail()).isPresent()) {
            throw new ConflictException("That email address is already in use");
        }

        // Defer the actual email swap until new address is verified
        String token = idGenerator.newVerificationToken();
        user.setVerificationToken(token);
        log.info("Email change verification token generated for {}", user.getEmail());
        // TODO: send verification email to new address
    }
}