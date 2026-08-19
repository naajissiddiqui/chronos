package com.chronos.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtTokenValidator {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenValidator.class);
    private final SecretKey key;

    public JwtTokenValidator(@Value("${jwt.secret}") String secret) {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException e) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, Math.min(keyBytes.length, 32));
            keyBytes = padded;
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public Optional<AuthenticatedPrincipal> validateAndExtractPrincipal(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String userIdStr = claims.get("userId", String.class);
            String orgIdStr = claims.get("organizationId", String.class);
            String role = claims.get("role", String.class);

            if (userIdStr == null || orgIdStr == null || role == null) {
                logger.warn("JWT claims missing required identity fields");
                return Optional.empty();
            }

            UUID userId = UUID.fromString(userIdStr);
            UUID organizationId = UUID.fromString(orgIdStr);

            return Optional.of(new AuthenticatedPrincipal(userId, organizationId, role, "JWT"));
        } catch (JwtException | IllegalArgumentException e) {
            logger.debug("Invalid or expired JWT: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
