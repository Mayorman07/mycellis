package com.mycelis.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * Records a single failed login attempt for rate-limiting purposes.
 *
 * <p>Stored regardless of whether the email maps to a real user — this is
 * deliberate, to prevent enumeration via the rate-limit logic itself.</p>
 */
@Entity
@Table(name = "login_attempts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginAttempt implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    /** Always stored lowercase. Use the setter to enforce. */
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "failed_at", nullable = false)
    private Instant failedAt;
}