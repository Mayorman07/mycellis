package com.mycelis.monitoring.service;

import com.mycelis.monitoring.constant.LatencyState;
import com.mycelis.monitoring.constant.ReliabilityState;
import com.mycelis.monitoring.dto.responses.PulseResponse;
import com.mycelis.monitoring.entity.Pulse;
import com.mycelis.monitoring.entity.Stalk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Derives per-pulse reliability and latency state from raw signals.
 *
 * <p>Thresholds are absolute milliseconds based on user perception research:
 * a response above 1s reads as sluggish, above 3s as stressed, above 5s
 * as degraded regardless of the stalk's configured timeout. Timeout is
 * a hard ceiling, not a baseline expectation, so thresholds derived from
 * timeout ratio misclassify slow-but-successful pulses as healthy.</p>
 *
 * <p>Per-stalk configurable thresholds are a planned future enhancement -
 * the stalk parameter is retained on the derivation signature for that.</p>
 */
@Slf4j
@Component
public class PulseMapper {

    private static final int STRESSED_LATENCY_MS = 3000;
    private static final int DEGRADED_LATENCY_MS = 5000;
    private static final int SERVER_ERROR_STATUS = 500;
    private static final int CLIENT_ERROR_STATUS = 400;

    public PulseResponse toResponse(Pulse pulse, Stalk stalk) {
        ReliabilityState reliabilityState;
        LatencyState latencyState;

        if (stalk == null) {
            // Shouldn't happen — the batch/single-stalk pulse flows verify ownership
            // (and thus load the stalk) before any pulse is ever mapped.
            log.warn("Pulse {} mapped without its parent stalk in scope; defaulting to HEALTHY/NORMAL. " +
                    "This indicates a bug upstream.", pulse.getId());
            reliabilityState = ReliabilityState.HEALTHY;
            latencyState = LatencyState.NORMAL;
        } else {
            reliabilityState = deriveReliabilityState(pulse, stalk);
            latencyState = deriveLatencyState(pulse, stalk);
        }

        return PulseResponse.builder()
                .id(pulse.getId())
                .stalkId(pulse.getStalk().getId())
                .statusCode(pulse.getStatusCode())
                .latencyMs(pulse.getLatencyMs())
                .isSuccess(pulse.getIsSuccess())
                .errorMessage(pulse.getErrorMessage())
                .responseSizeBytes(pulse.getResponseSizeBytes())
                .createdAt(pulse.getCreatedAt())
                .reliabilityState(reliabilityState)
                .latencyState(latencyState)
                .build();
    }

    // stalk is unused today (thresholds are absolute) but kept on the signature for
    // the planned per-stalk configurable thresholds — see class Javadoc.
    private ReliabilityState deriveReliabilityState(Pulse pulse, Stalk stalk) {
        Boolean isSuccess = pulse.getIsSuccess();
        Integer statusCode = pulse.getStatusCode();
        Long latencyMs = pulse.getLatencyMs();

        if (Boolean.FALSE.equals(isSuccess)) {
            return ReliabilityState.DOWN;
        }
        if (Boolean.TRUE.equals(isSuccess) && statusCode != null && statusCode >= SERVER_ERROR_STATUS) {
            return ReliabilityState.DOWN;
        }
        if (Boolean.TRUE.equals(isSuccess) && statusCode != null && statusCode >= CLIENT_ERROR_STATUS) {
            return ReliabilityState.DEGRADED;
        }
        if (Boolean.TRUE.equals(isSuccess) && latencyMs != null && latencyMs > DEGRADED_LATENCY_MS) {
            return ReliabilityState.DEGRADED;
        }
        return ReliabilityState.HEALTHY;
    }

    private LatencyState deriveLatencyState(Pulse pulse, Stalk stalk) {
        Long latencyMs = pulse.getLatencyMs();
        if (latencyMs != null && latencyMs > STRESSED_LATENCY_MS) {
            return LatencyState.STRESSED;
        }
        return LatencyState.NORMAL;
    }
}
