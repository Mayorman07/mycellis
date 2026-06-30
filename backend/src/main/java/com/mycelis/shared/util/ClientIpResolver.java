package com.mycelis.shared.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Resolves the originating client IP from an HTTP request.
 *
 * <p>Honors the {@code X-Forwarded-For} header when the application is behind
 * a reverse proxy (Render, AWS ALB, nginx, Cloudflare). Falls back to the
 * direct remote address otherwise.</p>
 *
 * <p><b>Trust assumption:</b> if the application is deployed WITHOUT a proxy
 * but on the public internet, attackers can set arbitrary
 * {@code X-Forwarded-For} headers and the IP-based rate limiter can be
 * bypassed. In production, ensure either:
 * <ul>
 *   <li>A reverse proxy is in front (Render, ALB, etc.) — header is trustworthy.</li>
 *   <li>Or the app rejects requests with this header — header is ignored.</li>
 * </ul>
 */
@Component
public class ClientIpResolver {

    private static final String XFF_HEADER = "X-Forwarded-For";

    /**
     * Returns the client IP. Never returns null.
     * Multi-hop XFF chains like "client, proxy1, proxy2" — take the first (leftmost) entry.
     */
    public String resolve(HttpServletRequest request) {
        String xff = request.getHeader(XFF_HEADER);
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma == -1 ? xff : xff.substring(0, comma)).trim();
        }
        return request.getRemoteAddr();
    }
}