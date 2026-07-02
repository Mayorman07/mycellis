package com.mycelis.monitoring.service;

import com.mycelis.monitoring.dto.requests.CreateStalkRequest;
import com.mycelis.monitoring.dto.responses.StalkResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.UUID;

/**
 * Contract for Stalk domain operations.
 * Governs target registration, configuration lifecycle, multi-tenant isolation,
 * and scheduler-driven metric/state transitions.
 *
 * <p>Implementations must enforce:
 * <ul>
 *   <li>Strict tenant scoping by {@code organizationId} on all read/write paths</li>
 *   <li>Atomic @Transactional boundaries for metric caching + state machine updates</li>
 *   <li>Pessimistic locking during scheduler fetch cycles to prevent duplicate execution</li>
 * </ul>
 * </p>
 *
 * <p><b>Tenancy note:</b> access control is scoped by {@code organizationId}, NOT by user id.
 * {@code createdByUserId} is retained only as audit metadata (who originally created the stalk).</p>
 */
public interface StalkService {

    /**
     * Registers a new monitoring target for an organization.
     *
     * @param organizationId  tenant identifier (from authenticated principal)
     * @param createdByUserId audit trail — which user created it (from authenticated principal)
     * @param request         validated configuration payload
     * @return full representation of the created stalk
     * @throws IllegalArgumentException if URL format or interval constraints violate policy
     */
    StalkResponse createStalk(UUID organizationId, UUID createdByUserId, CreateStalkRequest request);

    /**
     * Retrieves a single stalk by identifier, scoped to the organization.
     *
     * @param organizationId authenticated caller's tenant identifier
     * @param id target identifier
     * @return stalk representation
     * @throws com.mycelis.shared.exception.TenantAccessException if stalk does not belong to organization
     * @throws IllegalArgumentException if stalk not found
     */
    StalkResponse getStalkById(UUID organizationId, UUID id);

    /**
     * Returns paginated stalks for dashboard rendering, scoped to the organization.
     *
     * @param organizationId authenticated caller's tenant identifier
     * @param pageable pagination & sorting configuration
     * @return page of stalk representations
     */
    Page<StalkResponse> getAllStalks(UUID organizationId, Pageable pageable);

    /**
     * Updates scheduling or timeout configuration.
     * State and cached metrics are preserved; next_check_at is recalculated.
     *
     * @param organizationId authenticated caller's tenant identifier
     * @param id target identifier
     * @param request validated update payload
     * @return updated stalk representation
     */
    StalkResponse updateConfiguration(UUID organizationId, UUID id, CreateStalkRequest request);

    /**
     * Permanently removes a stalk and cascades to associated pulse records.
     *
     * @param organizationId authenticated caller's tenant identifier
     * @param id target identifier
     */
    void deleteStalk(UUID organizationId, UUID id);

    /**
     * INTERNAL: Invoked by PulseEngine post-check.
     * Recalculates sliding-window metrics and transitions state machine.
     * Must execute within the same transaction as pulse persistence.
     *
     * <p>Cross-tenant by design — the scheduler processes all orgs' stalks.
     * No tenant filter needed here.</p>
     *
     * @param stalkId target identifier
     * @param checkCompletedAt timestamp of the finished HTTP probe
     */
    void updateMetricsAndTransitionState(UUID stalkId, Instant checkCompletedAt);
}