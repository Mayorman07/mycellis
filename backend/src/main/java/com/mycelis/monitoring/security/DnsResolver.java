package com.mycelis.monitoring.security;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Seam over DNS resolution so SafeUrlValidator can be unit-tested without a
 * real resolver in the loop — tests inject a mock, production gets
 * SystemDnsResolver.
 */
public interface DnsResolver {
    InetAddress[] resolve(String host) throws UnknownHostException;
}
