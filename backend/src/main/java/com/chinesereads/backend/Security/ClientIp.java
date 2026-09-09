package com.chinesereads.backend.Security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolves the client IP behind the reverse proxy. Caddy terminates TLS in front of
 * the backend, so the socket address is always the proxy's; the real client is the
 * first entry of X-Forwarded-For. Shared by every per-IP rate limiter (login,
 * forgot-password, TTS) so they all agree on what "the same client" means.
 */
public final class ClientIp {

    private ClientIp() {
    }

    public static String of(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // X-Forwarded-For may be a comma-separated list; the first entry is the client.
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        String remote = request.getRemoteAddr();
        return remote != null ? remote : "unknown";
    }
}
