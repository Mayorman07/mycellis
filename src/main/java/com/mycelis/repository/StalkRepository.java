package com.mycelis.repository;

import com.mycelis.entity.Stalk;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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
 * Optimized for scheduler queries, state locking, and user-scoped retrieval.
 */
@Repository
public interface StalkRepository extends JpaRepository<Stalk, UUID> {

    /**
     * Retrieves stalks due for health checks.
     * Uses pessimistic locking to prevent concurrent scheduler races.
     */
    @Query("""
        SELECT s FROM Stalk s
        WHERE s.nextCheckAt <= :now
          AND s.isActive = true
          AND s.currentState != 'DORMANT'
        ORDER BY s.nextCheckAt ASC
        """)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Stalk> findDueForCheck(@Param("now") Instant now);

    Optional<Stalk> findByIdAndUserId(UUID id, UUID userId);

    Page<Stalk> findByUserId(UUID userId, Pageable pageable);

    @Modifying
    @Query("""
        UPDATE Stalk s
        SET s.nextCheckAt = :nextCheckAt,
            s.updatedAt = :now
        WHERE s.id = :id
        """)
    void updateNextCheckAt(@Param("id") UUID id,
                           @Param("nextCheckAt") Instant nextCheckAt,
                           @Param("now") Instant now);
}