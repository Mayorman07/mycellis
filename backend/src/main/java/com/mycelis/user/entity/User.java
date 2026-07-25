package com.mycelis.user.entity;

import com.mycelis.user.constant.Status;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.mycelis.user.constant.Gender;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_users_email", columnList = "email"),
                @Index(name = "idx_users_status", columnList = "status"),
                @Index(name = "idx_users_verification_token", columnList = "verification_token"),
                @Index(name = "idx_users_password_reset_token", columnList = "password_reset_token")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, length = 255, unique = true)
    private String email;

    @Column(name = "encrypted_password", nullable = false, length = 255)
    private String encryptedPassword;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Gender gender;

    @Column(name = "last_logged_in")
    private Instant lastLoggedIn;

    @Column(name = "last_password_reset_date")
    private Instant lastPasswordResetDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.NEW;

    @Column(name = "verification_token", length = 255)
    private String verificationToken;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "password_reset_token", length = 255)
    private String passwordResetToken;

    @Column(name = "password_reset_token_expiry_date")
    private Instant passwordResetTokenExpiryDate;

    @Column(name = "mobile_number", nullable = false, length = 20)
    private String mobileNumber;

    /**
     * @deprecated source of truth is now the {@code memberships} table
     * (see {@code com.mycelis.membership.entity.Membership}, primary membership's
     * organizationId). Kept populated in parallel by write paths for backward
     * compatibility. Removed in the V12 migration after memberships is verified
     * in production.
     */
    @Deprecated
    @Column(name = "organization_id")
    private UUID organizationId;

    /**
     * System-level super-admin flag. NOT a membership role — a super admin's
     * cross-org access isn't scoped to any single organization, so it doesn't
     * belong in {@code MembershipRole}. Supersedes the legacy SUPER_ADMIN
     * {@link Role} for authorization checks going forward; the legacy role
     * assignment is still granted in parallel where it already was, since other
     * authority checks may still depend on it.
     */
    @Column(name = "is_super_admin", nullable = false)
    @Builder.Default
    private boolean isSuperAdmin = false;

    @Column(name = "last_reactivation_email_sent_date")
    private Instant lastReactivationEmailSentDate;

    @Column(name = "last_password_reset_email_sent_at")
    private Instant lastPasswordResetEmailSentAt;

    @Column(name = "password_reset_email_count_today", nullable = false)
    private int passwordResetEmailCountToday;

    @Column(name = "password_reset_email_count_window_start")
    private Instant passwordResetEmailCountWindowStart;

    @Column(name = "last_verification_email_sent_at")
    private Instant lastVerificationEmailSentAt;

    @Column(name = "verification_email_count_today", nullable = false)
    private int verificationEmailCountToday;

    @Column(name = "verification_email_count_window_start")
    private Instant verificationEmailCountWindowStart;

    /** Optional override recipient for stalk alerts. Falls back to {@link #email} when null. */
    @Column(name = "alert_email", length = 255)
    private String alertEmail;

    /** If false, AlertEngine still tracks state transitions but skips sending email. */
    @Column(name = "alerts_enabled", nullable = false)
    @Builder.Default
    private boolean alertsEnabled = true;

    /**
     * Grants system-wide authorities (USER_READ, ORG_MANAGE, etc.) via
     * {@link Role#getAuthorities()} — this responsibility is NOT deprecated
     * and this field is NOT going away in V12.
     *
     * <p>What IS superseded: using this set to encode a per-organization role
     * (e.g. a user's "OWNER"/"MEMBER" standing within their org). That's now
     * {@code Membership.role}. SUPER_ADMIN specifically is superseded by
     * {@link #isSuperAdmin} for the same reason — it's cross-org, not
     * org-scoped — though the legacy SUPER_ADMIN role assignment is kept in
     * parallel here too, since other authority checks may still depend on it.</p>
     */
    @ManyToMany(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @JoinTable(
            name = "users_roles",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id")
    )
    @Builder.Default
    @ToString.Exclude
    private Set<Role> roles = new HashSet<>();

    // Helper methods
    public void addRole(Role role) {
        this.roles.add(role);
        role.getUsers().add(this);
    }

    public void removeRole(Role role) {
        this.roles.remove(role);
        role.getUsers().remove(this);
    }

    public boolean hasRole(String roleName) {
        return this.roles.stream()
                .anyMatch(role -> role.getName().equalsIgnoreCase(roleName));
    }
}