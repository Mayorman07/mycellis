package com.mycelis.user.service;

import com.mycelis.notification.event.PasswordResetRequestedEvent;
import com.mycelis.notification.event.UserCreatedEvent;
import com.mycelis.shared.exception.*;
import com.mycelis.shared.identity.IdGenerator;
import com.mycelis.shared.util.ClientIpResolver;
import com.mycelis.user.constant.Status;
import com.mycelis.user.entity.User;
import com.mycelis.user.model.request.*;
import com.mycelis.user.model.response.LoginResponse;
import com.mycelis.user.repository.UserRepository;
import com.mycelis.user.security.MycelisUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.server.Cookie;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final int REMEMBER_ME_SECONDS = 30 * 24 * 60 * 60;  // 30 days
    private static final int PASSWORD_RESET_TOKEN_TTL_HOURS = 1;
    private static final int PASSWORD_RESET_COOLDOWN_SECONDS = 60;
    private static final int PASSWORD_RESET_MAX_PER_WINDOW = 5;
    private static final int PASSWORD_RESET_WINDOW_HOURS = 24;
    private static final int VERIFICATION_RESEND_COOLDOWN_SECONDS = 60;
    private static final int VERIFICATION_RESEND_MAX_PER_WINDOW = 5;
    private static final int VERIFICATION_RESEND_WINDOW_HOURS = 24;

    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final PasswordEncoder passwordEncoder;
    private final IdGenerator idGenerator;
    private final ApplicationEventPublisher eventPublisher;

    private final LoginRateLimiterService loginRateLimiter;
    private final ClientIpResolver clientIpResolver;
    private final ServerProperties serverProperties;



    // -------------------- LOGIN --------------------

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String email = request.email();
        String ipAddress = clientIpResolver.resolve(httpRequest);

        // Rate-limit check BEFORE we run bcrypt — saves CPU on attack traffic.
        // We return the same BadCredentialsException as a wrong-password case
        if (loginRateLimiter.isRateLimited(email, ipAddress)) {
            log.warn("Login rate-limited: email={}, ip={}", email, ipAddress);
            throw new BadCredentialsException("Invalid email or password");
        }

        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (DisabledException e) {
            // Don't count disabled-account attempts as brute force — the password
            // might have been correct. Treat it as a UX issue, not a security signal.
            handleDisabledLogin(email);
            throw e; // unreachable
        } catch (BadCredentialsException e) {
            // Record the failure. Rethrow unchanged.
            loginRateLimiter.recordFailure(email, ipAddress);
            throw e;
        }

        // Session fixation defense (CWE-384): invalidate any pre-existing session
        // before we associate it with the authenticated identity.
        HttpSession existingSession = httpRequest.getSession(false);
        if (existingSession != null) {
            existingSession.invalidate();
        }
        HttpSession session = httpRequest.getSession(true);

        // rememberMe only extends the session TTL — everything else about the
        // login (auth, rate limiting, fixation rotation) is unaffected. Absent
        // or false leaves the profile default (24h in prod, servlet default in dev).
        //
        // setMaxInactiveInterval alone only extends how long the SERVER keeps the
        // session alive — it does not touch the browser cookie's Max-Age, which is
        // a servlet-container-wide setting, not per-session. Without also
        // overriding the cookie here, the browser would still discard JSESSIONID
        // on browser close (dev) or after the profile default (24h in prod), long
        // before the server-side session actually expired.
        if (Boolean.TRUE.equals(request.rememberMe())) {
            session.setMaxInactiveInterval(REMEMBER_ME_SECONDS);
            setRememberMeCookie(httpResponse, session.getId());
        }

        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        // Successful login — clear any prior failure trail for this (email, ip).
        loginRateLimiter.clearFailures(email, ipAddress);

        MycelisUserPrincipal principal = (MycelisUserPrincipal) auth.getPrincipal();
        assert principal != null;
        UUID userId = principal.getId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user " + userId + " not found in database — possible race with account deletion"));

        user.setLastLoggedIn(Instant.now());

        Set<String> roleNames = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).filter(Objects::nonNull)
                .filter(name -> name.startsWith("ROLE_"))
                .map(name -> name.substring("ROLE_".length()))
                .collect(Collectors.toSet());

        log.info("Successful login: {}", principal.getUsername());
        return new LoginResponse(userId, principal.getUsername(), roleNames);
    }

    /**
     * Re-issues the JSESSIONID cookie with an explicit Max-Age matching
     * REMEMBER_ME_SECONDS, so the browser actually retains it for 30 days —
     * matching (not replacing) the server-side session TTL set alongside this
     * call. Browsers replace the existing cookie with this one since the name
     * is identical.
     *
     * <p>Secure and SameSite are read from the active profile's session cookie
     * config (ServerProperties) so this cookie carries the same security
     * posture as the container's default one — never hardcoded here. HttpOnly
     * is always true regardless of profile: the session must never be exposed
     * to JS.</p>
     */
    private void setRememberMeCookie(HttpServletResponse response, String sessionId) {
        var cookieConfig = serverProperties.getServlet().getSession().getCookie();
        boolean secure = Boolean.TRUE.equals(cookieConfig.getSecure());
        Cookie.SameSite sameSite = cookieConfig.getSameSite() != null
                ? cookieConfig.getSameSite()
                : Cookie.SameSite.LAX;

        ResponseCookie cookie = ResponseCookie.from("JSESSIONID", sessionId)
                .maxAge(REMEMBER_ME_SECONDS)
                .path("/")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite.attributeValue())
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * Maps a {@code Status} to the precise client-facing exception. Each branch is
     * a different UX path:
     *   NEW         — show "check your inbox" + offer resend.
     *   INACTIVE    — show "account inactive, contact support" (we don't auto-reactivate yet).
     *   DEACTIVATED — show "account closed" (terminal, no remediation path).
     *
     * Always throws — never returns normally.
     */
    private void handleDisabledLogin(String email) {
        Status status = userRepository.findByEmail(email)
                .map(User::getStatus)
                .orElse(null);

        log.info("Login blocked for {} with status={}", email, status);

        if (status == Status.NEW) {
            throw new AccountNotVerifiedException(
                    "Please verify your email to continue.");
        }
        // INACTIVE or DEACTIVATED (or anything unexpected): treat as suspended.
        throw new AccountSuspendedException(
                status == Status.DEACTIVATED
                        ? "This account has been closed."
                        : "This account is inactive. Please contact support.");
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
        // Always respond identically — never reveal whether email exists OR
        // whether the user is rate-limited. Silent throttling.
        userRepository.findByEmail(request.email()).ifPresent(user -> {
            Instant now = Instant.now();

            if (isRateLimited(user, now)) {
                log.warn("Password reset rate-limited for {}", user.getEmail());
                return; // silently drop
            }

            // Update rate limit tracking
            applyRateLimitTracking(user, now);

            // Generate token + publish event as before
            String token = idGenerator.newPasswordResetToken();
            user.setPasswordResetToken(token);
            user.setPasswordResetTokenExpiryDate(
                    now.plus(PASSWORD_RESET_TOKEN_TTL_HOURS, ChronoUnit.HOURS));

            eventPublisher.publishEvent(new PasswordResetRequestedEvent(
                    user.getEmail(),
                    user.getFirstName(),
                    token
            ));

            log.info("Password reset requested for {}", user.getEmail());
        });
    }

// -------------------- RATE LIMITING --------------------

    private boolean isRateLimited(User user, Instant now) {
        // Cooldown check: was last email sent less than COOLDOWN_SECONDS ago?
        Instant lastSent = user.getLastPasswordResetEmailSentAt();
        if (lastSent != null
                && lastSent.plusSeconds(PASSWORD_RESET_COOLDOWN_SECONDS).isAfter(now)) {
            return true;
        }

        // Window cap check: have we sent MAX_PER_WINDOW emails in current window?
        Instant windowStart = user.getPasswordResetEmailCountWindowStart();
        if (windowStart != null
                && windowStart.plus(PASSWORD_RESET_WINDOW_HOURS, ChronoUnit.HOURS).isAfter(now)
                && user.getPasswordResetEmailCountToday() >= PASSWORD_RESET_MAX_PER_WINDOW) {
            return true;
        }

        return false;
    }

    private void applyRateLimitTracking(User user, Instant now) {
        Instant windowStart = user.getPasswordResetEmailCountWindowStart();

        if (windowStart == null
                || windowStart.plus(PASSWORD_RESET_WINDOW_HOURS, ChronoUnit.HOURS).isBefore(now)) {
            // No window yet, or window expired — start a fresh one
            user.setPasswordResetEmailCountWindowStart(now);
            user.setPasswordResetEmailCountToday(1);
        } else {
            // Still inside the current window — increment counter
            user.setPasswordResetEmailCountToday(user.getPasswordResetEmailCountToday() + 1);
        }

        user.setLastPasswordResetEmailSentAt(now);
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

    // -------------------- RESEND VERIFICATION EMAIL --------------------

    @Override
    @Transactional
    public void resendVerification(ResendVerificationRequest request) {
        // Always respond identically — never reveal whether email exists,
        // whether user is already verified, or whether rate limited.
        userRepository.findByEmail(request.email()).ifPresent(user -> {
            // Skip silently if user is already verified
            if (user.getStatus() != Status.NEW) {
                log.info("Verification resend ignored — user {} already verified", user.getEmail());
                return;
            }

            Instant now = Instant.now();

            if (isVerificationResendRateLimited(user, now)) {
                log.warn("Verification resend rate-limited for {}", user.getEmail());
                return;
            }

            applyVerificationResendTracking(user, now);

            // Regenerate the token (invalidates any older verification link)
            String token = idGenerator.newVerificationToken();
            user.setVerificationToken(token);

            eventPublisher.publishEvent(new UserCreatedEvent(
                    user.getEmail(),
                    user.getFirstName(),
                    token
            ));

            log.info("Verification email resent for {}", user.getEmail());
        });
    }

    private boolean isVerificationResendRateLimited(User user, Instant now) {
        Instant lastSent = user.getLastVerificationEmailSentAt();
        if (lastSent != null
                && lastSent.plusSeconds(VERIFICATION_RESEND_COOLDOWN_SECONDS).isAfter(now)) {
            return true;
        }

        Instant windowStart = user.getVerificationEmailCountWindowStart();
        return windowStart != null
                && windowStart.plus(VERIFICATION_RESEND_WINDOW_HOURS, ChronoUnit.HOURS).isAfter(now)
                && user.getVerificationEmailCountToday() >= VERIFICATION_RESEND_MAX_PER_WINDOW;
    }

    private void applyVerificationResendTracking(User user, Instant now) {
        Instant windowStart = user.getVerificationEmailCountWindowStart();

        if (windowStart == null
                || windowStart.plus(VERIFICATION_RESEND_WINDOW_HOURS, ChronoUnit.HOURS).isBefore(now)) {
            user.setVerificationEmailCountWindowStart(now);
            user.setVerificationEmailCountToday(1);
        } else {
            user.setVerificationEmailCountToday(user.getVerificationEmailCountToday() + 1);
        }

        user.setLastVerificationEmailSentAt(now);
    }
}