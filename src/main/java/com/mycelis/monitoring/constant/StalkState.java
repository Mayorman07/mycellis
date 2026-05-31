package com.mycelis.monitoring.constant;

/**
 * Represents the current health state of a monitored endpoint.
 * Transitions are managed by the Pulse Engine based on sliding-window metrics.
 */
public enum StalkState {
    /** All checks passing within latency thresholds */
    HEALTHY,
    /** Success rate 100% but latency exceeds warning threshold */
    STRESSED,
    /** Success rate between 10% and 89% */
    DEGRADED,
    /** 10 consecutive failures; check frequency automatically reduced */
    DORMANT
}
