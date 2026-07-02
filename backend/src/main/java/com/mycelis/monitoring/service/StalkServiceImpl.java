package com.mycelis.monitoring.service;

import com.mycelis.shared.config.MonitoringProperties;
import com.mycelis.monitoring.constant.LatencyState;
import com.mycelis.monitoring.constant.ReliabilityState;
import com.mycelis.monitoring.constant.StalkState;
import com.mycelis.monitoring.entity.Stalk;
import com.mycelis.shared.exception.TenantAccessException;
import com.mycelis.monitoring.dto.requests.CreateStalkRequest;
import com.mycelis.monitoring.dto.responses.StalkResponse;
import com.mycelis.monitoring.repository.PulseRepository;
import com.mycelis.monitoring.repository.StalkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Production implementation of Stalk lifecycle & state management.
 * Enforces tenant isolation, atomic metric updates, and strict state machine transitions.
 *
 * <p>State is modeled across two orthogonal axes:
 * <ul>
 *   <li>{@link ReliabilityState} — derived from success rate.</li>
 *   <li>{@link LatencyState} — derived from average latency.</li>
 * </ul>
 * The legacy {@code currentState} field is kept in sync via {@link #deriveLegacyState}
 * until all readers migrate to the two-axis model.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StalkServiceImpl implements StalkService {

    private final StalkRepository stalkRepository;
    private final PulseRepository pulseRepository;
    private final MonitoringProperties monitoringProperties;

    @Override
    @Transactional
    @SuppressWarnings("deprecation")
    public StalkResponse createStalk(UUID organizationId, UUID createdByUserId, CreateStalkRequest request) {
        validateTimeoutAgainstCycle(request.getTimeoutSeconds());
        Stalk stalk = Stalk.builder()
                .organizationId(organizationId)
                .createdByUserId(createdByUserId)
                .url(request.getUrl())
                .nickname(request.getNickname())
                .growthIntervalSeconds(request.getGrowthIntervalSeconds())
                .timeoutSeconds(request.getTimeoutSeconds())
                .currentState(StalkState.HEALTHY)
                .reliabilityState(ReliabilityState.HEALTHY)
                .latencyState(LatencyState.NORMAL)
                .healthIndex(0.0)
                .consecutiveFailures(0)
                .isActive(true)
                .nextCheckAt(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Stalk saved = stalkRepository.save(stalk);
        log.info("Stalk created: id={}, orgId={}, createdBy={}, url={}",
                saved.getId(), organizationId, createdByUserId, saved.getUrl());
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public StalkResponse getStalkById(UUID organizationId, UUID id) {
        Stalk stalk = stalkRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Stalk not found: " + id));

        if (!stalk.getOrganizationId().equals(organizationId)) {
            throw new TenantAccessException(
                    "Access denied: stalk " + id + " does not belong to organization " + organizationId);
        }
        return mapToResponse(stalk);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StalkResponse> getAllStalks(UUID organizationId, Pageable pageable) {
        return stalkRepository.findByOrganizationId(organizationId, pageable).map(this::mapToResponse);
    }
    @Override
    @Transactional
    public StalkResponse updateConfiguration(UUID organizationId, UUID id, CreateStalkRequest request) {
        validateTimeoutAgainstCycle(request.getTimeoutSeconds());
        Stalk stalk = stalkRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Stalk not found: " + id));

        if (!stalk.getOrganizationId().equals(organizationId)) {
            throw new TenantAccessException(
                    "Access denied: stalk " + id + " does not belong to organization " + organizationId);
        }

        stalk.setUrl(request.getUrl());
        stalk.setNickname(request.getNickname());
        stalk.setGrowthIntervalSeconds(request.getGrowthIntervalSeconds());
        stalk.setTimeoutSeconds(request.getTimeoutSeconds());
        stalk.setNextCheckAt(Instant.now().plusSeconds(request.getGrowthIntervalSeconds()));

        Stalk updated = stalkRepository.save(stalk);
        log.info("Stalk configuration updated: id={}", id);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteStalk(UUID organizationId, UUID id) {
        Stalk stalk = stalkRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Stalk not found: " + id));

        if (!stalk.getOrganizationId().equals(organizationId)) {
            throw new TenantAccessException(
                    "Access denied: stalk " + id + " does not belong to organization " + organizationId);
        }

        stalkRepository.delete(stalk);
        log.info("Stalk deleted: id={}, orgId={}", id, organizationId);
    }

    @Override
    @Transactional
    @SuppressWarnings("deprecation")
    public void updateMetricsAndTransitionState(UUID stalkId, Instant checkCompletedAt) {
        Instant windowStart = checkCompletedAt.minus(
                Duration.ofMinutes(monitoringProperties.getSlidingWindowMinutes())
        );

        long totalCount = pulseRepository.countTotalInWindow(stalkId, windowStart);

        // No data in window — don't transition state. Previous state stays as-is.
        // DORMANT is reserved for explicit user-paused stalks; we never enter it from a metrics calculation.
        if (totalCount == 0) {
            log.debug("Skipping state update for stalkId={}: no pulses in window", stalkId);
            return;
        }

        long successCount = pulseRepository.countSuccessesInWindow(stalkId, windowStart);
        Double avgLatency = pulseRepository.calculateAvgLatencyInWindow(stalkId, windowStart);

        double healthIndex = calculateHealthIndex(successCount, totalCount);
        ReliabilityState reliability = evaluateReliability(healthIndex, totalCount);
        LatencyState latency = evaluateLatency(avgLatency);

        Stalk stalk = stalkRepository.findById(stalkId)
                .orElseThrow(() -> new IllegalArgumentException("Stalk not found: " + stalkId));

        stalk.setHealthIndex(roundToTwoDecimals(healthIndex));
        stalk.setAverageLatencyMs(avgLatency != null ? Math.round(avgLatency) : 0L);
        stalk.setLast10SuccessCount((int) successCount);

        // Write new two-axis state
        stalk.setReliabilityState(reliability);
        stalk.setLatencyState(latency);

        // Backward-compatibility shim: keep legacy currentState in sync until readers migrate
        stalk.setCurrentState(deriveLegacyState(reliability, latency));

        stalk.setUpdatedAt(Instant.now());

        stalkRepository.save(stalk);

        log.info("State updated: stalkId={}, health={}%, successes={}/{} → reliability={}, latency={}",
                stalkId, healthIndex, successCount, totalCount, reliability, latency);
    }

    /**
     * Determines reliability state from success rate alone.
     * Independent of latency.
     *
     * <p>DORMANT is never set by metrics — it's only set by explicit user action.
     * No-data case is guarded upstream; this method always returns HEALTHY or DEGRADED.</p>
     */
    private ReliabilityState evaluateReliability(double healthIndex, long totalCount) {
        validateHealthIndex(healthIndex);

        if (totalCount == 0) {
            return ReliabilityState.DEGRADED;  // defensive; caller already guards
        }

        if (healthIndex >= monitoringProperties.getHealthyThreshold()) {
            return ReliabilityState.HEALTHY;
        }
        return ReliabilityState.DEGRADED;
    }

    /**
     * Determines latency state from average latency alone.
     * Independent of reliability.
     *
     * <p>If avgLatency is null (no successful requests in window), defaults to NORMAL —
     * we have no evidence of slowness, and reliability tells the failure story alone.</p>
     */
    private LatencyState evaluateLatency(Double avgLatency) {
        if (avgLatency == null) {
            return LatencyState.NORMAL;
        }
        if (avgLatency >= monitoringProperties.getLatencyThresholdMs()) {
            log.debug("Latency threshold exceeded: {}ms >= {}ms → STRESSED",
                    avgLatency, monitoringProperties.getLatencyThresholdMs());
            return LatencyState.STRESSED;
        }
        return LatencyState.NORMAL;
    }

    /**
     * Derives the legacy single-state enum from the two new axes.
     *
     * <p>This shim exists so deprecated readers of {@code currentState}
     * (controllers, listeners, repositories) continue to work during the migration.
     * It will be removed when {@code currentState} is dropped.</p>
     *
     * @deprecated only for backward compatibility during the two-axis migration.
     */
    @Deprecated
    private StalkState deriveLegacyState(ReliabilityState reliability, LatencyState latency) {
        return switch (reliability) {
            case DORMANT -> StalkState.DORMANT;
            case DEGRADED -> StalkState.DEGRADED;
            case HEALTHY -> (latency == LatencyState.STRESSED)
                    ? StalkState.STRESSED
                    : StalkState.HEALTHY;
        };
    }

    private void validateHealthIndex(double healthIndex) {
        if (healthIndex < 0.0 || healthIndex > 100.0) {
            log.error("Corrupted healthIndex detected: {}. Forcing DORMANT state.", healthIndex);
            throw new IllegalStateException("Invalid health index: " + healthIndex);
        }
    }

    /**
     * Calculates health index as success rate percentage.
     * @param successCount Number of successful checks in window
     * @param totalCount Total number of checks in window
     * @return Health index 0.0 - 100.0
     */
    private double calculateHealthIndex(long successCount, long totalCount) {
        if (totalCount == 0) return 0.0;
        return Math.min(100.0, (successCount / (double) totalCount) * 100.0);
    }

    private double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    @SuppressWarnings("deprecation")
    private StalkResponse mapToResponse(Stalk stalk) {
        return StalkResponse.builder()
                .id(stalk.getId())
                .url(stalk.getUrl())
                .nickname(stalk.getNickname())
                .growthIntervalSeconds(stalk.getGrowthIntervalSeconds())
                .timeoutSeconds(stalk.getTimeoutSeconds())
                .currentState(stalk.getCurrentState())
                .reliabilityState(stalk.getReliabilityState())
                .latencyState(stalk.getLatencyState())
                .healthIndex(stalk.getHealthIndex())
                .averageLatencyMs(stalk.getAverageLatencyMs())
                .consecutiveFailures(stalk.getConsecutiveFailures())
                .isActive(stalk.getIsActive())
                .createdAt(stalk.getCreatedAt())
                .build();
    }

    /**
     * Validates that the requested timeoutSeconds fits within the system's cycle ceiling.
     * The static DTO validation allows up to 120s, but the runtime ceiling depends on
     * monitoringProperties.maxCycleDuration. This check enforces the dynamic ceiling.
     */
    private void validateTimeoutAgainstCycle(int requestedTimeoutSeconds) {
        int maxAllowed = monitoringProperties.getMaxAllowedTimeoutSeconds();
        if (requestedTimeoutSeconds > maxAllowed) {
            throw new IllegalArgumentException(String.format(
                    "timeoutSeconds (%d) exceeds the system maximum of %d seconds. " +
                            "This is bounded by the scheduler's maxCycleDuration.",
                    requestedTimeoutSeconds, maxAllowed));
        }
    }
}