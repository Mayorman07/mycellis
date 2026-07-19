package com.mycelis.status.service;

import com.mycelis.monitoring.constant.LatencyState;
import com.mycelis.monitoring.constant.ReliabilityState;
import com.mycelis.monitoring.entity.Stalk;
import com.mycelis.monitoring.repository.PulseRepository;
import com.mycelis.monitoring.repository.StalkRepository;
import com.mycelis.organization.entity.Organization;
import com.mycelis.organization.repository.OrganizationRepository;
import com.mycelis.shared.exception.ResourceNotFoundException;
import com.mycelis.status.dto.PublicStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StatusServiceImpl implements StatusService {

    private static final int UPTIME_WINDOW_DAYS = 90;

    private final OrganizationRepository organizationRepository;
    private final StalkRepository stalkRepository;
    private final PulseRepository pulseRepository;

    @Override
    @Transactional(readOnly = true)
    public PublicStatusResponse getPublicStatus(String slug) {
        Organization organization = organizationRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Organization", slug));

        List<Stalk> activeStalks = stalkRepository
                .findByOrganizationId(organization.getId(), Pageable.unpaged())
                .getContent()
                .stream()
                .filter(Stalk::getIsActive)
                .toList();

        List<PublicStatusResponse.StalkStatus> stalkStatuses = activeStalks.stream()
                .map(this::toStalkStatus)
                .toList();

        return new PublicStatusResponse(
                new PublicStatusResponse.OrganizationSummary(organization.getName(), organization.getSlug()),
                computeOverallState(activeStalks),
                stalkStatuses,
                Instant.now()
        );
    }

    /**
     * Severity ordering: IMPAIRED > STRESSED > DEGRADED > HEALTHY.
     *
     * <p>Deliberately keyed off the current two-axis fields (reliabilityState,
     * latencyState), not the deprecated single-axis {@code currentState}
     * ({@link com.mycelis.monitoring.constant.StalkState}) — that enum has no
     * DOWN value at all (only HEALTHY/STRESSED/DEGRADED/DORMANT), so a
     * "currentState == DOWN" check could never match. reliabilityState DOWN
     * and latencyState STRESSED are the fields that actually carry those two
     * concepts today.</p>
     */
    private String computeOverallState(List<Stalk> stalks) {
        boolean anyDown = stalks.stream()
                .anyMatch(stalk -> stalk.getReliabilityState() == ReliabilityState.DOWN);
        if (anyDown) {
            return "IMPAIRED";
        }

        boolean anyStressed = stalks.stream()
                .anyMatch(stalk -> stalk.getLatencyState() == LatencyState.STRESSED);
        if (anyStressed) {
            return "STRESSED";
        }

        boolean anyDegraded = stalks.stream()
                .anyMatch(stalk -> stalk.getReliabilityState() == ReliabilityState.DEGRADED);
        if (anyDegraded) {
            return "DEGRADED";
        }

        return "HEALTHY";
    }

    private PublicStatusResponse.StalkStatus toStalkStatus(Stalk stalk) {
        return new PublicStatusResponse.StalkStatus(
                stalk.getNickname(),
                stalk.getReliabilityState().name(),
                stalk.getLatencyState().name(),
                stalk.getHealthIndex(),
                stalk.getAverageLatencyMs(),
                computeUptimeHistory(stalk.getId())
        );
    }

    /**
     * Builds a 90-element daily-uptime array: index 0 = 89 days ago, index 89
     * = today. Missing days (no pulses at all that day) are null, not 0 —
     * "no data" and "0% uptime" are different facts.
     *
     * <p>windowStart is the UTC midnight that starts the 89-days-ago calendar
     * day (not just "now minus 90*24h"), so the SQL aggregation and the
     * index-to-day mapping below cover exactly the same 90 calendar days.</p>
     */
    private List<Double> computeUptimeHistory(UUID stalkId) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate startDay = today.minusDays(UPTIME_WINDOW_DAYS - 1L);
        Instant windowStart = startDay.atStartOfDay(ZoneOffset.UTC).toInstant();

        List<Object[]> rows = pulseRepository.findDailyUptimeAggregates(stalkId, windowStart);

        Map<LocalDate, Double> percentByDay = new HashMap<>();
        for (Object[] row : rows) {
            LocalDate day = toLocalDate(row[0]);
            long total = ((Number) row[1]).longValue();
            long successful = ((Number) row[2]).longValue();
            percentByDay.put(day, total == 0 ? null : (successful * 100.0) / total);
        }

        List<Double> history = new ArrayList<>(UPTIME_WINDOW_DAYS);
        for (int i = 0; i < UPTIME_WINDOW_DAYS; i++) {
            history.add(percentByDay.get(startDay.plusDays(i)));
        }
        return history;
    }

    // Exact JDBC type for a native DATE(...) column can vary by driver/version
    // (java.sql.Date is typical, some coerce straight to LocalDate) — handle
    // both rather than assume one.
    private LocalDate toLocalDate(Object value) {
        return switch (value) {
            case java.sql.Date date -> date.toLocalDate();
            case LocalDate localDate -> localDate;
            default -> throw new IllegalStateException("Unexpected day value type: " + value.getClass());
        };
    }
}
