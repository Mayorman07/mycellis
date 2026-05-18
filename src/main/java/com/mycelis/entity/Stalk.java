package com.mycelis.entity;

import com.mycelis.constant.StalkState;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Core domain entity representing a monitored web endpoint.
 * Encapsulates scheduling configuration, real-time health state,
 * and pre-aggregated performance metrics to minimize database load.
 *
 * <p>Mapped to the {@code stalks} table with explicit constraints
 * aligning with Flyway migration V1. Designed for high-concurrency
 * scheduler access and partitioned time-series correlation.</p>
 */
@Entity
@Table(name = "stalks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Stalk {
    /** Globally unique identifier for the monitoring target */
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**Identifier for multi-user isolation*/
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Target URL or API endpoint being monitored */
    @Column(name = "url", length = 2048, nullable = false)
    private String url;

    /** User-defined alias for dashboard readability and reporting */
    @Column(name = "nickname", length = 255)
    private String nickname;

    /** Scheduled polling frequency in seconds (default: 60) */
    @Column(name = "growth_interval_seconds", nullable = false)
    @Builder.Default
    private Integer growthIntervalSeconds = 60;

    /** HTTP request timeout threshold in seconds (default: 30) */
    @Column(name = "timeout_seconds", nullable = false)
    @Builder.Default
    private Integer timeoutSeconds = 30;

    /** Current operational state derived from sliding-window metrics */
    @Enumerated(EnumType.STRING)
    @Column(name = "current_state", nullable = false, length = 20)
    @Builder.Default
    private StalkState currentState = StalkState.DORMANT;

    /** Health score (0.00–100.00) representing recent success rate */
    @Column(name = "health_index", precision = 5, scale = 2)
    @Builder.Default
    private Double healthIndex = 0.00;

    /** Count of successful checks within the rolling evaluation window */
    @Column(name = "last_10_success_count")
    @Builder.Default
    private Integer last10SuccessCount = 0;

    /** Mean round-trip latency (ms) over the recent evaluation window */
    @Column(name = "last_10_avg_latency_ms")
    @Builder.Default
    private Long averageLatencyMs = 0L;

    /** Sequential failure counter; triggers state degradation thresholds */
    @Column(name = "consecutive_failures")
    @Builder.Default
    private Integer consecutiveFailures = 0;

    /** Timestamp scheduling the next health check execution */
    @Column(name = "next_check_at", nullable = false)
    private Instant nextCheckAt;

    /** Timestamp of the most recently completed health check */
    @Column(name = "last_checked_at")
    private Instant lastCheckedAt;

    /** Flag indicating whether automated monitoring is active */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /** Immutable registration timestamp (UTC) */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Audit timestamp tracking the last entity mutation */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}