package com.mycelis.monitoring.service;

import com.mycelis.monitoring.constant.LatencyState;
import com.mycelis.monitoring.constant.ReliabilityState;
import com.mycelis.monitoring.dto.responses.PulseResponse;
import com.mycelis.monitoring.entity.Pulse;
import com.mycelis.monitoring.entity.Stalk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Maps Pulse entities to PulseResponse, computing reliabilityState and latencyState
 * at mapping time from the pulse's raw signals plus the parent stalk's timeout config.
 *
 * <p>These two axes are never persisted on the pulse itself — they're derived fresh
 * on every read, consistent with how Stalk exposes its own two-axis state. Kept as
 * the single place this if-else logic lives; callers must not duplicate it.</p>
 */
@Slf4j
@Component
public class PulseMapper {

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

    private ReliabilityState deriveReliabilityState(Pulse pulse, Stalk stalk) {
        Boolean isSuccess = pulse.getIsSuccess();
        Integer statusCode = pulse.getStatusCode();
        Long latencyMs = pulse.getLatencyMs();

        if (Boolean.FALSE.equals(isSuccess)) {
            return ReliabilityState.DOWN;
        }
        if (Boolean.TRUE.equals(isSuccess) && statusCode != null && statusCode >= 500) {
            return ReliabilityState.DOWN;
        }
        if (Boolean.TRUE.equals(isSuccess) && statusCode != null && statusCode >= 400) {
            return ReliabilityState.DEGRADED;
        }
        if (Boolean.TRUE.equals(isSuccess) && latencyMs != null && latencyMs > degradedLatencyThresholdMs(stalk)) {
            return ReliabilityState.DEGRADED;
        }
        return ReliabilityState.HEALTHY;
    }

    private LatencyState deriveLatencyState(Pulse pulse, Stalk stalk) {
        Long latencyMs = pulse.getLatencyMs();
        if (latencyMs != null && latencyMs > stressedLatencyThresholdMs(stalk)) {
            return LatencyState.STRESSED;
        }
        return LatencyState.NORMAL;
    }

    // 75% of the stalk's timeout, in ms — a pulse this slow counts as DEGRADED even on success.
    private long degradedLatencyThresholdMs(Stalk stalk) {
        return stalk.getTimeoutSeconds() * 750L;
    }

    // 50% of the stalk's timeout, in ms — the STRESSED latency threshold.
    private long stressedLatencyThresholdMs(Stalk stalk) {
        return stalk.getTimeoutSeconds() * 500L;
    }
}
