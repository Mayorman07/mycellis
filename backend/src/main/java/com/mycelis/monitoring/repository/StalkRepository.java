package com.mycelis.monitoring.repository;

import com.mycelis.monitoring.entity.Stalk;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data access layer for Stalk entities.
 * Optimized for scheduler-based distributed processing using Postgres SKIP LOCKED.
 *
 * <p>All tenant-scoped queries filter by {@code organizationId}, not by user id.
 * User id (as {@code createdByUserId}) is audit metadata only.</p>
 */
@Repository
public interface StalkRepository extends JpaRepository<Stalk, UUID> {

    /**
     * Atomically claims due stalks for processing.
     * Cross-tenant by design — the scheduler processes ALL orgs' stalks.
     */
    @Query(value = """
        SELECT *
        FROM stalks
        WHERE next_check_at <= :now
          AND is_active = true
          AND reliability_state != 'DORMANT'
        ORDER BY next_check_at ASC
        LIMIT :limit
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<Stalk> findDueForCheck(@Param("now") Instant now, @Param("limit") int limit);

    /**
     * Tenant-safe lookup by organization.
     */
    Optional<Stalk> findByIdAndOrganizationId(UUID id, UUID organizationId);

    /**
     * Organization-scoped pagination for the dashboard.
     */
    Page<Stalk> findByOrganizationId(UUID organizationId, Pageable pageable);

    /**
     * Reschedules next execution time after a completed check.
     * Bulk JPQL bypasses @UpdateTimestamp so updatedAt is set manually.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE Stalk s
        SET s.nextCheckAt = :nextCheckAt,
            s.lastCheckedAt = :now,
            s.updatedAt = :now
        WHERE s.id = :id
    """)
    int rescheduleAfterCheck(@Param("id") UUID id,
                             @Param("nextCheckAt") Instant nextCheckAt,
                             @Param("now") Instant now);

    /**
     * Counts pending work for backlog monitoring.
     */
    @Query(value = """
        SELECT COUNT(*)
        FROM stalks
        WHERE next_check_at <= :now
          AND is_active = true
          AND reliability_state != 'DORMANT'
        """, nativeQuery = true)
    long countDueForCheck(@Param("now") Instant now);
}