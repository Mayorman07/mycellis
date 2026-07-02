package com.mycelis.monitoring.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * Ensures monthly partitions of the {@code pulses} table exist ahead of time.
 * <p>
 * The pulses table is partitioned by {@code created_at} with monthly range partitions.
 * If no partition exists for a given month, INSERTs fail with a partition-key error.
 * This service prevents that by pre-creating partitions for the current month and
 * the next few months, both on application startup and on a monthly schedule.
 * <p>
 * All operations are idempotent — {@code CREATE TABLE IF NOT EXISTS} means calling
 * multiple times is safe. Concurrent invocations across restarts are also safe.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PulsePartitionMaintenanceService {

    /** How many months ahead of the current month to keep pre-created. */
    private static final int MONTHS_AHEAD = 3;

    private static final DateTimeFormatter PARTITION_MONTH_FMT = DateTimeFormatter.ofPattern("yyyy_MM");
    private static final DateTimeFormatter BOUND_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final JdbcTemplate jdbcTemplate;

    /**
     * Runs once at app startup, right after Spring finishes wiring beans.
     * Catches "we've been down for weeks and are missing recent partitions" cases.
     */
    @PostConstruct
    public void ensurePartitionsOnStartup() {
        log.info("Startup partition check: ensuring current + next {} months exist", MONTHS_AHEAD);
        ensureUpcomingPartitions();
    }

    /**
     * Runs on the 15th of every month at 02:00 UTC.
     * Ensures the next {@link #MONTHS_AHEAD} months of partitions exist so we
     * always have runway.
     */
    @Scheduled(cron = "0 0 2 15 * *", zone = "UTC")
    public void ensurePartitionsMonthly() {
        log.info("Monthly partition check triggered");
        ensureUpcomingPartitions();
    }

    /**
     * Ensures partitions exist for the current month and the next
     * {@link #MONTHS_AHEAD} months. Idempotent — existing partitions are skipped
     * by Postgres via IF NOT EXISTS.
     */
    @Transactional
    protected void ensureUpcomingPartitions() {
        YearMonth start = YearMonth.now();
        for (int offset = 0; offset <= MONTHS_AHEAD; offset++) {
            YearMonth ym = start.plusMonths(offset);
            createPartitionIfMissing(ym);
        }
    }

    private void createPartitionIfMissing(YearMonth ym) {
        String partitionName = "pulses_" + ym.format(PARTITION_MONTH_FMT);
        LocalDate fromDate = ym.atDay(1);
        LocalDate toDate = ym.plusMonths(1).atDay(1);

        String sql = """
            CREATE TABLE IF NOT EXISTS %s
            PARTITION OF pulses
            FOR VALUES FROM ('%s') TO ('%s')
            """.formatted(
                partitionName,
                fromDate.format(BOUND_DATE_FMT),
                toDate.format(BOUND_DATE_FMT)
        );

        try {
            jdbcTemplate.execute(sql);
            log.info("Ensured partition {} covers [{}, {})", partitionName, fromDate, toDate);
        } catch (Exception e) {
            // Don't let a partition-creation failure kill startup — log and continue.
            // The next scheduled run (or restart) will retry.
            log.error("Failed to ensure partition {}: {}", partitionName, e.getMessage(), e);
        }
    }
}