package com.lab2.demo.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final Key key;
    private final long accessMs;
    private final long refreshMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access.expiration-ms}") long accessMs,
            @Value("${jwt.refresh.expiration-ms}") long refreshMs
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessMs = accessMs;
        this.refreshMs = refreshMs;
    }

    public String generateAccessToken(String username, String role, UUID sessionId) {
        Instant now = Instant.now();
        Instant exp = now.plusMillis(accessMs);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .addClaims(Map.of(
                        "type", JwtTokenType.ACCESS.name(),
                        "role", role,
                        "sessionId", sessionId.toString()
                ))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(String username, UUID sessionId) {
        Instant now = Instant.now();
        Instant exp = now.plusMillis(refreshMs);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .addClaims(Map.of(
                        "type", JwtTokenType.REFRESH.name(),
                        "sessionId", sessionId.toString()
                ))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Jws<Claims> parse(String token) throws JwtException {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
    }

    public boolean validate(String token) {
        parse(token);
        return true;
    }

    public JwtTokenType getType(String token) {
        Claims c = parse(token).getBody();
        return JwtTokenType.valueOf(c.get("type", String.class));
    }

    public String getUsername(String token) {
        return parse(token).getBody().getSubject();
    }

    public String getRole(String token) {
        return parse(token).getBody().get("role", String.class);
    }

    public UUID getSessionId(String token) {
        String s = parse(token).getBody().get("sessionId", String.class);
        return UUID.fromString(s);
    }

    public Instant getExpiry(String token) {
        return parse(token).getBody().getExpiration().toInstant();
    }
}
