package com.mycelis.monitoring.service;

import com.mycelis.shared.config.MonitoringProperties;
import com.mycelis.monitoring.constant.StalkState;
import com.mycelis.monitoring.constant.StateCategory;
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
    public StalkResponse createStalk(UUID userId, CreateStalkRequest request) {
        Stalk stalk = Stalk.builder()
                .userId(userId)
                .url(request.getUrl())
                .nickname(request.getNickname())
                .growthIntervalSeconds(request.getGrowthIntervalSeconds())
                .timeoutSeconds(request.getTimeoutSeconds())
                .currentState(StalkState.HEALTHY)
                .healthIndex(0.0)
                .consecutiveFailures(0)
                .isActive(true)
                .nextCheckAt(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        Stalk saved = stalkRepository.save(stalk);
        log.info("Stalk created: id={}, userId={}, url={}", saved.getId(), userId, saved.getUrl());
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public StalkResponse getStalkById(UUID userId, UUID id) {
        Stalk stalk = stalkRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Stalk not found: " + id));

        if (!stalk.getUserId().equals(userId)) {
            throw new TenantAccessException("Access denied: Stalk does not belong to tenant " + userId);
        }
        return mapToResponse(stalk);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StalkResponse> getAllStalks(UUID userId, Pageable pageable) {
        return stalkRepository.findByUserId(userId, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional
    public StalkResponse updateConfiguration(UUID userId, UUID id, CreateStalkRequest request) {
        Stalk stalk = stalkRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Stalk not found: " + id));

        if (!stalk.getUserId().equals(userId)) {
            throw new TenantAccessException("Access denied: Stalk does not belong to tenant " + userId);
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
    public void deleteStalk(UUID userId, UUID id) {
        Stalk stalk = stalkRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Stalk not found: " + id));

        if (!stalk.getUserId().equals(userId)) {
            throw new TenantAccessException("Access denied: Stalk does not belong to tenant " + userId);
        }

        stalkRepository.delete(stalk);
        log.info("Stalk deleted: id={}, userId={}", id, userId);
    }

    @Override
    @Transactional
    public void updateMetricsAndTransitionState(UUID stalkId, Instant checkCompletedAt) {
        Instant windowStart = checkCompletedAt.minus(
                Duration.ofMinutes(monitoringProperties.getSlidingWindowSize())
        );

        // Get metrics from the sliding window
        long successCount = pulseRepository.countSuccessesInWindow(stalkId, windowStart);
        long totalCount = pulseRepository.countTotalInWindow(stalkId, windowStart);  // ← NEW
        Double avgLatency = pulseRepository.calculateAvgLatencyInWindow(stalkId, windowStart);

        // Calculate health as success RATE (not raw count)
        double healthIndex = calculateHealthIndex(successCount, totalCount);
        StalkState newState = evaluateState(healthIndex, avgLatency, totalCount);
        // Fetch and update the stalk entity
        Stalk stalk = stalkRepository.findById(stalkId)
                .orElseThrow(() -> new IllegalArgumentException("Stalk not found: " + stalkId));

        stalk.setHealthIndex(roundToTwoDecimals(healthIndex));
        stalk.setAverageLatencyMs(avgLatency != null ? Math.round(avgLatency) : 0L);
        stalk.setLast10SuccessCount((int) successCount);
        stalk.setCurrentState(newState);
        stalk.setUpdatedAt(Instant.now());

        stalkRepository.save(stalk);

        log.info("📊 State updated: stalkId={}, health={}%, successes={}/{} → {}",
                stalkId, healthIndex, successCount, totalCount, newState);
    }

    /**
     * Evaluates stalk state using configurable thresholds and defensive validation.
     * Implements explicit state machine transitions with audit logging.
     */
    private StalkState evaluateState(double healthIndex, Double avgLatency, long totalCount) {
        validateHealthIndex(healthIndex);
        // No checks yet → DORMANT
        if (totalCount == 0) {
            return StalkState.DORMANT;
        }
        // All failures → DEGRADED
        if (healthIndex == 0.0) {
            return StalkState.DEGRADED;
        }
        // Now we have data and some successes
        return switch (getStateCategory(healthIndex)) {
            case HEALTHY_RANGE -> evaluateHealthyState(avgLatency);
            case DEGRADED_RANGE -> StalkState.DEGRADED;
            case LOW_HEALTH_RANGE -> StalkState.DEGRADED;  // Low but non-zero = failing
        };
    }

    /**
     * Categorizes health index into explicit ranges for clear state machine logic.
     */
    private StateCategory getStateCategory(double healthIndex) {
        if (healthIndex >= monitoringProperties.getHealthyThreshold()) {
            return StateCategory.HEALTHY_RANGE;
        }
        if (healthIndex > monitoringProperties.getDegradedThreshold()) {
            return StateCategory.DEGRADED_RANGE;
        }
        return StateCategory.LOW_HEALTH_RANGE;
    }

    /**
     * Determines if a healthy-range endpoint is STRESSED due to latency.
     */
    private StalkState evaluateHealthyState(Double avgLatency) {
        if (avgLatency != null && avgLatency >= monitoringProperties.getLatencyThresholdMs()) {
            log.debug("Latency threshold exceeded: {}ms >= {}ms → STRESSED",
                    avgLatency, monitoringProperties.getLatencyThresholdMs());
            return StalkState.STRESSED;
        }
        return StalkState.HEALTHY;
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
        if (totalCount == 0) return 0.0;  // No data yet → DORMANT
        return Math.min(100.0, (successCount / (double) totalCount) * 100.0);
    }

    private double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private StalkResponse mapToResponse(Stalk stalk) {
        return StalkResponse.builder()
                .id(stalk.getId())
                .url(stalk.getUrl())
                .nickname(stalk.getNickname())
                .growthIntervalSeconds(stalk.getGrowthIntervalSeconds())
                .timeoutSeconds(stalk.getTimeoutSeconds())
                .currentState(stalk.getCurrentState())
                .healthIndex(stalk.getHealthIndex())
                .averageLatencyMs(stalk.getAverageLatencyMs())
                .consecutiveFailures(stalk.getConsecutiveFailures())
                .isActive(stalk.getIsActive())
                .createdAt(stalk.getCreatedAt())
                .build();
    }
}