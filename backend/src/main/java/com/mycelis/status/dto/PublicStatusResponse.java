package com.mycelis.status.dto;

import java.time.Instant;
import java.util.List;

/**
 * Public, unauthenticated response for a workspace's status page.
 *
 * <p>Deliberately excludes anything that isn't needed to display status
 * publicly: stalk URLs (privacy — visitors shouldn't learn which endpoints
 * a workspace monitors), stalk/organization ids, plan tier, member count.</p>
 */
public record PublicStatusResponse(
        OrganizationSummary organization,
        String overallState,
        List<StalkStatus> stalks,
        Instant lastUpdated
) {
    public record OrganizationSummary(
            String name,
            String slug
    ) {}

    public record StalkStatus(
            String nickname,
            String reliabilityState,
            String latencyState,
            Double healthIndex,
            Long averageLatencyMs,
            List<Double> uptimeHistory90d
    ) {}
}
