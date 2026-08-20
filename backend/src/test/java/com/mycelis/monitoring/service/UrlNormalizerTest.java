package com.mycelis.monitoring.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UrlNormalizerTest {

    private final UrlNormalizer normalizer = new UrlNormalizer();

    @Test
    void lowercasesSchemeAndHost() {
        assertThat(normalizer.normalize("HTTPS://EXAMPLE.COM/Path"))
                .isEqualTo("https://example.com/Path");
    }

    @Test
    void dropsDefaultHttpPort() {
        assertThat(normalizer.normalize("http://example.com:80/foo"))
                .isEqualTo("http://example.com/foo");
    }

    @Test
    void dropsDefaultHttpsPort() {
        assertThat(normalizer.normalize("https://example.com:443/foo"))
                .isEqualTo("https://example.com/foo");
    }

    @Test
    void keepsNonDefaultPort() {
        assertThat(normalizer.normalize("https://example.com:8443/foo"))
                .isEqualTo("https://example.com:8443/foo");
    }

    @Test
    void collapsesBareRootPath() {
        assertThat(normalizer.normalize("https://example.com/"))
                .isEqualTo("https://example.com");
    }

    @Test
    void collapsesEmptyPath() {
        assertThat(normalizer.normalize("https://example.com"))
                .isEqualTo("https://example.com");
    }

    @Test
    void doesNotStripTrailingSlashOnDeeperPaths() {
        assertThat(normalizer.normalize("https://example.com/foo/"))
                .isEqualTo("https://example.com/foo/");
    }

    @Test
    void stripsFragment() {
        assertThat(normalizer.normalize("https://example.com/path#section"))
                .isEqualTo("https://example.com/path");
    }

    @Test
    void keepsQueryString() {
        assertThat(normalizer.normalize("https://example.com/path?foo=bar&baz=1"))
                .isEqualTo("https://example.com/path?foo=bar&baz=1");
    }

    @Test
    void preservesPathCase() {
        assertThat(normalizer.normalize("https://example.com/CamelCase/Path"))
                .isEqualTo("https://example.com/CamelCase/Path");
    }

    @Test
    void combinesAllRulesTogether() {
        assertThat(normalizer.normalize("HTTPS://EXAMPLE.COM:443/MixedCase?q=1#frag"))
                .isEqualTo("https://example.com/MixedCase?q=1");
    }

    @Test
    void unparseableUrlThrows() {
        assertThatThrownBy(() -> normalizer.normalize("not a url at all"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void nullUrlThrows() {
        assertThatThrownBy(() -> normalizer.normalize(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void emptyStringThrows() {
        assertThatThrownBy(() -> normalizer.normalize(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ipv6HostRoundTrips() {
        assertThat(normalizer.normalize("https://[::1]/foo"))
                .isEqualTo("https://[::1]/foo");
    }

    @Test
    void userinfoIsStripped() {
        assertThat(normalizer.normalize("https://user:pass@example.com/"))
                .isEqualTo("https://example.com");
    }
}
