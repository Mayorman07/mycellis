package com.mycelis.user.repository;

import com.mycelis.user.entity.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, UUID> {

    /**
     * Count failed attempts for a specific (email, ip) pair within a time window.
     */
    @Query("""
           SELECT COUNT(la) FROM LoginAttempt la
           WHERE la.email = :email
             AND la.ipAddress = :ipAddress
             AND la.failedAt >= :windowStart
           """)
    long countRecentFailures(@Param("email") String email,
                             @Param("ipAddress") String ipAddress,
                             @Param("windowStart") Instant windowStart);

    /**
     * Clear all failures for a (email, ip) pair — called on successful login
     * so honest users aren't penalized for fumble-fingering their password.
     */
    @Modifying
    @Query("""
           DELETE FROM LoginAttempt la
           WHERE la.email = :email
             AND la.ipAddress = :ipAddress
           """)
    void deleteByEmailAndIp(@Param("email") String email,
                            @Param("ipAddress") String ipAddress);

    /**
     * Cleanup: delete all rows older than the retention cutoff.
     * Called by a scheduled job.
     */
    @Modifying
    @Query("DELETE FROM LoginAttempt la WHERE la.failedAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);
}