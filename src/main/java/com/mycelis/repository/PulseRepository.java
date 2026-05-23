package com.mycelis.repository;

import com.mycelis.entity.Pulse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
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
}
