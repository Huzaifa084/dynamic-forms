package com.apex.payroll.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
@Getter
@Setter
public class LoginRequest {
    @Schema(description = "User's email address for login", example = "devuser@yopmail.com")
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    @Schema(description = "User's password for login", example = "pass123")
    @NotBlank(message = "Password is required")
    private String password;
}
