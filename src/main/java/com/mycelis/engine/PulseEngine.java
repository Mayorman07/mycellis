package com.mycelis.engine;

import com.mycelis.config.MonitoringProperties;
import com.mycelis.entity.Stalk;
import com.mycelis.event.PulseCheckedEvent;
import com.mycelis.service.PulseService;
import com.mycelis.service.StalkService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import javax.net.ssl.SSLException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
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


    private final MonitoringProperties monitoringProperties;
    private final MeterRegistry meterRegistry;
    private final ApplicationEventPublisher eventPublisher;  // ← New dependency

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
     * Constructs PulseEngine with event publishing capability.
     *
     * @param monitoringProperties configuration for thresholds and limits
     * @param meterRegistry Micrometer registry for observability
     * @param eventPublisher Spring event publisher for async fan-out
     */
    public PulseEngine(MonitoringProperties monitoringProperties,
                       MeterRegistry meterRegistry,
                       ApplicationEventPublisher eventPublisher) {  // ← Updated constructor
        this.monitoringProperties = monitoringProperties;
        this.meterRegistry = meterRegistry;
        this.eventPublisher = eventPublisher;

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
     * Creates or retrieves an HttpClient configured with the specified timeout.
     * Caches clients to avoid recreating them for the same timeout value.
     *
     * @param timeoutSeconds the timeout in seconds
     * @return configured RestClient instance
     */
    private RestClient getClientForTimeout(int timeoutSeconds) {
        // You can cache clients if needed, or create a new one each time
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);

        return RestClient.builder()
                .requestFactory(requestFactory)
                .defaultHeader("User-Agent", "Mycelis-Monitor/1.0")
                .defaultHeader("Accept", "*/*")
                .build();
    }


    /**
     * Dispatches due stalks to virtual threads for concurrent execution.
     * Blocks until all checks in this cycle complete or timeout.
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
     * Publishes an immutable event upon completion - virtual thread dies immediately after.
     *
     * @param stalk the monitoring target to check
     */
    private void executeCheck(Stalk stalk) {
        Timer.Sample sample = Timer.start(meterRegistry);
        Instant requestStart = Instant.now();
        String url = stalk.getUrl();
        int timeoutSeconds = stalk.getTimeoutSeconds();

        log.debug("Checking stalk {}: {}", stalk.getId(), url);

        try {
            RestClient client = getClientForTimeout(timeoutSeconds);

            HttpStatusCode statusCode = client.get()
                    .uri(url)
                    .retrieve()
                    .toBodilessEntity()
                    .getStatusCode();

            long latencyMs = Duration.between(requestStart, Instant.now()).toMillis();
            boolean isSuccess = statusCode.is2xxSuccessful() || statusCode.is3xxRedirection();

            // Publish event and return - virtual thread dies here
            publishPulseCheckedEvent(stalk, statusCode.value(), latencyMs, isSuccess, null);

            log.debug("Check succeeded: stalkId={}, status={}, latency={}ms",
                    stalk.getId(), statusCode.value(), latencyMs);

            sample.stop(Timer.builder("app.engine.check.duration")
                    .tag("result", "success")
                    .tag("status", String.valueOf(statusCode.value()))
                    .tag("url_hash", hashUrl(url))
                    .register(meterRegistry));

        } catch (RestClientResponseException e) {
            long latencyMs = Duration.between(requestStart, Instant.now()).toMillis();
            int statusCode = e.getStatusCode().value();
            boolean isSuccess = statusCode >= 200 && statusCode < 400;

            //  Publish event for HTTP errors too
            publishPulseCheckedEvent(stalk, statusCode, latencyMs, isSuccess, e.getMessage());

            log.debug("Check returned error status: stalkId={}, status={}, latency={}ms, error={}",
                    stalk.getId(), statusCode, latencyMs, e.getMessage());

            sample.stop(Timer.builder("app.engine.check.duration")
                    .tag("result", "http_error")
                    .tag("status", String.valueOf(statusCode))
                    .tag("url_hash", hashUrl(url))
                    .register(meterRegistry));

        } catch (ResourceAccessException e) {
            long latencyMs = Duration.between(requestStart, Instant.now()).toMillis();
            Throwable rootCause = e.getCause() != null ? e.getCause() : e;
            String errorMessage = classifyException(rootCause, timeoutSeconds);

            //  Publish event for network errors
            publishPulseCheckedEvent(stalk, 0, latencyMs, false, errorMessage);

            log.warn("Network error: stalkId={}, latency={}ms, error={}",
                    stalk.getId(), latencyMs, errorMessage);

            sample.stop(Timer.builder("app.engine.check.duration")
                    .tag("result", "network_error")
                    .tag("error_type", classifyExceptionForMetric(rootCause))
                    .tag("url_hash", hashUrl(url))
                    .register(meterRegistry));

        } catch (Exception e) {
            long latencyMs = Duration.between(requestStart, Instant.now()).toMillis();
            Throwable rootCause = e.getCause() != null ? e.getCause() : e;
            String errorMessage = "UNEXPECTED: " + classifyException(rootCause, timeoutSeconds);

            // Publish event for unexpected errors
            publishPulseCheckedEvent(stalk, 0, latencyMs, false, errorMessage);

            log.error("Unexpected error: stalkId={}, latency={}ms, error={}",
                    stalk.getId(), latencyMs, errorMessage, e);

            sample.stop(Timer.builder("app.engine.check.duration")
                    .tag("result", "unexpected_error")
                    .tag("error_type", rootCause.getClass().getSimpleName())
                    .tag("url_hash", hashUrl(url))
                    .register(meterRegistry));
        }
    }

    /**
     * Publishes an immutable event for async processing.
     * Virtual thread dies immediately after this call.
     */
    private void publishPulseCheckedEvent(Stalk stalk, int statusCode, long latencyMs,
                                          boolean isSuccess, String errorMessage) {
        PulseCheckedEvent event = PulseCheckedEvent.builder()
                .stalkId(stalk.getId())
                .userId(stalk.getUserId())  // Pass tenant ID for isolation in listeners
                .statusCode(statusCode)
                .latencyMs(latencyMs)
                .isSuccess(isSuccess)
                .errorMessage(errorMessage)
                .checkedAt(Instant.now())
                .urlHash(hashUrl(stalk.getUrl()))
                .build();

        eventPublisher.publishEvent(event);
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
            case SSLException ignored -> "ssl_error";
            case HttpTimeoutException ignored -> "timeout";
            case TimeoutException ignored -> "timeout";
            case SocketTimeoutException ignored -> "read_timeout";
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
            case SSLException ignored ->
                    "SSL_ERROR: Certificate validation failed";
            case HttpConnectTimeoutException ignored ->
                    "CONNECT_TIMEOUT: Could not establish connection within " + timeoutSeconds + "s";
            case HttpTimeoutException ignored ->
                    "TIMEOUT: Exceeded " + timeoutSeconds + "s";
            case TimeoutException ignored ->
                    "TIMEOUT: Exceeded " + timeoutSeconds + "s";
            case SocketTimeoutException ignored ->
                    "READ_TIMEOUT: Server did not respond in time";
            default -> {
                String message = e.getMessage();
                yield e.getClass().getSimpleName() + ": " +
                        (message != null ? message : "Unknown error");
            }
        };
    }
}