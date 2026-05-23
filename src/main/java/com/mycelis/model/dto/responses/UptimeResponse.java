package com.mycelis.model.dto.responses;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/**
 * API response for uptime calculation endpoints.
 * Encapsulates windowed success ratio with audit metadata.
 *
 */
@Data
@Builder
public class UptimeResponse {
    /** Parent monitoring target identifier */
    private UUID stalkId;

    /** Evaluated time window (ISO-8601 format) */
    private String window;

    /** Success ratio between 0.00 and 100.00 */
    private Double uptimePercentage;

    /** Calculation timestamp (UTC) */
    private Instant calculatedAt;
}