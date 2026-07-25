package com.mycelis.alert.entity;

import com.mycelis.alert.constant.AlertType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * A fired DOWN or RECOVERY alert for one stalk. Also the suppression source
 * of truth: AlertEngine checks whether the most recent row for a stalk is a
 * DOWN with no paired RECOVERY yet before firing another DOWN.
 */
@Entity
@Table(name = "alerts", indexes = {
        @Index(name = "idx_alerts_stalk_id_fired_at", columnList = "stalk_id, fired_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alert {

    @Id
    @Column(nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "stalk_id", nullable = false)
    private UUID stalkId;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 20)
    private AlertType alertType;

    @Column(name = "fired_at", nullable = false)
    private Instant firedAt;

    /** Null if alertsEnabled was false for the recipient at fire time — the row still exists for suppression. */
    @Column(name = "delivered_at")
    private Instant deliveredAt;

    /** RECOVERY only: seconds between the paired DOWN alert's firedAt and this row's firedAt. */
    @Column(name = "downtime_seconds")
    private Integer downtimeSeconds;
}
