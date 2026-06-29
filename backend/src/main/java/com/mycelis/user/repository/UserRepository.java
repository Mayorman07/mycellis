package com.mycelis.user.repository;

import com.mycelis.user.constant.Status;
import com.mycelis.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    /**
     * Loads a user by email with roles AND authorities eagerly fetched in a single query.
     *
     * <p>Use this in authentication flows where the roles/authorities will be needed
     * (login, UserDetailsService). For non-auth lookups, prefer {@link #findByEmail(String)}
     * to avoid wasted joins.</p>
     */
    @Query("""
           SELECT DISTINCT u FROM User u
           LEFT JOIN FETCH u.roles r
           LEFT JOIN FETCH r.authorities
           WHERE u.email = :email
           """)
    Optional<User> findByEmailWithRolesAndAuthorities(@Param("email") String email);
    Optional<User> findByVerificationToken(String token);
    Optional<User> findByPasswordResetToken(String token);

    List<User> findAllByStatusAndLastLoggedInBefore(Status status, Instant cutoff);

    @Query("""
           SELECT u FROM User u WHERE
           LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
           LOWER(u.lastName)  LIKE LOWER(CONCAT('%', :keyword, '%')) OR
           LOWER(u.email)     LIKE LOWER(CONCAT('%', :keyword, '%'))
           """)
    Page<User> findAllByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
           SELECT u FROM User u WHERE
           u.lastLoggedIn < :inactiveSince
           AND u.lastLoggedIn IS NOT NULL
           AND (u.lastReactivationEmailSentDate IS NULL
                OR u.lastReactivationEmailSentDate < :reEmailCutoff)
           """)
    List<User> findUsersForReactivation(@Param("inactiveSince") Instant inactiveSince,
                                        @Param("reEmailCutoff") Instant reEmailCutoff);
}