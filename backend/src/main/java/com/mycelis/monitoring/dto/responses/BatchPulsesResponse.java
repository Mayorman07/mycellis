package com.mycelis.monitoring.dto.responses;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * API response for the batch pulses endpoint.
 * Keys are limited to stalk ids the caller's organization actually owns —
 * foreign-org or unknown ids are silently absent, never surfaced as an error.
 */
@Data
@Builder
public class BatchPulsesResponse {
    private Map<UUID, List<PulseResponse>> pulsesByStalkId;
}
