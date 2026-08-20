package com.mycelis.monitoring.service;

import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/**
 * Normalizes a stalk URL to a canonical form for duplicate detection —
 * lowercase scheme + host, default ports (80/443) dropped, bare-root path
 * ("/") collapsed to nothing, fragment stripped, query string and path case
 * preserved as-is. Userinfo (if present) is dropped since it's never read
 * here; IPv6 literal hosts round-trip unchanged (brackets included, already
 * lowercase).
 *
 * <p>Single source of truth for these rules going forward. V14's backfill
 * migration reimplements the same rules once in SQL as a static, one-time
 * snapshot for existing rows — that SQL is not kept in sync with this class
 * after it ships, by design (migrations are historical record, not live
 * code) — but every row written from this point on goes through here.</p>
 *
 * <p>Callers must validate the URL (e.g. via SafeUrlValidator) before
 * calling this — it assumes a parseable http(s) URL and throws rather than
 * guessing if that assumption doesn't hold.</p>
 */
@Component
public class UrlNormalizer {

    public String normalize(String url) {
        if (url == null) {
            throw new IllegalArgumentException("Cannot normalize a null URL");
        }

        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Cannot normalize an unparseable URL: " + url, e);
        }

        String scheme = uri.getScheme();
        String host = uri.getHost();
        if (scheme == null || host == null) {
            throw new IllegalArgumentException("Cannot normalize a URL with no scheme/host: " + url);
        }
        scheme = scheme.toLowerCase(Locale.ROOT);
        host = host.toLowerCase(Locale.ROOT);

        int port = uri.getPort();
        boolean isDefaultPort = port == -1
                || ("http".equals(scheme) && port == 80)
                || ("https".equals(scheme) && port == 443);

        String path = uri.getRawPath();
        boolean hasPath = path != null && !path.isEmpty() && !path.equals("/");

        String query = uri.getRawQuery();
        boolean hasQuery = query != null && !query.isEmpty();

        StringBuilder normalized = new StringBuilder();
        normalized.append(scheme).append("://").append(host);
        if (!isDefaultPort) {
            normalized.append(':').append(port);
        }
        if (hasPath) {
            normalized.append(path);
        }
        if (hasQuery) {
            normalized.append('?').append(query);
        }
        // Fragment intentionally never appended — always stripped.
        // Userinfo intentionally never appended — always stripped.

        return normalized.toString();
    }
}
