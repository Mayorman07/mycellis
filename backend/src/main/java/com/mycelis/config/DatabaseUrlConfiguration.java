package com.mycelis.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * Fly Postgres injects DATABASE_URL as {@code postgres://user:pass@host:port/db},
 * not the {@code jdbc:postgresql://...} form Spring's datasource auto-config
 * expects, and doesn't split out separate username/password secrets. This
 * runs before that auto-config sees any properties, translating Fly's shape
 * into spring.datasource.url/username/password if present.
 *
 * <p>Registered via
 * META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor.imports
 * rather than a {@code @Bean} — EnvironmentPostProcessors run before the
 * ApplicationContext exists, so this can't be a managed Spring bean.</p>
 */
public class DatabaseUrlConfiguration implements EnvironmentPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(DatabaseUrlConfiguration.class);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String databaseUrl = environment.getProperty("DATABASE_URL");

        // Pass through unchanged if not Fly's postgres:// format — covers both
        // "not set" (dev, which uses application.properties's own
        // spring.datasource.url) and "already jdbc:" (nothing to translate).
        if (databaseUrl == null || !(databaseUrl.startsWith("postgres://") || databaseUrl.startsWith("postgresql://"))) {
            return;
        }

        try {
            URI uri = new URI(databaseUrl);
            String userInfo = uri.getUserInfo();
            if (userInfo == null || !userInfo.contains(":")) {
                log.warn("DATABASE_URL is postgres:// but has no user:pass — leaving spring.datasource.* untouched");
                return;
            }

            String[] credentials = userInfo.split(":", 2);
            String username = credentials[0];
            String password = credentials[1];

            String jdbcUrl = String.format("jdbc:postgresql://%s:%d%s%s",
                    uri.getHost(),
                    uri.getPort() == -1 ? 5432 : uri.getPort(),
                    uri.getPath(),
                    uri.getQuery() != null ? "?" + uri.getQuery() : ""
            );

            Map<String, Object> props = new HashMap<>();
            props.put("spring.datasource.url", jdbcUrl);
            props.put("spring.datasource.username", username);
            props.put("spring.datasource.password", password);

            environment.getPropertySources().addFirst(
                    new MapPropertySource("flyDatabaseUrl", props)
            );

            log.info("Translated Fly Postgres DATABASE_URL into spring.datasource.url={}", jdbcUrl);
        } catch (URISyntaxException e) {
            log.warn("DATABASE_URL could not be parsed as a URI ({}) — leaving spring.datasource.* untouched, " +
                    "Spring will fail with its own datasource error if that's needed", e.getMessage());
        }
    }
}
