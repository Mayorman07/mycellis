package com.mycelis.monitoring.constant;

/**
 * Reliability axis of a stalk's state. Reflects success rate over the sliding window.
 *
 * <p>This is independent of latency — a stalk can be HEALTHY (reliable) yet STRESSED
 * (slow), or DEGRADED (unreliable) yet have low latency. See {@link LatencyState}.</p>
 *
 * <ul>
 *   <li>{@link #HEALTHY} — success rate at or above {@code healthyThreshold}</li>
 *   <li>{@link #DEGRADED} — success rate below {@code healthyThreshold}, including zero</li>
 *   <li>{@link #DORMANT} — explicitly paused by user; not derived from metrics</li>
 * </ul>
 */
public enum ReliabilityState {
    HEALTHY,
    DEGRADED,
    DORMANT
}