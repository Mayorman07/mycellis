package com.mycelis.monitoring.entity;

import com.mycelis.monitoring.constant.LatencyState;
import com.mycelis.monitoring.constant.ReliabilityState;
import com.mycelis.monitoring.constant.StalkState;
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

    /**
     * Tenancy filter. Every stalk belongs to an organization. All read/write
     * access is scoped by this field. See {@code StalkServiceImpl.assertAccess}.
     */
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    /**
     * Audit trail: the user who originally created this stalk. Retained for
     * "created by" display and future "notify creator on incident" features.
     * NOT used for access control — that's {@link #organizationId}.
     */
    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

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

    /**
     * Legacy combined state. Retained for backward compatibility during the
     * two-axis state migration. Will be removed once {@link #reliabilityState}
     * and {@link #latencyState} are the single source of truth.
     *
     * @deprecated use {@link #reliabilityState} and {@link #latencyState}.
     */
    @Deprecated
    @Enumerated(EnumType.STRING)
    @Column(name = "current_state", nullable = false, length = 20)
    @Builder.Default
    private StalkState currentState = StalkState.DORMANT;

    /**
     * Reliability axis: success rate over the sliding window.
     * Independent of latency. See {@link ReliabilityState}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "reliability_state", nullable = false, length = 20)
    @Builder.Default
    private ReliabilityState reliabilityState = ReliabilityState.DORMANT;

    /**
     * Latency axis: avg response time over the sliding window.
     * Independent of reliability. See {@link LatencyState}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "latency_state", nullable = false, length = 20)
    @Builder.Default
    private LatencyState latencyState = LatencyState.NORMAL;

    /** Health score (0.00–100.00) representing recent success rate */
    @Column(name = "health_index", precision = 5)
    @Builder.Default
    private Double healthIndex=0.0;

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