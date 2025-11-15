package com.apex.payroll.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshAllowListService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String REFRESH_KEY_PREFIX = "user:";
    private static final String REFRESH_KEY_SUFFIX = ":currentRefreshId";
    private static final Duration DEFAULT_TTL = Duration.ofDays(30); // Match refresh token TTL

    /**
     * Store the current refresh token ID for a user
     */
    public void storeRefreshTokenId(Long userId, String refreshTokenId) {
        String key = buildKey(userId);
        redisTemplate.opsForValue().set(key, refreshTokenId, DEFAULT_TTL);
        log.debug("Stored refresh token ID for user: {}", userId);
    }

    /**
     * Get the current refresh token ID for a user
     */
    public Optional<String> getCurrentRefreshTokenId(Long userId) {
        String key = buildKey(userId);
        String refreshTokenId = redisTemplate.opsForValue().get(key);
        return Optional.ofNullable(refreshTokenId);
    }

    /**
     * Check if the provided refresh token ID is valid for the user
     */
    public boolean isValidRefreshToken(Long userId, String refreshTokenId) {
        if (refreshTokenId == null) return true;

        return getCurrentRefreshTokenId(userId)
                .map(storedId -> storedId.equals(refreshTokenId))
                .orElse(false);
    }

    /**
     * Revoke the current refresh token for a user
     */
    public void revokeRefreshToken(Long userId) {
        String key = buildKey(userId);
        redisTemplate.delete(key);
        log.debug("Revoked refresh token for user: {}", userId);
    }

    /**
     * Update the refresh token ID (for rotation)
     */
    public void rotateRefreshToken(Long userId, String newRefreshTokenId) {
        storeRefreshTokenId(userId, newRefreshTokenId);
        log.debug("Rotated refresh token for user: {}", userId);
    }

    private String buildKey(Long userId) {
        return REFRESH_KEY_PREFIX + userId + REFRESH_KEY_SUFFIX;
    }
}
