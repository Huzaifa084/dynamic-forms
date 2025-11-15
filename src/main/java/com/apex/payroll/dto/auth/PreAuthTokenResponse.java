package com.apex.payroll.dto.auth;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public final class PreAuthTokenResponse implements AuthResult {
    private String preAuthToken;
    private long expiresIn;
    private String emailMasked;
}
