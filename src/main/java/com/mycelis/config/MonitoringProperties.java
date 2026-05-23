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
}
