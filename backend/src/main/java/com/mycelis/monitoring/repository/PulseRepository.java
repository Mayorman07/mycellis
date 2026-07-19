package com.mycelis.monitoring.repository;

import com.mycelis.monitoring.entity.Pulse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Data access layer for Pulse entities.
 * Optimized for time-series retrieval and sliding-window metric aggregation.
 */
@Repository
public interface PulseRepository extends JpaRepository<Pulse, UUID> {

    @Query("""
        SELECT p FROM Pulse p
        WHERE p.stalk.id = :stalkId
        ORDER BY p.createdAt DESC
        """)
    List<Pulse> findTop10ByStalkIdOrderByCreatedAtDesc(@Param("stalkId") UUID stalkId, Pageable pageable);

    Page<Pulse> findByStalkId(UUID stalkId, Pageable pageable);

    @Query("""
        SELECT COUNT(p) FROM Pulse p
        WHERE p.stalk.id = :stalkId
          AND p.isSuccess = true
          AND p.createdAt >= :windowStart
        """)
    long countSuccessesInWindow(@Param("stalkId") UUID stalkId,
                                @Param("windowStart") java.time.Instant windowStart);

    @Query("""
        SELECT AVG(p.latencyMs) FROM Pulse p
        WHERE p.stalk.id = :stalkId
          AND p.isSuccess = true
          AND p.createdAt >= :windowStart
        """)
    Double calculateAvgLatencyInWindow(@Param("stalkId") UUID stalkId,
                                       @Param("windowStart") java.time.Instant windowStart);

    /**
     * Retrieves recent pulses ordered by recency (descending).
     * Uses index on (stalk_id, created_at) for O(log n) performance.
     */
    @Query("""
    SELECT p FROM Pulse p
    WHERE p.stalk.id = :stalkId
    ORDER BY p.createdAt DESC
    """)
    List<Pulse> findTopByStalkIdOrderByCreatedAtDesc(@Param("stalkId") UUID stalkId, Pageable pageable);

    /**
     * Counts total pulses in a time window for uptime calculation.
     * Partition-pruned query for scalable time-series analytics.
     */
    @Query("""
    SELECT COUNT(p) FROM Pulse p
    WHERE p.stalk.id = :stalkId
      AND p.createdAt >= :windowStart
    """)
    long countByStalkIdAndCreatedAtAfter(@Param("stalkId") UUID stalkId,
                                         @Param("windowStart") Instant windowStart);

    @Query("""
    SELECT COUNT(p) FROM Pulse p
    WHERE p.stalk.id = :stalkId
    AND p.createdAt >= :windowStart
    """)
    long countTotalInWindow(@Param("stalkId") UUID stalkId, @Param("windowStart") Instant windowStart);

    /**
     * Fetches the most recent {@code limit} pulses per stalk across multiple stalks
     * in a single query. ROW_NUMBER() partitions by stalk_id so Postgres computes
     * the top-N-per-group ranking in one partition walk instead of one query per stalk.
     */
    @Query(value = """
        SELECT * FROM (
            SELECT
                p.*,
                ROW_NUMBER() OVER (PARTITION BY p.stalk_id ORDER BY p.created_at DESC) AS rn
            FROM pulses p
            WHERE p.stalk_id IN (:stalkIds)
        ) ranked
        WHERE ranked.rn <= :limit
        ORDER BY ranked.stalk_id, ranked.created_at DESC
        """, nativeQuery = true)
    List<Pulse> findRecentByStalkIds(@Param("stalkIds") Set<UUID> stalkIds, @Param("limit") int limit);

    /**
     * Daily pulse totals + successes for the public status page's uptime
     * history. Aggregated in SQL (one query per stalk) rather than fetching
     * every pulse row to Java — a stalk on a 60s interval can log thousands
     * of pulses across a 90-day window.
     *
     * <p>Only returns rows for days that had at least one pulse; days with
     * zero pulses are simply absent — the caller fills those in as "no
     * data" rather than assuming 0%.</p>
     */
    @Query(value = """
        SELECT
            DATE(created_at) AS day,
            COUNT(*) AS total,
            COUNT(*) FILTER (WHERE is_success = true) AS successful
        FROM pulses
        WHERE stalk_id = :stalkId
          AND created_at >= :windowStart
        GROUP BY DATE(created_at)
        ORDER BY DATE(created_at)
        """, nativeQuery = true)
    List<Object[]> findDailyUptimeAggregates(@Param("stalkId") UUID stalkId,
                                              @Param("windowStart") Instant windowStart);
}
