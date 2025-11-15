package com.apex.payroll.security;

import com.apex.payroll.exception.InvalidStateException;
import com.apex.payroll.exception.InvalidTokenException;
import com.apex.payroll.exception.TokenExpiredException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class JwtService {

    private final JwtConfig jwtConfig;
    private SecretKey encryptionKey;

    @PostConstruct
    void init() {
        validateSecretKey();
        validateEncryptionKey();
        this.encryptionKey = new SecretKeySpec(
                ensureKeyLength(jwtConfig.getEncryptionSecret().getBytes(StandardCharsets.UTF_8)),
                "AES"
        );
    }

    private byte[] ensureKeyLength(byte[] key) {
        if (key.length == 32) {
            return key;
        }

        byte[] result = new byte[32];
        System.arraycopy(key, 0, result, 0, Math.min(key.length, 32));
        return result;
    }

    public String generateJwtToken() {
        return buildToken(JwtConfig.TOKEN_TYPE_ACCESS, jwtConfig.getAccessTtlMillis());
    }

    public String generateRefreshToken(long duration) {
        return buildToken(JwtConfig.TOKEN_TYPE_REFRESH, duration);
    }

    public String generatePasswordResetToken() {
        return buildToken(JwtConfig.TOKEN_TYPE_RESET, jwtConfig.getPasswordResetTtlMillis());
    }

    public UserDetailsImpl parseToken(String token) {
        Claims claims = extractAllClaims(token);
        validateTokenClaims(claims);
        return new UserDetailsImpl(
                getUserId(claims),
                getEmail(claims),
                getUsername(claims),
                claims.get(JwtConfig.CLAIM_TOKEN_VER, Long.class)
        );
    }

    public void validateJwtToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            validateTokenExpiry(claims);
        } catch (ExpiredJwtException ex) {
            throw new TokenExpiredException("Token expired", ex);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidTokenException("Invalid token", ex);
        }
    }

    public Claims validate(String token, String expectedType) {
        try {
            Claims claims = extractAllClaims(token);
            validateTokenExpiry(claims);
            String type = claims.get(JwtConfig.TOKEN_TYPE, String.class);
            log.debug("Type of claims is {}", type);
            if (expectedType != null && !expectedType.equals(type)) {
                throw new InvalidTokenException("Invalid token type: " + type);
            }
            return claims;
        } catch (ExpiredJwtException ex) {
            throw new TokenExpiredException("Token expired", ex);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidTokenException("Invalid token", ex);
        }
    }

    public String buildToken(String tokenType, long duration) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Map<String, Object> claims = createClaims(userDetails, tokenType);
        Instant now = Instant.now();

        return Jwts.builder()
                .header().type("JWE").and()
                .issuer(jwtConfig.getIssuer())
                .subject(userDetails.getEmail())
                .claims(claims)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(duration)))
                .compressWith(Jwts.ZIP.DEF)
                .encryptWith(
                        encryptionKey,
                        Jwts.ENC.A256GCM
                )
                .compact();
    }

    private Map<String, Object> createClaims(UserDetailsImpl user, String tokenType) {
        Map<String, Object> claims = new LinkedHashMap<>();

        claims.put(JwtConfig.CLAIM_USER_ID, user.getId());
        claims.put(JwtConfig.TOKEN_TYPE, tokenType);
        claims.put(JwtConfig.CLAIM_TOKEN_VER, user.getTokenVersion());
        claims.put("jti", UUID.randomUUID().toString());

        if (!user.getEmail().equals(user.getUsername())) claims.put(JwtConfig.CLAIM_USERNAME, user.getUsername());
        if (JwtConfig.TOKEN_TYPE_RESET.equals(tokenType)) claims.put("nonce", UUID.randomUUID().toString());

        return claims;
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .decryptWith(encryptionKey)
                .build()
                .parseEncryptedClaims(token)
                .getPayload();
    }

    private void validateSecretKey() {
        byte[] keyBytes = jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < jwtConfig.getMinSecretKeyLength()) {
            throw new InvalidStateException(
                    "JWT secret key must be at least 512 bits (64 characters). Current length: " +
                            (keyBytes.length * 8) + " bits");
        }
    }

    private void validateEncryptionKey() {
        if (jwtConfig.getEncryptionSecret() == null || jwtConfig.getEncryptionSecret().length() < 32) {
            throw new InvalidStateException(
                    "Encryption key must be at least 256 bits (32 characters)"
            );
        }
    }

    private void validateTokenClaims(Claims claims) {
        if (!claims.containsKey(JwtConfig.CLAIM_USER_ID)) throw new InvalidTokenException("Missing user ID claim");
        if (!claims.containsKey(JwtConfig.CLAIM_TOKEN_VER)) throw new InvalidTokenException("Missing token version");
    }

    private void validateTokenExpiry(Claims claims) {
        if (claims.getExpiration().before(Date.from(Instant.now()))) throw new TokenExpiredException("Token expired");
    }

    public String getEmail(Claims claims) {
        return claims.getSubject();
    }

    public String getUsername(Claims claims) {
        return claims.getOrDefault(JwtConfig.CLAIM_USERNAME, claims.getSubject()).toString();
    }

    public Long getUserId(Claims claims) {
        return claims.get(JwtConfig.CLAIM_USER_ID, Long.class);
    }

    public Long getTokenVersion(Claims claims) {
        return claims.get(JwtConfig.CLAIM_TOKEN_VER, Long.class);
    }

    public String getTokenId(Claims claims) {
        return claims.get("jti", String.class);
    }

    public Long extractUserIdFromToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return getUserId(claims);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidTokenException("Invalid token", ex);
        }
    }
}