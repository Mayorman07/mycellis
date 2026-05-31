package com.mycelis.monitoring.service;


import com.mycelis.monitoring.dto.responses.PulseResponse;
import com.mycelis.monitoring.dto.responses.UptimeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Contract for Pulse domain operations.
 * Governs diagnostic record ingestion, time-series retrieval,
 * and uptime/latency analytics computation.
 *
 * <p>Implementations must enforce:
 * <ul>
 *   <li>Append-only persistence (no UPDATE/DELETE on committed pulses)</li>
 *   <li>Write-queue buffering for high-throughput virtual thread ingestion</li>
 *   <li>Partition-aware queries to prevent index bloat on historical ranges</li>
 * </ul>
 * </p>
 *
 */
public interface PulseService {

    /**
     * Records a completed health check result.
     * Typically invoked by Virtual Thread callback upon HTTP response resolution.
     *
     * @param stalkId parent monitoring target
     * @param statusCode HTTP response code
     * @param latencyMs round-trip duration in milliseconds
     * @param isSuccess derived success flag (2xx/3xx = true)
     * @param errorMessage nullable diagnostic context on failure
     * @return persisted pulse representation
     */
    PulseResponse recordCheckResult(UUID stalkId, int statusCode, long latencyMs,
                                    boolean isSuccess, String errorMessage);

    /**
     * Retrieves recent pulses for real-time dashboard rendering.
     *
     * @param stalkId parent monitoring target
     * @param limit maximum records to return (capped at 200 for payload safety)
     * @return ordered list of recent pulses
     */
    List<PulseResponse> getRecentPulses(UUID stalkId, int limit);

    /**
     * Returns paginated pulse history for trend analysis and export.
     *
     * @param stalkId parent monitoring target
     * @param pageable pagination & time-range filtering configuration
     * @return page of pulse records
     */
    Page<PulseResponse> getPulseHistory(UUID stalkId, Pageable pageable);

    /**
     * Computes uptime percentage over a rolling time window.
     * Uses partition-pruned aggregation for O(log n) performance.
     *
     * @param stalkId parent monitoring target
     * @param window duration to evaluate (e.g., P7D for 7 days)
     * @return uptime ratio between 0.00 and 100.00
     */
    double calculateUptimePercentage(UUID stalkId, Duration window);

    UptimeResponse getUptimeByWindow(UUID stalkId, String window);

}