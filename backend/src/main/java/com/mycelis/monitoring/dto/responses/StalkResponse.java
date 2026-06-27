package com.mycelis.monitoring.dto.responses;

import com.mycelis.monitoring.constant.LatencyState;
import com.mycelis.monitoring.constant.ReliabilityState;
import com.mycelis.monitoring.constant.StalkState;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/**
 * API response representation of a monitored endpoint.
 * Used for POST, GET, and list endpoints to maintain a consistent contract.
 *
 * <p>State is exposed across two orthogonal axes:
 * <ul>
 *   <li>{@link #reliabilityState} — derived from success rate.</li>
 *   <li>{@link #latencyState} — derived from average latency.</li>
 * </ul>
 * The legacy {@link #currentState} field is retained for backward compatibility
 * and will be removed once API consumers migrate to the two-axis fields.</p>
 */
@Data
@Builder
public class StalkResponse {
    /** Unique identifier for the monitoring target */
    private UUID id;

    /** Exact URL being monitored */
    private String url;

    /** User-defined display name for dashboard readability */
    private String nickname;

    /** Frequency of health checks in seconds (default: 60) */
    private Integer growthIntervalSeconds;

    /** Maximum wait time for HTTP response before marking as failed (default: 30) */
    private Integer timeoutSeconds;

    /**
     * Legacy combined state. Retained for backward compatibility.
     *
     * @deprecated use {@link #reliabilityState} and {@link #latencyState}.
     */
    @Deprecated
    private StalkState currentState;

    /** Reliability axis: HEALTHY, DEGRADED, or DORMANT (success rate). */
    private ReliabilityState reliabilityState;

    /** Latency axis: NORMAL or STRESSED (average response time). */
    private LatencyState latencyState;

    /** Health score (0.00–100.00) based on recent check success rate */
    private Double healthIndex;

    /** Average response time over the recent check window (milliseconds) */
    private Long averageLatencyMs;

    /** Count of consecutive failed checks; resets on first success */
    private Integer consecutiveFailures;

    /** Whether automated monitoring is currently enabled */
    private Boolean isActive;

    /** Timestamp when the target was first registered */
    private Instant createdAt;
}