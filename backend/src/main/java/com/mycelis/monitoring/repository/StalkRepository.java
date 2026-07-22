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
import java.util.Set;
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
     *
     * <p>Not tenant-scoped by itself — the caller supplies organizationId directly.
     * Regular controllers derive it from the authenticated principal; the
     * super-admin cross-tenant viewer passes an arbitrary org id instead,
     * which is safe there only because that endpoint is already gated by
     * super-admin authorization.</p>
     */
    Page<Stalk> findByOrganizationId(UUID organizationId, Pageable pageable);

    /** Stalk count for the super-admin org list/detail views. */
    long countByOrganizationId(UUID organizationId);

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

    /**
     * Filters the given ids down to only those owned by the organization, loading
     * full entities (not just ids) so callers have timeoutSeconds etc. available for
     * downstream per-pulse state derivation without a second query.
     *
     * <p>Used for tenant-safe batch operations where callers may supply ids belonging
     * to other orgs (or bogus ids) — those are silently dropped by the caller rather
     * than surfaced as an error.</p>
     */
    List<Stalk> findByIdInAndOrganizationId(Set<UUID> ids, UUID organizationId);
}