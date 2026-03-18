package com.sportstreamlive.streaming.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;

/**
 * Utilidad para generar y validar tokens JWT.
 * Se usa LocalAuth (E1). Preparado para integrarse
 * con el flujo OAuth2 (Microsoft/Google) a futuro.
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    private Key signingKey;

    @PostConstruct
    public void init() {
        // La clave se inicializa una sola vez al arrancar el contexto
        signingKey = Keys.hmacShaKeyFor(secret.getBytes());
    }

    /** Genera un JWT con el email del usuario como subject. */
    public String generateToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /** Extrae el email (subject) del token. */
    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    /** Valida el token. Lanza excepcion si es invalido o expirado. */
    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
