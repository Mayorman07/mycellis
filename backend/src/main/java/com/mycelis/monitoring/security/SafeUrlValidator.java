package com.mycelis.monitoring.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

/**
 * Checks whether a stalk's URL is safe to send an outbound HTTP request to —
 * rejects loopback, private (RFC 1918), link-local (incl. cloud metadata
 * endpoints), IPv6 unique-local, multicast, broadcast, unspecified, and a
 * handful of well-known internal hostnames/suffixes.
 *
 * <p>Two callers need this (StalkServiceImpl, wants a 400; PulseEngine,
 * wants a failed-pulse record and never an exception) so this returns a
 * Result instead of throwing — each caller decides its own failure mode.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SafeUrlValidator {

    private static final long DNS_TIMEOUT_SECONDS = 5;

    private static final String BLOCKED_REASON = "URLs pointing to internal addresses are not allowed";

    private static final Pattern IPV4_LITERAL =
            Pattern.compile("^(25[0-5]|2[0-4]\\d|1?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|1?\\d?\\d)){3}$");

    private static final Set<String> BLOCKED_HOSTNAMES = Set.of(
            "localhost",
            "localhost.localdomain",
            "ip6-localhost",
            "ip6-loopback",
            "metadata.google.internal",
            "metadata.azure.com"
    );

    private static final Set<String> BLOCKED_HOSTNAME_SUFFIXES = Set.of(
            ".local", ".internal", ".localdomain"
    );

    private final DnsResolver dnsResolver;

    public Result validate(String url) {
        String host;
        try {
            host = new URI(url).getHost();
        } catch (URISyntaxException e) {
            return Result.reject("URL could not be parsed");
        }

        if (host == null || host.isBlank()) {
            return Result.reject("URL is missing a host");
        }

        String normalizedHost = stripBrackets(host).toLowerCase(Locale.ROOT);

        if (isBlockedHostnameLiteral(normalizedHost)) {
            return Result.reject(BLOCKED_REASON);
        }

        if (isIpLiteral(normalizedHost)) {
            return validateIpLiteral(normalizedHost);
        }

        return validateHostname(normalizedHost);
    }

    private boolean isBlockedHostnameLiteral(String host) {
        if (BLOCKED_HOSTNAMES.contains(host)) {
            return true;
        }
        for (String suffix : BLOCKED_HOSTNAME_SUFFIXES) {
            if (host.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    private Result validateIpLiteral(String host) {
        InetAddress address;
        try {
            // Safe: a string that already matched the IPv4 regex or contains a
            // ':' is a numeric literal, so this is a local parse, not a
            // network call — see isIpLiteral().
            address = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            return Result.reject("URL host could not be parsed as an IP address");
        }
        return isBlockedAddress(address) ? Result.reject(BLOCKED_REASON) : Result.allow();
    }

    private Result validateHostname(String host) {
        InetAddress[] resolved;
        try {
            resolved = resolveWithTimeout(host);
        } catch (UnknownHostException e) {
            return Result.reject("URL host could not be resolved");
        } catch (TimeoutException e) {
            log.warn("DNS resolution timed out for host={}", host);
            return Result.reject("URL host could not be resolved in time");
        }

        for (InetAddress address : resolved) {
            if (isBlockedAddress(address)) {
                return Result.reject(BLOCKED_REASON);
            }
        }
        return Result.allow();
    }

    /**
     * Timeout is on the wait, not the underlying DNS lookup. Under high
     * volume, threads can accumulate. Revisit with an async DNS resolver
     * (dnsjava, Netty) if the pulse executor sees thread pressure.
     */
    private InetAddress[] resolveWithTimeout(String host) throws UnknownHostException, TimeoutException {
        CompletableFuture<InetAddress[]> future = CompletableFuture.supplyAsync(() -> {
            try {
                return dnsResolver.resolve(host);
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        });

        try {
            return future.get(DNS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof UnknownHostException uhe) {
                throw uhe;
            }
            if (cause != null && cause.getCause() instanceof UnknownHostException uhe) {
                throw uhe;
            }
            throw new UnknownHostException(host);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UnknownHostException(host);
        }
    }

    private boolean isIpLiteral(String host) {
        return IPV4_LITERAL.matcher(host).matches() || host.contains(":");
    }

    private String stripBrackets(String host) {
        if (host.startsWith("[") && host.endsWith("]")) {
            return host.substring(1, host.length() - 1);
        }
        return host;
    }

    private boolean isBlockedAddress(InetAddress address) {
        if (address.isLoopbackAddress()      // 127.0.0.0/8, ::1
                || address.isSiteLocalAddress()  // 10/8, 172.16/12, 192.168/16
                || address.isLinkLocalAddress()  // 169.254.0.0/16, fe80::/10
                || address.isMulticastAddress()  // 224.0.0.0/4, ff00::/8
                || address.isAnyLocalAddress()) { // 0.0.0.0, ::
            return true;
        }
        return isBroadcast(address) || isIpv6UniqueLocal(address);
    }

    private boolean isBroadcast(InetAddress address) {
        if (!(address instanceof Inet4Address)) {
            return false;
        }
        for (byte b : address.getAddress()) {
            if (b != (byte) 0xFF) {
                return false;
            }
        }
        return true;
    }

    /** fc00::/7 — not covered by isSiteLocalAddress(), which only knows the deprecated fec0::/10 range. */
    private boolean isIpv6UniqueLocal(InetAddress address) {
        if (!(address instanceof Inet6Address)) {
            return false;
        }
        return (address.getAddress()[0] & 0xFE) == 0xFC;
    }

    public record Result(boolean allowed, String reason) {
        public static Result allow() {
            return new Result(true, null);
        }

        public static Result reject(String reason) {
            return new Result(false, reason);
        }
    }
}
