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
 *
 * <p>All read paths take {@code organizationId} for tenant scoping.
 * The associated stalk must belong to that organization or a
 * {@link com.mycelis.shared.exception.TenantAccessException} is thrown.</p>
 */
public interface PulseService {

    /**
     * Records a completed health check result.
     * Not tenant-scoped — pulse ingestion is driven by the internal scheduler,
     * not by user-facing endpoints.
     */
    PulseResponse recordCheckResult(UUID stalkId, int statusCode, long latencyMs,
                                    boolean isSuccess, String errorMessage);

    /**
     * Retrieves recent pulses for a stalk, tenant-scoped.
     */
    List<PulseResponse> getRecentPulses(UUID organizationId, UUID stalkId, int limit);

    /**
     * Returns paginated pulse history, tenant-scoped.
     */
    Page<PulseResponse> getPulseHistory(UUID organizationId, UUID stalkId, Pageable pageable);

    /**
     * Computes uptime percentage over a rolling time window, tenant-scoped.
     */
    double calculateUptimePercentage(UUID organizationId, UUID stalkId, Duration window);

    UptimeResponse getUptimeByWindow(UUID organizationId, UUID stalkId, String window);
}