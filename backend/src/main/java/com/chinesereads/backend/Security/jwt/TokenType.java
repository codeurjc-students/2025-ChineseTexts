package com.chinesereads.backend.Security.jwt;

import java.time.Duration;

import jakarta.servlet.http.Cookie;

public enum TokenType {

    ACCESS(Duration.ofDays(7), "AuthToken"),
    REFRESH(Duration.ofDays(7), "RefreshToken");

    /**
     * Token lifetime in seconds
     */
    public final Duration duration;
    public final String cookieName;

    TokenType(Duration duration, String cookieName) {
        this.duration = duration;
        this.cookieName = cookieName;
    }

    /**
     * Cookie that deletes this token from the browser (empty value, max-age 0). Shared
     * by logout and by the request filter when it drops a blocked account's session,
     * so both paths clear exactly the same cookie (same name, path and flags).
     */
    public Cookie expiredCookie() {
        Cookie cookie = new Cookie(cookieName, "");
        cookie.setMaxAge(0);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        return cookie;
    }
}
