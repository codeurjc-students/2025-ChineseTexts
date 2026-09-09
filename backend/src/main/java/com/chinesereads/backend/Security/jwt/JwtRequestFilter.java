package com.chinesereads.backend.Security.jwt;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {
	
	private static final Logger log = LoggerFactory.getLogger(JwtRequestFilter.class);

	private final UserDetailsService userDetailsService;

	private final JwtTokenProvider jwtTokenProvider;

	public JwtRequestFilter(UserDetailsService userDetailsService, JwtTokenProvider jwtTokenProvider) {
		this.userDetailsService = userDetailsService;
		this.jwtTokenProvider = jwtTokenProvider;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

		try {
			var claims = jwtTokenProvider.validateToken(request, true);
			var userDetails = userDetailsService.loadUserByUsername(claims.getSubject());

			// A blocked account is "disabled" (see RepositoryUserDetailsService). Login and
			// refresh already reject it, but a cookie issued BEFORE the block stays valid
			// for up to 7 days, so the check has to happen here too: the request goes on
			// unauthenticated (protected endpoints answer 401) and the browser is told to
			// drop both cookies, which ends the session on the spot.
			if (!userDetails.isEnabled()) {
				// No identifier in the log on purpose: no personal data in server logs.
				log.info("Dropped the session of a blocked account on {}", request.getRequestURI());
				response.addCookie(TokenType.ACCESS.expiredCookie());
				response.addCookie(TokenType.REFRESH.expiredCookie());
				filterChain.doFilter(request, response);
				return;
			}

			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
						userDetails, null, userDetails.getAuthorities());
				
			authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			SecurityContextHolder.getContext().setAuthentication(authentication);
		} catch (Exception ex) {
			//Avoid logging when no token is found
			if(!ex.getMessage().equals("No access token cookie found in request")) {
				log.error("Exception processing JWT Token: ", ex);
			}			
		}

		filterChain.doFilter(request, response);
	}	

}
