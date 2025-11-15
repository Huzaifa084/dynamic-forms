package com.apex.payroll.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record OtpVerifyRequest(
    @NotBlank(message = "OTP is required") 
    String otp
) {}
