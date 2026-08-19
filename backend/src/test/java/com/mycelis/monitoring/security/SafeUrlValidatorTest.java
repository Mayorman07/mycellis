package com.mycelis.monitoring.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Pure unit test — no Spring context, DnsResolver is mocked. First unit test
 * (as opposed to @SpringBootTest integration test) in this codebase; the
 * class under test does its own DNS-independent parsing/blocklist logic, so
 * a full context isn't needed to exercise it.
 */
@ExtendWith(MockitoExtension.class)
class SafeUrlValidatorTest {

    @Mock
    private DnsResolver dnsResolver;

    private SafeUrlValidator validator;

    private void setUp() {
        validator = new SafeUrlValidator(dnsResolver);
    }

    // ---------- Allowed ----------

    @ParameterizedTest
    @ValueSource(strings = {
            "https://google.com",
            "http://example.com",
            "https://example.com:8443",
    })
    void allowsPublicHostnames(String url) throws Exception {
        setUp();
        lenient().when(dnsResolver.resolve("google.com")).thenReturn(addresses("142.250.64.14"));
        lenient().when(dnsResolver.resolve("example.com")).thenReturn(addresses("93.184.216.34"));

        assertThat(validator.validate(url).allowed()).isTrue();
    }

    @Test
    void allowsPublicIpLiteral() {
        setUp();
        assertThat(validator.validate("https://8.8.8.8").allowed()).isTrue();
    }

    // ---------- Blocked: IP literals ----------

    @Test
    void blocksLoopbackIpv4Literal() {
        setUp();
        SafeUrlValidator.Result result = validator.validate("http://127.0.0.1");
        assertThat(result.allowed()).isFalse();
        assertThat(result.reason()).isEqualTo("URLs pointing to internal addresses are not allowed");
    }

    @Test
    void blocksPrivateRfc1918Literal() {
        setUp();
        assertThat(validator.validate("https://192.168.1.1").allowed()).isFalse();
    }

    @Test
    void blocksLinkLocalMetadataIp() {
        setUp();
        assertThat(validator.validate("http://169.254.169.254").allowed()).isFalse();
    }

    @Test
    void blocksTenDotRange() {
        setUp();
        assertThat(validator.validate("http://10.0.0.1").allowed()).isFalse();
    }

    @Test
    void blocksOneSevenTwoRange() {
        setUp();
        assertThat(validator.validate("http://172.16.0.1").allowed()).isFalse();
    }

    @Test
    void blocksIpv6Loopback() {
        setUp();
        assertThat(validator.validate("http://[::1]").allowed()).isFalse();
    }

    @Test
    void blocksIpv6UniqueLocal() {
        setUp();
        assertThat(validator.validate("http://[fd12:3456:789a::1]").allowed()).isFalse();
    }

    @Test
    void blocksIpv6LinkLocal() {
        setUp();
        assertThat(validator.validate("http://[fe80::1]").allowed()).isFalse();
    }

    @Test
    void blocksBroadcastAddress() {
        setUp();
        assertThat(validator.validate("http://255.255.255.255").allowed()).isFalse();
    }

    @Test
    void blocksUnspecifiedAddress() {
        setUp();
        assertThat(validator.validate("http://0.0.0.0").allowed()).isFalse();
    }

    // ---------- Blocked: hostname literals ----------

    @Test
    void blocksLocalhostByName() {
        setUp();
        assertThat(validator.validate("http://localhost").allowed()).isFalse();
    }

    @Test
    void blocksGoogleMetadataHostname() {
        setUp();
        SafeUrlValidator.Result result = validator.validate("https://metadata.google.internal");
        assertThat(result.allowed()).isFalse();
        assertThat(result.reason()).isEqualTo("URLs pointing to internal addresses are not allowed");
    }

    @Test
    void blocksDotInternalSuffix() {
        setUp();
        assertThat(validator.validate("http://my-service.internal").allowed()).isFalse();
    }

    @Test
    void blocksDotLocalSuffix() {
        setUp();
        assertThat(validator.validate("http://printer.local").allowed()).isFalse();
    }

    // ---------- Edge case: hostname resolves to a private IP ----------

    @Test
    void blocksHostnameThatResolvesToPrivateIp() throws Exception {
        setUp();
        when(dnsResolver.resolve("sneaky.example.test")).thenReturn(addresses("10.0.0.5"));

        SafeUrlValidator.Result result = validator.validate("http://sneaky.example.test");

        assertThat(result.allowed()).isFalse();
        assertThat(result.reason()).isEqualTo("URLs pointing to internal addresses are not allowed");
    }

    @Test
    void allowsHostnameWhereOnlySomeResolvedIpsArePublic() throws Exception {
        setUp();
        // Any blocked IP in the set is enough to reject, even if others are public.
        when(dnsResolver.resolve("mixed.example.test"))
                .thenReturn(addresses("93.184.216.34", "127.0.0.1"));

        assertThat(validator.validate("http://mixed.example.test").allowed()).isFalse();
    }

    @Test
    void unresolvableHostnameIsRejected() throws Exception {
        setUp();
        when(dnsResolver.resolve("nowhere.example.test")).thenThrow(new UnknownHostException("nowhere.example.test"));

        assertThat(validator.validate("http://nowhere.example.test").allowed()).isFalse();
    }

    // ---------- Malformed URLs ----------

    @Test
    void malformedUrlIsRejected() {
        setUp();
        SafeUrlValidator.Result result = validator.validate("not a url at all");
        assertThat(result.allowed()).isFalse();
    }

    @Test
    void urlWithNoHostIsRejected() {
        setUp();
        SafeUrlValidator.Result result = validator.validate("file:///etc/passwd");
        assertThat(result.allowed()).isFalse();
    }

    private InetAddress[] addresses(String... ips) throws UnknownHostException {
        InetAddress[] result = new InetAddress[ips.length];
        for (int i = 0; i < ips.length; i++) {
            result[i] = InetAddress.getByName(ips[i]);
        }
        return result;
    }
}
