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
 */
@Repository
public interface StalkRepository extends JpaRepository<Stalk, UUID> {

    /**
     * Atomically claims due stalks for processing.
     * Uses pure native Postgres SKIP LOCKED to ensure horizontal scalability without blocking.
     * No JPA locking annotations are used to prevent SQL syntax clashes.
     *
     * @param now Current timestamp to filter next_check_at
     * @param limit Max number of stalks to fetch (backpressure control)
     * @return List of safely locked stalks ready for processing
     */
    @Query(value = """
        SELECT *
        FROM stalks
        WHERE next_check_at <= :now
          AND is_active = true
          AND current_state != 'DORMANT'
        ORDER BY next_check_at ASC
        LIMIT :limit
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<Stalk> findDueForCheck(@Param("now") Instant now, @Param("limit") int limit);

    /**
     * Tenant-safe lookup.
     */
    Optional<Stalk> findByIdAndUserId(UUID id, UUID userId);

    /**
     * User-scoped pagination.
     */
    Page<Stalk> findByUserId(UUID userId, Pageable pageable);

    /**
     * Reschedules next execution time after a completed check.
     * * Note: Bulk JPQL queries bypass Hibernate's @UpdateTimestamp lifecycle events,
     * so updatedAt must be manually assigned here to ensure the audit trail remains accurate.
     *
     * @param id Stalk ID
     * @param nextCheckAt New scheduled check time (with jitter)
     * @param now Current timestamp for audit trail
     * @return The number of rows updated (should be 1)
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
     * Used to expose the 'app.scheduler.backlog.count' metric to Prometheus.
     */
    @Query(value = """
        SELECT COUNT(*)
        FROM stalks
        WHERE next_check_at <= :now
          AND is_active = true
          AND current_state != 'DORMANT'
        """, nativeQuery = true)
    long countDueForCheck(@Param("now") Instant now);
}