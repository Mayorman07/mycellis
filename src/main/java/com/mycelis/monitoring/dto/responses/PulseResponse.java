package com.mycelis.monitoring.dto.responses;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/**
 * API response representation of a single health check execution (Pulse).
 * Used for recent check retrieval, historical analytics, and real-time dashboard rendering.
 *
 * <p>Designed as an immutable data carrier with strict field contracts to prevent
 * serialization ambiguity and ensure consistent frontend consumption across
 * paginated history and streaming metrics endpoints.</p>
 *
 */
@Data
@Builder
public class PulseResponse {

    /** Unique identifier for the diagnostic record */
    private UUID id;

    /** Parent monitoring target identifier (foreign key to stalks) */
    private UUID stalkId;

    /** HTTP response status code returned by the target endpoint */
    private Integer statusCode;

    /** Round-trip network latency in milliseconds */
    private Long latencyMs;

    /** Boolean flag indicating success (2xx/3xx range) or failure */
    private Boolean isSuccess;

    /** Human-readable error description or exception message (nullable) */
    private String errorMessage;

    /** Payload size of the HTTP response in bytes (nullable) */
    private Long responseSizeBytes;

    /** Immutable execution timestamp (UTC) */
    private Instant createdAt;
}