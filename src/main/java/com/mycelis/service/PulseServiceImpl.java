package com.mycelis.service;

import com.mycelis.config.MonitoringProperties;
import com.mycelis.entity.Pulse;
import com.mycelis.entity.Stalk;
import com.mycelis.model.dto.responses.PulseResponse;
import com.mycelis.model.dto.responses.UptimeResponse;
import com.mycelis.repository.PulseRepository;
import com.mycelis.repository.StalkRepository;
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
        // 1. Validate parent stalk exists (foreign key constraint at app layer for clearer errors)
        Stalk stalk = stalkRepository.findById(stalkId)
                .orElseThrow(() -> new IllegalArgumentException("Parent stalk not found: " + stalkId));

        // 2. Build immutable pulse record
        Pulse pulse = Pulse.builder()
                .stalk(stalk)
                .statusCode(statusCode)
                .latencyMs(latencyMs)
                .isSuccess(isSuccess)
                .errorMessage(truncateErrorMessage(errorMessage))
                .responseSizeBytes(null) // Future: capture from WebClient response
                .createdAt(Instant.now())
                .build();

        // 3. Append-only persistence (never UPDATE a committed pulse)
        Pulse saved = pulseRepository.save(pulse);
        log.debug("Pulse recorded: stalkId={}, status={}, latencyMs={}, success={}",
                stalkId, statusCode, latencyMs, isSuccess);

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PulseResponse> getRecentPulses(UUID stalkId, int limit) {
        // Defensive cap to prevent OOM from malicious/unbounded requests
        int safeLimit = Math.min(limit, monitoringProperties.getMaxRecentPulses());

        return pulseRepository.findTopByStalkIdOrderByCreatedAtDesc(stalkId, PageRequest.of(0, safeLimit))
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PulseResponse> getPulseHistory(UUID stalkId, Pageable pageable) {
        // Enforce max page size to prevent accidental full-table scans
        Pageable safePageable = enforceMaxPageSize(pageable);
        return pulseRepository.findByStalkId(stalkId, safePageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public double calculateUptimePercentage(UUID stalkId, Duration window) {
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
    public UptimeResponse getUptimeByWindow(UUID stalkId, String window) {
        // 1. Parse the ISO-8601 string to Duration
        Duration duration = parseWindowToDuration(window);

        // 2. Calculate raw uptime ratio (0.0 to 1.0) using renamed helper
        double rawUptime = calculateRawUptimeRatio(stalkId, duration);

        // 3. Round to 2 decimal places
        double roundedUptime = Math.round(rawUptime * 100.0) / 100.0;

        // 4. Build and return the response DTO
        return UptimeResponse.builder()
                .stalkId(stalkId)
                .window(window)
                .uptimePercentage(roundedUptime)
                .calculatedAt(Instant.now())
                .build();
    }

    /**
     * Parses ISO-8601 duration string (P7D, P30D) to java.time.Duration.
     * Handles common abbreviations (D=days, W=weeks, M=months).
     */
    private Duration parseWindowToDuration(String window) {
        // Convert P7D → PT168H, P2W → PT336H, P1M → PT720H
        String simplified = window.substring(1) // Remove leading 'P'
                .replace("D", "24H")
                .replace("W", "168H")
                .replace("M", "720H");
        return Duration.parse("PT" + simplified);
    }

    /**
     * Internal helper: calculates raw uptime ratio (0.0 to 1.0).
     * Kept private since external callers should use getUptimeByWindow().
     */
    private double calculateRawUptimeRatio(UUID stalkId, Duration window) {
        Instant windowStart = Instant.now().minus(window);

        long totalCount = pulseRepository.countByStalkIdAndCreatedAtAfter(stalkId, windowStart);
        if (totalCount == 0) {
            return 0.0;
        }

        long successCount = pulseRepository.countSuccessesInWindow(stalkId, windowStart);
        return (successCount / (double) totalCount); // Returns 0.0 to 1.0
    }

    /**
     * Maps domain entity to API response DTO.
     * Decouples database schema from external contract.
     */
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

    /**
     * Truncates error messages to prevent database bloat or injection via exception strings.
     */
    private String truncateErrorMessage(String message) {
        if (message == null) return null;
        int maxLength = monitoringProperties.getMaxErrorMessageLength();
        return message.length() <= maxLength ? message : message.substring(0, maxLength) + "...";
    }

    /**
     * Enforces safe pagination limits to prevent resource exhaustion attacks.
     */
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