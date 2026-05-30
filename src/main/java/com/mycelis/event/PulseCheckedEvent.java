package com.mycelis.event;


import lombok.Builder;
import lombok.Getter;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable event published when a health check completes.
 * Contains all data needed for downstream processing.
 */
@Getter
@Builder
public class PulseCheckedEvent {
    private final UUID stalkId;
    private final UUID userId;          // For tenant isolation in listeners
    private final int statusCode;
    private final long latencyMs;
    private final boolean isSuccess;
    private final String errorMessage;  // null if success
    private final Instant checkedAt;
    private final String urlHash;       // For metrics (low-cardinality)
}
