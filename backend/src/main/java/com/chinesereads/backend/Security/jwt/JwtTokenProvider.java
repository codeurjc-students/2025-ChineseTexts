package com.chinesereads.backend.Security.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class JwtTokenProvider {

	// Clave de firma HS256. Con jwt.secret configurado la clave es estable entre
	// arranques, así que las sesiones sobreviven a los redeploys; sin configurar
	// se genera una aleatoria por arranque (suficiente en desarrollo y tests).
	private final SecretKey jwtSecret;

	// Crear parser con la clave
	private final JwtParser jwtParser;

	public JwtTokenProvider(@Value("${jwt.secret:}") String configuredSecret) {
		this.jwtSecret = (configuredSecret == null || configuredSecret.isBlank())
				? Keys.secretKeyFor(SignatureAlgorithm.HS256)
				// hmacShaKeyFor exige >= 256 bits (32 bytes): un secreto corto
				// aborta el arranque en vez de firmar con una clave débil.
				: Keys.hmacShaKeyFor(configuredSecret.trim().getBytes(StandardCharsets.UTF_8));
		this.jwtParser = Jwts.parserBuilder()
				.setSigningKey(jwtSecret)
				.build();
	}

	public String tokenStringFromHeaders(HttpServletRequest req){
		String bearerToken = req.getHeader(HttpHeaders.AUTHORIZATION);
		if (bearerToken == null) {
			throw new IllegalArgumentException("Missing Authorization header");
		}
		if(!bearerToken.startsWith("Bearer ")){
			throw new IllegalArgumentException("Authorization header does not start with Bearer: " + bearerToken);
		}
		return bearerToken.substring(7);
	}

	private String tokenStringFromCookies(HttpServletRequest request) {
		var cookies = request.getCookies();
		if (cookies == null) {
			throw new IllegalArgumentException("No cookies found in request");
		}

		for (Cookie cookie : cookies) {
			if (TokenType.ACCESS.cookieName.equals(cookie.getName())) {
				String accessToken = cookie.getValue();
				if (accessToken == null) {
					throw new IllegalArgumentException("Cookie %s has null value".formatted(TokenType.ACCESS.cookieName));
				}

				return accessToken;
			}
		}
		throw new IllegalArgumentException("No access token cookie found in request");
	}

	public Claims validateToken(HttpServletRequest req, boolean fromCookie){
		var token = fromCookie?
				tokenStringFromCookies(req):
				tokenStringFromHeaders(req);
		return validateToken(token);
	}

	public Claims validateToken(String token) {
		return jwtParser.parseClaimsJws(token).getBody(); // ✅ Esto sí funciona en 0.11.5
	}

	public String generateAccessToken(UserDetails userDetails) {
		return buildToken(TokenType.ACCESS, userDetails).compact();
	}

	public String generateRefreshToken(UserDetails userDetails) {
		var token = buildToken(TokenType.REFRESH, userDetails);
        return token.compact();
	}

	private JwtBuilder buildToken(TokenType tokenType, UserDetails userDetails) {
    var currentDate = new Date();
    var expiryDate = Date.from(currentDate.toInstant().plus(tokenType.duration));
    
    return Jwts.builder()
            .setSubject(userDetails.getUsername()) // ✅ Usa "setSubject"
            .setIssuedAt(currentDate)              // ✅ Usa "setIssuedAt"
            .setExpiration(expiryDate)             // ✅ Usa "setExpiration"
            .claim("roles", userDetails.getAuthorities())
            .claim("type", tokenType.name())
            .signWith(jwtSecret);                  // Correcto para 0.11.5
	}

}

