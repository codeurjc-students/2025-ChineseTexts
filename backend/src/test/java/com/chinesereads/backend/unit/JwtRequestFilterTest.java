package com.chinesereads.backend.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import com.chinesereads.backend.Security.jwt.JwtRequestFilter;
import com.chinesereads.backend.Security.jwt.JwtTokenProvider;
import com.chinesereads.backend.Security.jwt.TokenType;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;

/**
 * The per-request half of the "blocked account" contract. Login and refresh reject a
 * blocked user, but a cookie issued BEFORE the block is still a valid JWT, so the
 * filter is the only place that can end that session: it must not authenticate the
 * request and must tell the browser to drop both cookies. Everything else (a normal
 * user, a request without cookie) has to keep behaving exactly as before.
 */
public class JwtRequestFilterTest {

    private static final String EMAIL = "maria@test.com";

    private JwtTokenProvider tokenProvider;
    private UserDetailsService userDetailsService;
    private JwtRequestFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    @BeforeEach
    public void setUp() {
        tokenProvider = mock(JwtTokenProvider.class);
        userDetailsService = mock(UserDetailsService.class);
        filter = new JwtRequestFilter(userDetailsService, tokenProvider);

        request = new MockHttpServletRequest("GET", "/api/users/me");
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void givenValidCookieFor(boolean enabled) {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(EMAIL);
        when(tokenProvider.validateToken(any(), anyBoolean())).thenReturn(claims);
        UserDetails details = new User(EMAIL, "hash", enabled, true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(details);
    }

    @Test
    @DisplayName("A valid cookie of an active account authenticates the request (unchanged behaviour)")
    public void activeAccountIsAuthenticated() throws Exception {
        givenValidCookieFor(true);

        filter.doFilter(request, response, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals(EMAIL, auth.getName());
        assertNotNull(chain.getRequest(), "the request must continue down the chain");
        assertEquals(0, response.getCookies().length, "no cookie is touched for an active account");
    }

    @Test
    @DisplayName("A valid cookie of a BLOCKED account does not authenticate and both cookies are expired")
    public void blockedAccountSessionIsDropped() throws Exception {
        givenValidCookieFor(false);

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(),
                "a blocked account must not be authenticated, whatever its cookie says");
        assertNotNull(chain.getRequest(), "the request still continues (protected endpoints answer 401)");

        Cookie[] cookies = response.getCookies();
        assertEquals(2, cookies.length);
        for (Cookie cookie : cookies) {
            assertEquals(0, cookie.getMaxAge(), cookie.getName() + " must be deleted by the browser");
            assertEquals("", cookie.getValue());
            assertEquals("/", cookie.getPath());
            assertEquals(true, cookie.isHttpOnly());
        }
        assertEquals(TokenType.ACCESS.cookieName, cookies[0].getName());
        assertEquals(TokenType.REFRESH.cookieName, cookies[1].getName());
    }

    @Test
    @DisplayName("A request without cookie passes through unauthenticated and untouched")
    public void noCookieIsAnonymous() throws Exception {
        when(tokenProvider.validateToken(any(), anyBoolean()))
                .thenThrow(new IllegalArgumentException("No access token cookie found in request"));

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertNotNull(chain.getRequest());
        assertEquals(0, response.getCookies().length);
    }
}
