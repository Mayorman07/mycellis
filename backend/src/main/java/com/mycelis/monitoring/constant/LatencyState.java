package com.mycelis.monitoring.constant;

/**
 * Latency axis of a stalk's state. Reflects average response time over the sliding window.
 *
 * <p>This is independent of reliability — see {@link ReliabilityState}.</p>
 *
 * <ul>
 *   <li>{@link #NORMAL} — avg latency below {@code latencyThresholdMs}, or no data</li>
 *   <li>{@link #STRESSED} — avg latency at or above {@code latencyThresholdMs}</li>
 * </ul>
 */
public enum LatencyState {
    NORMAL,
    STRESSED
}