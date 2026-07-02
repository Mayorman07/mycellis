package com.mycelis.monitoring.engine;

import com.mycelis.shared.config.MonitoringProperties;
import com.mycelis.monitoring.entity.Stalk;
import com.mycelis.monitoring.event.PulseCheckedEvent;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
    private final ApplicationEventPublisher eventPublisher;

    private static final int MAX_CLIENT_CACHE_SIZE = 50;

    private final Counter cycleSuccessCounter;
    private final Counter cycleFailureCounter;

    /** LRU-bounded cache of RestClients keyed by their connect+read timeout value (seconds). */
    private final Map<Integer, RestClient> clientCache = Collections.synchronizedMap(
            new LinkedHashMap<Integer, RestClient>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Integer, RestClient> eldest) {
                    return size() > MAX_CLIENT_CACHE_SIZE;
                }
            }
    );

    public PulseEngine(MonitoringProperties monitoringProperties,
                       MeterRegistry meterRegistry,
                       ApplicationEventPublisher eventPublisher) {
        this.monitoringProperties = monitoringProperties;
        this.meterRegistry = meterRegistry;
        this.eventPublisher = eventPublisher;

        this.cycleSuccessCounter = Counter.builder("app.engine.cycle.success")
                .description("Number of successful health checks per cycle")
                .register(meterRegistry);

        this.cycleFailureCounter = Counter.builder("app.engine.cycle.failure")
                .description("Number of failed health checks per cycle")
                .register(meterRegistry);

        Gauge.builder("app.engine.client_cache.size", clientCache, Map::size)
                .description("Number of cached RestClient instances (LRU-bounded)")
                .register(meterRegistry);
    }

    /**
     * Returns a cached RestClient configured for the given timeout, or builds and caches one.
     *
     * <p>The {@code timeoutSeconds} parameter is applied to BOTH the TCP connect step
     * (HttpClient.connectTimeout) and the response-read step (factory.setReadTimeout).
     * Without a read timeout, a server that accepts the connection but never responds
     * would hold a virtual thread indefinitely (slow-loris pattern).</p>
     *
     * @param timeoutSeconds the maximum duration for connect and read, in seconds
     * @return cached or freshly built RestClient
     */
    private RestClient getClientForTimeout(int timeoutSeconds) {
        return clientCache.computeIfAbsent(timeoutSeconds, this::buildClient);
    }

    private RestClient buildClient(int timeoutSeconds) {
        Duration timeout = Duration.ofSeconds(timeoutSeconds);

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(timeout);   // critical — prevents slow-loris hangs

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

            publishPulseCheckedEvent(stalk, statusCode.value(), latencyMs, isSuccess, null);

            log.debug("Check succeeded: stalkId={}, status={}, latency={}ms",
                    stalk.getId(), statusCode.value(), latencyMs);

            sample.stop(Timer.builder("app.engine.check.duration")
                    .tag("result", "success")
                    .tag("error_type", "none")
                    .tag("url_hash", hashUrl(url))
                    .register(meterRegistry));

        } catch (RestClientResponseException e) {
            long latencyMs = Duration.between(requestStart, Instant.now()).toMillis();
            int statusCode = e.getStatusCode().value();
            boolean isSuccess = statusCode >= 200 && statusCode < 400;

            publishPulseCheckedEvent(stalk, statusCode, latencyMs, isSuccess, e.getMessage());

            log.debug("Check returned error status: stalkId={}, status={}, latency={}ms, error={}",
                    stalk.getId(), statusCode, latencyMs, e.getMessage());

            sample.stop(Timer.builder("app.engine.check.duration")
                    .tag("result", "http_error")
                    .tag("error_type", "http_status_" + statusCodeBucket(statusCode))
                    .tag("url_hash", hashUrl(url))
                    .register(meterRegistry));

        } catch (ResourceAccessException e) {
            long latencyMs = Duration.between(requestStart, Instant.now()).toMillis();
            Throwable rootCause = e.getCause() != null ? e.getCause() : e;
            String errorMessage = classifyException(rootCause, timeoutSeconds);

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

            publishPulseCheckedEvent(stalk, 0, latencyMs, false, errorMessage);

            log.error("Unexpected error: stalkId={}, latency={}ms, error={}",
                    stalk.getId(), latencyMs, errorMessage, e);

            sample.stop(Timer.builder("app.engine.check.duration")
                    .tag("result", "unexpected_error")
                    .tag("error_type", "other")
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
                .organizationId(stalk.getOrganizationId())
                .statusCode(statusCode)
                .latencyMs(latencyMs)
                .isSuccess(isSuccess)
                .errorMessage(truncateErrorMessage(errorMessage))
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
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(url.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                sb.append(String.format("%02x", digest[i]));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("MD5 must be available in every JDK", e);
        }
    }

    /**
     * Maps an HTTP status code to a low-cardinality bucket for metric tags.
     * Returns "2xx", "3xx", "4xx", "5xx", or "other".
     */
    private String statusCodeBucket(int statusCode) {
        if (statusCode >= 200 && statusCode < 300) return "2xx";
        if (statusCode >= 300 && statusCode < 400) return "3xx";
        if (statusCode >= 400 && statusCode < 500) return "4xx";
        if (statusCode >= 500 && statusCode < 600) return "5xx";
        return "other";
    }

    /**
     * Truncates an error message to maxErrorMessageLength to honor the storage contract.
     * Returns null unchanged. Adds "..." suffix when truncation occurs.
     */
    private String truncateErrorMessage(String message) {
        if (message == null) return null;
        int maxLength = monitoringProperties.getMaxErrorMessageLength();
        return message.length() <= maxLength ? message : message.substring(0, maxLength) + "...";
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
                        (message != null ? message : "Error currently unknown");
            }
        };
    }
}