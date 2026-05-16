package com.mycelis.model.dto.responses;

import com.mycelis.constant.StalkState;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/**
 * API response representation of a monitored endpoint.
 * Used for POST, GET, and list endpoints to maintain a consistent contract.
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

    /** Current operational state derived from sliding-window metrics */
    private StalkState currentState;

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