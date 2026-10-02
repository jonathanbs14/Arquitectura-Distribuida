package com.prueba.graftsql.credito.auth.adapter;

import com.prueba.graftsql.credito.auth.application.ports.JwtPort;
import com.prueba.graftsql.credito.auth.domain.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import jakarta.annotation.PostConstruct;
@Component
public class JwtAdapter implements JwtPort {

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${jwt.expiration:3600000}")
    private long jwtExpiration;

    @Value("${jwt.refresh-expiration:86400000}")
    private long refreshTokenExpiration;

    @PostConstruct
    void validateSecret() {
        if (jwtSecret == null || jwtSecret.getBytes(StandardCharsets.UTF_8).length < 64) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 64 bytes para usar HS512");
        }
    }

    @Override
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", user.getEmail());
        claims.put("fullName", user.getFullName());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getId())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS512)
                .compact();
    }

    @Override
    public String generateRefreshToken(User user) {
        return Jwts.builder()
                .setSubject(user.getId())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                .signWith(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS512)
                .compact();
    }
}
