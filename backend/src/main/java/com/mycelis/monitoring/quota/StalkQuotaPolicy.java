package com.mycelis.monitoring.quota;

import java.util.UUID;

/**
 * Determines how many stalks an organization is allowed to have at once.
 * One implementation exists today — {@link FreeTierStalkQuotaPolicy},
 * applied unconditionally to every organization regardless of actual plan
 * tier. See the TODO at its injection site in StalkServiceImpl for the
 * planned tier-dispatch follow-up once paid tiers launch.
 */
public interface StalkQuotaPolicy {
    int maxStalks(UUID organizationId);
}
