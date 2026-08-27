package com.mycelis.monitoring.quota;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FreeTierStalkQuotaPolicyTest {

    private final FreeTierStalkQuotaPolicy policy = new FreeTierStalkQuotaPolicy();

    @Test
    void capsAt20() {
        assertThat(policy.maxStalks(UUID.randomUUID())).isEqualTo(20);
    }

    @Test
    void capIsTheSameForAnyOrganization() {
        UUID orgA = UUID.randomUUID();
        UUID orgB = UUID.randomUUID();
        assertThat(policy.maxStalks(orgA)).isEqualTo(policy.maxStalks(orgB));
    }
}
