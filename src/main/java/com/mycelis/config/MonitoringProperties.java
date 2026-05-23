package com.mycelis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

/**
 * Centralized configuration for health monitoring thresholds and state transitions.
 * Externalized to allow runtime tuning via Config Server without code changes.
 */
@Data
@ConfigurationProperties(prefix = "mycelis.monitoring")
@Validated
public class MonitoringProperties {

    // =================================================================
    // STATE MACHINE THRESHOLDS
    // =================================================================

    /**
     * Minimum health index percentage to be considered HEALTHY or STRESSED.
     * Range: 0-100
     */
    @Min(0)
    @Max(100)
    private double healthyThreshold = 90.0;

    /**
     * Minimum health index percentage to be considered DEGRADED (vs DORMANT).
     * Range: 0-100
     */
    @Min(0)
    @Max(100)
    private double degradedThreshold = 1.0;

    /**
     * Latency threshold in milliseconds above which a healthy endpoint is marked STRESSED.
     * Default: 2000ms (2 seconds)
     */
    @Min(100)
    private long latencyThresholdMs = 2000L;

    /**
     * Sliding window size for metric calculations (number of recent pulses).
     */
    @Min(1)
    private int slidingWindowSize = 10;

    /**
     * Maximum allowed consecutive failures before forcing DORMANT state.
     */
    @Min(1)
    private int maxConsecutiveFailures = 10;

    // =================================================================
    // API RESPONSE LIMITS (Defense Against Resource Exhaustion)
    // =================================================================

    /**
     * Maximum number of recent pulses to return in a single request.
     * Prevents OOM attacks via unbounded ?limit=100000 queries.
     */
    @Min(1)
    @Max(500)
    private int maxRecentPulses = 200;

    /**
     * Maximum page size for paginated pulse history endpoints.
     */
    @Min(1)
    @Max(200)
    private int maxHistoryPageSize = 100;

    /**
     * Maximum length for stored error messages to prevent database bloat.
     */
    @Min(50)
    @Max(2048)
    private int maxErrorMessageLength = 512;

    // =================================================================
    // PULSE ENGINE CONFIGURATION (Virtual Thread Execution)
    // =================================================================

    /**
     * Maximum duration in seconds for a complete check cycle.
     * If exceeded, the engine forces shutdown to prevent scheduler drift.
     * Range: 10-300 seconds
     */
    @Min(10)
    @Max(300)
    private int maxCycleDurationSeconds = 120;

    /**
     * Maximum response body size in bytes for RestClient requests.
     * Prevents memory exhaustion from malicious or unexpectedly large payloads.
     * Default: 16MB. Range: 1MB-100MB.
     */
    @Min(1_048_576)      // 1MB minimum
    @Max(104_857_600)    // 100MB maximum
    private int maxResponseSizeBytes = 16 * 1024 * 1024;

    /**
     * Default connection timeout in seconds for HTTP requests when not specified per-stalk.
     * Used as fallback if stalk.timeoutSeconds is null or invalid.
     */
    @Min(5)
    @Max(120)
    private int defaultTimeoutSeconds = 30;
}