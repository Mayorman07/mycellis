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
                @Index(name = "idx_users_user_id", columnList = "user_id"),
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

    @Column(name = "user_id", nullable = false, unique = true, length = 36)
    private String userId;

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

    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "last_reactivation_email_sent_date")
    private Instant lastReactivationEmailSentDate;

    @Column(name = "last_password_reset_email_sent_at")
    private Instant lastPasswordResetEmailSentAt;

    @Column(name = "password_reset_email_count_today", nullable = false)
    private int passwordResetEmailCountToday;

    @Column(name = "password_reset_email_count_window_start")
    private Instant passwordResetEmailCountWindowStart;


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