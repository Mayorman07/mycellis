package com.mycelis.alert.repository;

import com.mycelis.alert.entity.Alert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AlertRepository extends JpaRepository<Alert, UUID> {

    /** The most recent alert for a stalk — the suppression/state-machine source of truth. */
    Optional<Alert> findTopByStalkIdOrderByFiredAtDesc(UUID stalkId);
}
