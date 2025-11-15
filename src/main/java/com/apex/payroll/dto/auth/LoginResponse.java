package com.apex.payroll.dto.auth;

import lombok.Builder;

import java.util.UUID;

@Builder
public record LoginResponse(
        UUID userId,
        String email,
        String firstName,
        String lastName,
        String fullName,
        String phoneNumber,
        String accessToken,
        String accessTokenExpiresIn,
        String refreshToken,
        String refreshTokenExpiresIn
) implements AuthResult {
}
