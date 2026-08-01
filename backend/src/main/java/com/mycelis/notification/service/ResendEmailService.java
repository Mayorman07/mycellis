package com.mycelis.notification.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * Resend REST API implementation, for production. No profile restriction —
 * selected purely by mycelis.email.provider so it can be exercised from any
 * profile (e.g. testing locally via an env var override without switching
 * to the prod profile).
 *
 * <p>Builds its own RestClient rather than sharing one — PulseEngine's
 * RestClient usage elsewhere in the codebase is purpose-built for
 * arbitrary-target health-check polling (per-timeout caching, generic
 * User-Agent) and isn't a fit for a fixed-target, authenticated JSON API
 * call; there's no existing general-purpose RestClient bean to reuse.</p>
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "mycelis.email.provider", havingValue = "resend")
public class ResendEmailService implements EmailService {

    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    private final RestClient restClient = RestClient.create();

    @Value("${mycelis.email.from:}")
    private String fromAddress;

    @Value("${mycelis.email.resend.api-key:}")
    private String apiKey;

    /**
     * Fails startup rather than boot with an email path that will silently
     * misroute every message in production — same reasoning as the
     * Commit 20.5 seeder guards, except this fails hard instead of skipping,
     * since there's no reasonable "skip sending email in prod" degradation.
     */
    @PostConstruct
    void validateConfiguration() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "mycelis.email.provider=resend requires RESEND_API_KEY to be set, but it is blank.");
        }
        if (fromAddress == null || fromAddress.isBlank()) {
            throw new IllegalStateException(
                    "mycelis.email.provider=resend requires MYCELIS_EMAIL_FROM to be set, but it is blank.");
        }
    }

    @Override
    @Async("emailTaskExecutor")
    public void send(String to, String subject, String htmlBody) {
        Map<String, Object> payload = Map.of(
                "from", fromAddress,
                "to", List.of(to),
                "subject", subject,
                "html", htmlBody
        );

        try {
            restClient.post()
                    .uri(RESEND_API_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Email sent to {} | subject='{}'", to, subject);
        } catch (RestClientResponseException e) {
            log.error("Resend API error: status={}, body={}, to={}, subject='{}'",
                    e.getStatusCode(), e.getResponseBodyAsString(), to, subject);
            throw e;
        } catch (RestClientException e) {
            log.error("Resend API call failed: to={}, subject='{}'", to, subject, e);
            throw e;
        }
    }
}
