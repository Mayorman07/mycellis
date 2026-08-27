package com.mycelis.monitoring.quota;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Free tier's stalk cap. Applied unconditionally to every organization
 * today — there's no tier dispatch yet, so this is the only policy in play
 * regardless of an org's actual PlanTier.
 */
@Component
public class FreeTierStalkQuotaPolicy implements StalkQuotaPolicy {

    private static final int FREE_TIER_MAX_STALKS = 20;

    @Override
    public int maxStalks(UUID organizationId) {
        return FREE_TIER_MAX_STALKS;
    }
}
