package com.mycelis.organization.repository;

import com.mycelis.organization.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    Optional<Organization> findBySlug(String slug);

    boolean existsBySlug(String slug);

    /**
     * Super-admin org list: member + stalk counts aggregated in SQL (one
     * query total) rather than fetched and counted per-org in Java.
     * Row shape: id, name, slug, plan_tier, created_at, member_count, stalk_count.
     */
    @Query(value = """
        SELECT
            o.id AS id,
            o.name AS name,
            o.slug AS slug,
            o.plan_tier AS plan_tier,
            o.created_at AS created_at,
            COUNT(DISTINCT m.id) AS member_count,
            COUNT(DISTINCT s.id) AS stalk_count
        FROM organizations o
        LEFT JOIN memberships m ON m.organization_id = o.id
        LEFT JOIN stalks s ON s.organization_id = o.id
        GROUP BY o.id, o.name, o.slug, o.plan_tier, o.created_at
        ORDER BY o.created_at DESC
        """, nativeQuery = true)
    List<Object[]> findAllWithCounts();
}