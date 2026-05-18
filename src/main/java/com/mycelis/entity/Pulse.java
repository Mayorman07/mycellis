package com.mycelis.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable diagnostic record representing a single health check execution.
 * Functions as a time-series data point for trend analysis, alerting,
 * and sliding-window metric aggregation.
 *
 * <p>Mapped to the partitioned {@code pulses} table. Records are append-only
 * and never updated post-commit to guarantee audit integrity and simplify
 * retention/archival strategies.</p>
 */
@Entity
@Table(name = "pulses")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pulse {

    /** Unique identifier for the diagnostic record */
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Parent monitoring target (foreign key to stalks.id) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stalk_id", nullable = false, updatable = false)
    private Stalk stalk;

    /** HTTP response status code returned by the target endpoint */
    @Column(name = "status_code", nullable = false)
    private Integer statusCode;

    /** Round-trip network latency in milliseconds */
    @Column(name = "latency_ms", nullable = false)
    private Long latencyMs;

    /** Boolean flag indicating success (2xx/3xx range) or failure */
    @Column(name = "is_success", nullable = false)
    private Boolean isSuccess;

    /** Human-readable error description or exception message (if applicable) */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /** Payload size of the HTTP response in bytes */
    @Column(name = "response_size_bytes")
    private Long responseSizeBytes;

    /** Immutable execution timestamp (UTC) */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
