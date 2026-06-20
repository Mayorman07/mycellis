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
 *   <li>Strict tenant scoping (userId) on all read/write paths</li>
 *   <li>Atomic @Transactional boundaries for metric caching + state machine updates</li>
 *   <li>Pessimistic locking during scheduler fetch cycles to prevent duplicate execution</li>
 * </ul>
 * </p>
 *
 */
public interface StalkService {

    /**
     * Registers a new monitoring target for a tenant.
     *
     * @param userId authenticated tenant identifier
     * @param request validated configuration payload
     * @return full representation of the created stalk
     * @throws IllegalArgumentException if URL format or interval constraints violate policy
     */
    StalkResponse createStalk(UUID userId, CreateStalkRequest request);

    /**
     * Retrieves a single stalk by identifier, scoped to tenant.
     *
     * @param userId authenticated tenant identifier
     * @param id target identifier
     * @return stalk representation
     * @throws SecurityException if stalk does not belong to requesting tenant
     * @throws IllegalArgumentException if stalk not found
     */
    StalkResponse getStalkById(UUID userId, UUID id);

    /**
     * Returns paginated stalks for dashboard rendering.
     *
     * @param userId authenticated tenant identifier
     * @param pageable pagination & sorting configuration
     * @return page of stalk representations
     */
    Page<StalkResponse> getAllStalks(UUID userId, Pageable pageable);

    /**
     * Updates scheduling or timeout configuration.
     * State and cached metrics are preserved; next_check_at is recalculated.
     *
     * @param userId authenticated tenant identifier
     * @param id target identifier
     * @param request validated update payload
     * @return updated stalk representation
     */
    StalkResponse updateConfiguration(UUID userId, UUID id, CreateStalkRequest request);

    /**
     * Permanently removes a stalk and cascades to associated pulse records.
     *
     * @param userId authenticated tenant identifier
     * @param id target identifier
     */
    void deleteStalk(UUID userId, UUID id);

    /**
     * INTERNAL: Invoked by PulseEngine post-check.
     * Recalculates sliding-window metrics and transitions state machine.
     * Must execute within the same transaction as pulse persistence.
     *
     * @param stalkId target identifier
     * @param checkCompletedAt timestamp of the finished HTTP probe
     */
    void updateMetricsAndTransitionState(UUID stalkId, Instant checkCompletedAt);
}