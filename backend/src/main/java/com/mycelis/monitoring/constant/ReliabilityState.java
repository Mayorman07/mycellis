package com.mycelis.monitoring.constant;

/**
 * Reliability axis of a stalk's state. Reflects success rate over the sliding window.
 *
 * <p>This is independent of latency — a stalk can be HEALTHY (reliable) yet STRESSED
 * (slow), or DEGRADED (unreliable) yet have low latency. See {@link LatencyState}.</p>
 *
 * <ul>
 *   <li>{@link #AWAKENING} — fewer than {@code awakeningPulseThreshold} pulses
 *       recorded since the stalk's last activation; not enough data yet for a
 *       real verdict. See {@code MonitoringProperties.awakeningPulseThreshold}
 *       and {@code Stalk.lastActivatedAt}.</li>
 *   <li>{@link #HEALTHY} — success rate at or above {@code healthyThreshold}</li>
 *   <li>{@link #DEGRADED} — success rate below {@code healthyThreshold}, including zero</li>
 *   <li>{@link #DOWN} — per-pulse only: a hard failure (request failed, or 5xx). The
 *       coarser stalk-level aggregate has never needed this distinction — it only
 *       applies at single-pulse granularity, e.g. {@code PulseMapper}.</li>
 *   <li>{@link #DORMANT} — explicitly paused by user; not derived from metrics</li>
 * </ul>
 */
public enum ReliabilityState {
    AWAKENING,
    HEALTHY,
    DEGRADED,
    DOWN,
    DORMANT
}