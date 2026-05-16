package com.mycelis.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
