package com.mycelis.monitoring.service;

import com.mycelis.shared.config.MonitoringProperties;
import com.mycelis.shared.exception.ResourceNotFoundException;
import com.mycelis.shared.exception.TenantAccessException;
import com.mycelis.monitoring.entity.Pulse;
import com.mycelis.monitoring.entity.Stalk;
import com.mycelis.monitoring.dto.responses.PulseResponse;
import com.mycelis.monitoring.dto.responses.UptimeResponse;
import com.mycelis.monitoring.repository.PulseRepository;
import com.mycelis.monitoring.repository.StalkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Production implementation for diagnostic record ingestion & time-series analytics.
 * Enforces append-only persistence, partition-aware queries, and tenant-safe retrieval.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PulseServiceImpl implements PulseService {

    private final PulseRepository pulseRepository;
    private final StalkRepository stalkRepository;
    private final MonitoringProperties monitoringProperties;

    @Override
    @Transactional
    public PulseResponse recordCheckResult(UUID stalkId, int statusCode, long latencyMs,
                                           boolean isSuccess, String errorMessage) {
        Stalk stalk = stalkRepository.findById(stalkId)
                .orElseThrow(() -> new IllegalArgumentException("Parent stalk not found: " + stalkId));

        Pulse pulse = Pulse.builder()
                .stalk(stalk)
                .statusCode(statusCode)
                .latencyMs(latencyMs)
                .isSuccess(isSuccess)
                .errorMessage(truncateErrorMessage(errorMessage))
                .responseSizeBytes(null)
                .createdAt(Instant.now())
                .build();

        Pulse saved = pulseRepository.save(pulse);
        log.debug("Pulse recorded: stalkId={}, status={}, latencyMs={}, success={}",
                stalkId, statusCode, latencyMs, isSuccess);

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PulseResponse> getRecentPulses(UUID userId, UUID stalkId, int limit) {
        verifyStalkOwnership(userId, stalkId);
        int safeLimit = Math.min(limit, monitoringProperties.getMaxRecentPulses());
        return pulseRepository.findTopByStalkIdOrderByCreatedAtDesc(stalkId, PageRequest.of(0, safeLimit))
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PulseResponse> getPulseHistory(UUID userId, UUID stalkId, Pageable pageable) {
        verifyStalkOwnership(userId, stalkId);
        Pageable safePageable = enforceMaxPageSize(pageable);
        return pulseRepository.findByStalkId(stalkId, safePageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public double calculateUptimePercentage(UUID userId, UUID stalkId, Duration window) {
        verifyStalkOwnership(userId, stalkId);
        Instant windowStart = Instant.now().minus(window);

        long totalCount = pulseRepository.countByStalkIdAndCreatedAtAfter(stalkId, windowStart);
        if (totalCount == 0) {
            log.debug("No pulses found for stalk {} in window {}", stalkId, window);
            return 0.0;
        }

        long successCount = pulseRepository.countSuccessesInWindow(stalkId, windowStart);
        double uptimePercentage = (successCount / (double) totalCount) * 100.0;

        log.debug("Uptime: stalkId={}, window={}, successCount={}, totalCount={}, uptime={}%",
                stalkId, window, successCount, totalCount, uptimePercentage);

        return uptimePercentage;
    }

    @Override
    @Transactional(readOnly = true)
    public UptimeResponse getUptimeByWindow(UUID userId, UUID stalkId, String window) {
        verifyStalkOwnership(userId, stalkId);

        Duration duration = parseWindowToDuration(window);
        double rawUptime = calculateRawUptimeRatio(stalkId, duration);
        double roundedUptime = Math.round(rawUptime * 100.0) / 100.0;

        return UptimeResponse.builder()
                .stalkId(stalkId)
                .window(window)
                .uptimePercentage(roundedUptime)
                .calculatedAt(Instant.now())
                .build();
    }

    /**
     * Verifies the stalk exists and belongs to the requesting user.
     * Throws ResourceNotFoundException if missing, TenantAccessException if cross-tenant.
     */
    private void verifyStalkOwnership(UUID userId, UUID stalkId) {
        Stalk stalk = stalkRepository.findById(stalkId)
                .orElseThrow(() -> new ResourceNotFoundException("Stalk", stalkId.toString()));

        if (!stalk.getUserId().equals(userId)) {
            throw new TenantAccessException("Access denied: Stalk does not belong to tenant " + userId);
        }
    }

    private Duration parseWindowToDuration(String window) {
        String simplified = window.substring(1)
                .replace("D", "24H")
                .replace("W", "168H")
                .replace("M", "720H");
        return Duration.parse("PT" + simplified);
    }

    private double calculateRawUptimeRatio(UUID stalkId, Duration window) {
        Instant windowStart = Instant.now().minus(window);

        long totalCount = pulseRepository.countByStalkIdAndCreatedAtAfter(stalkId, windowStart);
        if (totalCount == 0) {
            return 0.0;
        }

        long successCount = pulseRepository.countSuccessesInWindow(stalkId, windowStart);
        return (successCount / (double) totalCount);
    }

    private PulseResponse mapToResponse(Pulse pulse) {
        return PulseResponse.builder()
                .id(pulse.getId())
                .stalkId(pulse.getStalk().getId())
                .statusCode(pulse.getStatusCode())
                .latencyMs(pulse.getLatencyMs())
                .isSuccess(pulse.getIsSuccess())
                .errorMessage(pulse.getErrorMessage())
                .responseSizeBytes(pulse.getResponseSizeBytes())
                .createdAt(pulse.getCreatedAt())
                .build();
    }

    private String truncateErrorMessage(String message) {
        if (message == null) return null;
        int maxLength = monitoringProperties.getMaxErrorMessageLength();
        return message.length() <= maxLength ? message : message.substring(0, maxLength) + "...";
    }

    private Pageable enforceMaxPageSize(Pageable pageable) {
        int maxPageSize = monitoringProperties.getMaxHistoryPageSize();
        if (pageable.getPageSize() > maxPageSize) {
            log.warn("Page size {} exceeded limit {}; capping to {}",
                    pageable.getPageSize(), maxPageSize, maxPageSize);
            return PageRequest.of(pageable.getPageNumber(), maxPageSize, pageable.getSort());
        }
        return pageable;
    }
}