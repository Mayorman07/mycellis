package com.mycelis.engine;

import com.mycelis.config.MonitoringProperties;
import com.mycelis.entity.Stalk;
import com.mycelis.service.PulseService;
import com.mycelis.service.StalkService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * High-concurrency health check executor leveraging Java 21 Virtual Threads.
 * Uses RestClient (synchronous) to align with Virtual Thread blocking semantics.
 *
 * <p>Design principles:
 * <ul>
 *   <li>One virtual thread per check → massive concurrency with minimal memory overhead</li>
 *   <li>RestClient + blocking I/O → JVM unmounts virtual threads during network waits</li>
 *   <li>Per-stalk timeout caching → dynamic timeouts without rebuilding factories</li>
 *   <li>Root-cause exception classification → accurate error reporting despite RestClient wrapping</li>
 *   <li>Graceful degradation → timeouts and errors recorded as pulses, not crashes</li>
 *   <li>Observability via Micrometer → SLO tracking, anomaly detection, and production debugging</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
public class PulseEngine {

    private final PulseService pulseService;
    private final StalkService stalkService;
    private final MonitoringProperties monitoringProperties;
    private final MeterRegistry meterRegistry;

    private static final int MAX_CLIENT_CACHE_SIZE = 50;

    // Counters for cycle-level metrics
    private final Counter cycleSuccessCounter;
    private final Counter cycleFailureCounter;

    // Cache of RestClient instances keyed by timeout value (LRU-bounded)
    private final Map<Integer, RestClient> clientCache = Collections.synchronizedMap(
            new LinkedHashMap<Integer, RestClient>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Integer, RestClient> eldest) {
                    return size() > MAX_CLIENT_CACHE_SIZE;
                }
            }
    );

    /**
     * Constructs PulseEngine with production-grade RestClient configuration and metrics.
     *
     * @param pulseService service for recording diagnostic pulses
     * @param stalkService service for updating stalk state and metrics
     * @param monitoringProperties configuration for thresholds and limits
     * @param meterRegistry Micrometer registry for observability
     */
    public PulseEngine(PulseService pulseService,
                       StalkService stalkService,
                       MonitoringProperties monitoringProperties,
                       MeterRegistry meterRegistry) {
        this.pulseService = pulseService;
        this.stalkService = stalkService;
        this.monitoringProperties = monitoringProperties;
        this.meterRegistry = meterRegistry;

        // Register counters for cycle metrics
        this.cycleSuccessCounter = Counter.builder("app.engine.cycle.success")
                .description("Number of successful health checks per cycle")
                .register(meterRegistry);

        this.cycleFailureCounter = Counter.builder("app.engine.cycle.failure")
                .description("Number of failed health checks per cycle")
                .register(meterRegistry);

        // Register gauge for cache size monitoring
        Gauge.builder("app.engine.client_cache.size", clientCache, Map::size)
                .description("Number of cached RestClient instances (LRU-bounded)")
                .register(meterRegistry);
    }

    /**
     * Returns a RestClient instance configured with the specified timeout.
     * Caches instances to avoid redundant factory creation.
     *
     * @param timeoutSeconds timeout value in seconds
     * @return configured RestClient instance
     */
    private RestClient getClientForTimeout(int timeoutSeconds) {
        return clientCache.computeIfAbsent(timeoutSeconds, t -> {
            // Create HttpClient with custom timeouts
            java.net.http.HttpClient httpClient = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(t))
                    .build();

            // Create request factory with the custom HttpClient
            JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);

            return RestClient.builder()
                    .requestFactory(factory)
                    .defaultHeader("User-Agent", "Mycelis-Monitor/1.0 (Uptime Monitoring)")
                    .defaultHeader("Accept", "*/*")
                    .build();
        });
    }

    /**
     * Dispatches due stalks to virtual threads for concurrent execution.
     * Blocks until all checks in this cycle complete or timeout.
     *
     * @param dueStalks list of stalks scheduled for immediate health checks
     */
    public void executeCycle(List<Stalk> dueStalks) {
        if (dueStalks.isEmpty()) {
            log.debug("No stalks due for checking");
            return;
        }

        log.info("Starting check cycle for {} stalks", dueStalks.size());
        Instant cycleStart = Instant.now();
        AtomicInteger successfulChecks = new AtomicInteger(0);
        AtomicInteger failedChecks = new AtomicInteger(0);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            dueStalks.forEach(stalk -> executor.submit(() -> {
                try {
                    executeCheck(stalk);
                    successfulChecks.incrementAndGet();
                } catch (Exception e) {
                    log.error("Unhandled exception in check for stalk {}: {}",
                            stalk.getId(), e.getMessage(), e);
                    failedChecks.incrementAndGet();
                }
            }));

            executor.shutdown();
            boolean terminated = executor.awaitTermination(
                    monitoringProperties.getMaxCycleDuration().getSeconds(), TimeUnit.SECONDS);

            if (!terminated) {
                log.warn("Check cycle did not complete within {} seconds. Forcing shutdown.",
                        monitoringProperties.getMaxCycleDuration().getSeconds());
                executor.shutdownNow();
            }

        } catch (InterruptedException e) {
            log.error("Check cycle interrupted", e);
            Thread.currentThread().interrupt();
        }

        // Record cycle-level metrics
        cycleSuccessCounter.increment(successfulChecks.get());
        cycleFailureCounter.increment(failedChecks.get());

        Duration cycleDuration = Duration.between(cycleStart, Instant.now());
        log.info("Check cycle completed: duration={}ms, successful={}, failed={}, total={}",
                cycleDuration.toMillis(),
                successfulChecks.get(),
                failedChecks.get(),
                dueStalks.size());
    }

    /**
     * Executes a single HTTP health check inside a virtual thread.
     * Uses RestClient for clean blocking I/O that cooperates with Virtual Thread unmounting.
     *
     * @param stalk the monitoring target to check
     */
    private void executeCheck(Stalk stalk) {
        // Start timer for per-check duration tracking
        Timer.Sample sample = Timer.start(meterRegistry);

        Instant requestStart = Instant.now();
        String url = stalk.getUrl();
        int timeoutSeconds = stalk.getTimeoutSeconds();

        log.debug("Checking stalk {}: {}", stalk.getId(), url);

        try {
            // Get timeout-specific RestClient instance
            RestClient client = getClientForTimeout(timeoutSeconds);

            // Execute synchronous HTTP GET with configured timeout
            HttpStatusCode statusCode = client.get()
                    .uri(url)
                    .retrieve()
                    .toBodilessEntity()
                    .getStatusCode();

            long latencyMs = Duration.between(requestStart, Instant.now()).toMillis();
            boolean isSuccess = statusCode.is2xxSuccessful() || statusCode.is3xxRedirection();

            pulseService.recordCheckResult(stalk.getId(), statusCode.value(), latencyMs, isSuccess, null);
            stalkService.updateMetricsAndTransitionState(stalk.getId(), Instant.now());

            log.debug("Check succeeded: stalkId={}, status={}, latency={}ms",
                    stalk.getId(), statusCode.value(), latencyMs);

            // Record success metric with status tag
            sample.stop(Timer.builder("app.engine.check.duration")
                    .description("Duration of a single health check")
                    .tag("result", "success")
                    .tag("status", String.valueOf(statusCode.value()))
                    .tag("url_hash", hashUrl(url)) // Avoid high-cardinality URL tag
                    .register(meterRegistry));

        } catch (RestClientResponseException e) {
            // HTTP 4xx/5xx responses are valid responses, not exceptions
            long latencyMs = Duration.between(requestStart, Instant.now()).toMillis();
            int statusCode = e.getStatusCode() != null ? e.getStatusCode().value() : 0;
            boolean isSuccess = statusCode >= 200 && statusCode < 400;

            pulseService.recordCheckResult(stalk.getId(), statusCode, latencyMs, isSuccess, e.getMessage());
            stalkService.updateMetricsAndTransitionState(stalk.getId(), Instant.now());

            log.debug("Check returned error status: stalkId={}, status={}, latency={}ms, error={}",
                    stalk.getId(), statusCode, latencyMs, e.getMessage());

            // Record HTTP error metric
            sample.stop(Timer.builder("app.engine.check.duration")
                    .description("Duration of a single health check")
                    .tag("result", "http_error")
                    .tag("status", String.valueOf(statusCode))
                    .tag("url_hash", hashUrl(url))
                    .register(meterRegistry));

        } catch (ResourceAccessException e) {
            // Network-level failures (timeouts, connection refused, DNS, SSL)
            long latencyMs = Duration.between(requestStart, Instant.now()).toMillis();

            // Extract root cause before classification
            Throwable rootCause = e.getCause() != null ? e.getCause() : e;
            String errorMessage = classifyException(rootCause, timeoutSeconds);

            pulseService.recordCheckResult(stalk.getId(), 0, latencyMs, false, errorMessage);
            stalkService.updateMetricsAndTransitionState(stalk.getId(), Instant.now());

            log.warn("Network error: stalkId={}, latency={}ms, error={}",
                    stalk.getId(), latencyMs, errorMessage);

            // Record network error metric
            sample.stop(Timer.builder("app.engine.check.duration")
                    .description("Duration of a single health check")
                    .tag("result", "network_error")
                    .tag("error_type", classifyExceptionForMetric(rootCause))
                    .tag("url_hash", hashUrl(url))
                    .register(meterRegistry));

        } catch (Exception e) {
            // Fallback for any other unexpected errors
            long latencyMs = Duration.between(requestStart, Instant.now()).toMillis();
            Throwable rootCause = e.getCause() != null ? e.getCause() : e;
            String errorMessage = "UNEXPECTED: " + classifyException(rootCause, timeoutSeconds);

            pulseService.recordCheckResult(stalk.getId(), 0, latencyMs, false, errorMessage);
            stalkService.updateMetricsAndTransitionState(stalk.getId(), Instant.now());

            log.error("Unexpected error: stalkId={}, latency={}ms, error={}",
                    stalk.getId(), latencyMs, errorMessage, e);

            // Record unexpected error metric
            sample.stop(Timer.builder("app.engine.check.duration")
                    .description("Duration of a single health check")
                    .tag("result", "unexpected_error")
                    .tag("error_type", rootCause.getClass().getSimpleName())
                    .tag("url_hash", hashUrl(url))
                    .register(meterRegistry));
        }
    }

    /**
     * Creates a low-cardinality hash of the URL for metrics tagging.
     * Avoids high-cardinality explosion from unique URLs.
     */
    private String hashUrl(String url) {
        // Simple hash: first 8 chars of MD5 (good enough for grouping)
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(url.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                sb.append(String.format("%02x", digest[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Classifies exception for metric tagging (low-cardinality).
     */
    private String classifyExceptionForMetric(Throwable e) {
        return switch (e) {
            case ConnectException ignored -> "connection_refused";
            case UnknownHostException ignored -> "dns_error";
            case javax.net.ssl.SSLException ignored -> "ssl_error";
            case HttpTimeoutException ignored -> "timeout";
            case TimeoutException ignored -> "timeout";
            case java.net.SocketTimeoutException ignored -> "read_timeout";
            default -> "other";
        };
    }

    /**
     * Classifies exception types using pattern matching on Throwable for clearer error reporting.
     * Handles both direct exceptions and wrapped root causes from RestClient.
     */
    private String classifyException(Throwable e, int timeoutSeconds) {
        return switch (e) {
            case ConnectException ignored ->
                    "CONNECTION_REFUSED: Target unreachable";
            case UnknownHostException ignored ->
                    "DNS_ERROR: Hostname resolution failed";
            case javax.net.ssl.SSLException ignored ->
                    "SSL_ERROR: Certificate validation failed";
            case java.net.http.HttpConnectTimeoutException ignored ->
                    "CONNECT_TIMEOUT: Could not establish connection within " + timeoutSeconds + "s";
            case HttpTimeoutException ignored ->
                    "TIMEOUT: Exceeded " + timeoutSeconds + "s";
            case TimeoutException ignored ->
                    "TIMEOUT: Exceeded " + timeoutSeconds + "s";
            case java.net.SocketTimeoutException ignored ->
                    "READ_TIMEOUT: Server did not respond in time";
            default -> {
                String message = e.getMessage();
                yield e.getClass().getSimpleName() + ": " +
                        (message != null ? message : "Unknown error");
            }
        };
    }
}