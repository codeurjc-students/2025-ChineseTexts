package com.chinesereads.backend.Security.jwt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import com.chinesereads.backend.Security.ClientIp;
import com.chinesereads.backend.Service.LoginRateLimiterService;
import com.chinesereads.backend.Service.UserService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Service
public class UserLoginService {

	private static final Logger log = LoggerFactory.getLogger(UserLoginService.class);

	private final AuthenticationManager authenticationManager;
	private final UserDetailsService userDetailsService;
	private final JwtTokenProvider jwtTokenProvider;
	private final UserService userService;

	private final LoginRateLimiterService loginRateLimiter;

	public UserLoginService(AuthenticationManager authenticationManager, UserDetailsService userDetailsService,
			JwtTokenProvider jwtTokenProvider, UserService userService, LoginRateLimiterService loginRateLimiter) {
		this.authenticationManager = authenticationManager;
		this.userDetailsService = userDetailsService;
		this.jwtTokenProvider = jwtTokenProvider;
		this.userService = userService;
		this.loginRateLimiter = loginRateLimiter;
	}

	public ResponseEntity<AuthResponse> login(HttpServletRequest request, HttpServletResponse response,
			LoginRequest loginRequest) {

		String username = loginRequest.getUsername();
		String clientIp = ClientIp.of(request);

		// Brute-force guard: once this IP or this account has burnt its failed attempts,
		// refuse BEFORE checking the password (a correct guess must not slip through).
		if (loginRateLimiter.isBlocked(clientIp, username)) {
			return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(new AuthResponse(
					AuthResponse.Status.FAILURE,
					"Too many failed login attempts. Please try again in a few minutes."));
		}

		Authentication authentication;
		try {
			authentication = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(username, loginRequest.getPassword()));
		} catch (DisabledException | LockedException e) {
			// Blocked account: reject with a clear, distinguishable response (403). It
			// still counts as a failure so a blocked account cannot be probed for free.
			loginRateLimiter.recordFailure(clientIp, username);
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new AuthResponse(
					AuthResponse.Status.FAILURE,
					"Your account has been blocked. Please contact support."));
		} catch (BadCredentialsException e) {
			// Wrong password or unknown email: same 401 and same body for both, so the
			// response never reveals which emails are registered.
			loginRateLimiter.recordFailure(clientIp, username);
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthResponse(
					AuthResponse.Status.FAILURE, "Invalid credentials."));
		}

		SecurityContextHolder.getContext().setAuthentication(authentication);
		loginRateLimiter.recordSuccess(username);

		UserDetails user = userDetailsService.loadUserByUsername(username);

		// Record the successful login moment for the admin panel.
		userService.recordLastAccess(username);

		HttpHeaders responseHeaders = new HttpHeaders();
		var newAccessToken = jwtTokenProvider.generateAccessToken(user);
		var newRefreshToken = jwtTokenProvider.generateRefreshToken(user);

		response.addCookie(buildTokenCookie(TokenType.ACCESS, newAccessToken));
		response.addCookie(buildTokenCookie(TokenType.REFRESH, newRefreshToken));

		AuthResponse loginResponse = new AuthResponse(AuthResponse.Status.SUCCESS,
				"Auth successful. Tokens are created in cookie.");
		return ResponseEntity.ok().headers(responseHeaders).body(loginResponse);
	}

	public ResponseEntity<AuthResponse> refresh(HttpServletResponse response, String refreshToken) {
		try {
			var claims = jwtTokenProvider.validateToken(refreshToken);
			UserDetails user = userDetailsService.loadUserByUsername(claims.getSubject());

			if (!user.isEnabled()) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new AuthResponse(
						AuthResponse.Status.FAILURE, "Your account has been blocked."));
			}

			var newAccessToken = jwtTokenProvider.generateAccessToken(user);
			response.addCookie(buildTokenCookie(TokenType.ACCESS, newAccessToken));

			AuthResponse loginResponse = new AuthResponse(AuthResponse.Status.SUCCESS,
					"Auth successful. Tokens are created in cookie.");
			return ResponseEntity.ok().body(loginResponse);

		} catch (Exception e) {
			log.error("Error while processing refresh token", e);
			AuthResponse loginResponse = new AuthResponse(AuthResponse.Status.FAILURE,
					"Failure while processing refresh token");
			return ResponseEntity.ok().body(loginResponse);
		}
	}

	public String logout(HttpServletResponse response) {
		SecurityContextHolder.clearContext();
		response.addCookie(TokenType.ACCESS.expiredCookie());
		response.addCookie(TokenType.REFRESH.expiredCookie());

		return "logout successfully";
	}

	private Cookie buildTokenCookie(TokenType type, String token) {
		Cookie cookie = new Cookie(type.cookieName, token);
		cookie.setMaxAge((int) type.duration.getSeconds());
		cookie.setHttpOnly(true);
		cookie.setPath("/");
		return cookie;
	}
}

