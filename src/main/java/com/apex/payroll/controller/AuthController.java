package com.apex.payroll.controller;

import com.apex.payroll.dto.auth.LoginRequest;
import com.apex.payroll.dto.auth.LoginResponse;
import com.apex.payroll.dto.base.BaseResponseEntity;
import com.apex.payroll.dto.base.ResponseBuilder;
import com.apex.payroll.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public BaseResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest req, HttpServletResponse response
    ) {
        LoginResponse resp = authService.login(req);
        addRefreshCookie(response, resp.refreshToken(), resp.refreshTokenExpiresIn());
        return ResponseBuilder.success(resp, "Login successful");
    }


    @PostMapping("/refresh")
    @Operation(summary = """
            Handles token refresh requests for both web and mobile clients.
            This method extracts the refresh token from multiple possible sources: -
            A cookie (`refreshToken`) for web clients. -
            An `Authorization` header with the prefix "Refresh " for mobile clients. -
            A request body parameter (`refreshToken`) as a fallback.
            Once the token is identified, it is validated and refreshed using the `AuthService`.
            A new refresh token is then added to the response as a secure HTTP-only cookie.
            Params:
            cookie – The refresh token from the cookie (optional).
            header – The refresh token from the Authorization header (optional).
            body – The refresh token from the request body (optional).
            response – The HTTP response to which the new refresh token cookie is added.
            Returns:
            A response entity containing the new access token and refresh token.""",
            description = "Refresh")
    public BaseResponseEntity<LoginResponse> refresh(
            @CookieValue(value = "refreshToken", required = false) String cookie,
            @RequestHeader(value = "Authorization", required = false) String header,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletResponse response
    ) {
        String token = null;
        if (header != null && header.startsWith("Refresh ")) token = header.substring(8);
        if (token == null && cookie != null) token = cookie;
        if (token == null && body != null) token = body.get("refreshToken");
        log.debug("Refreshing token: {}", token);

        if (token == null || token.trim().isEmpty()) {
            return ResponseBuilder.badRequest("refreshToken", "Refresh token is required." +
                    " Please provide it via cookie, Authorization header (with 'refreshToken ' prefix), or request body.");
        }

        LoginResponse resp = authService.refresh(token);
        addRefreshCookie(response, resp.refreshToken(), resp.refreshTokenExpiresIn());
        return ResponseBuilder.success(resp, "Token refreshed successfully");
    }

    @PostMapping("/logout")
    public BaseResponseEntity<Void> logout(HttpServletResponse response) {
        authService.logout();
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "").path("/").
                maxAge(0).httpOnly(true).secure(true).sameSite("Strict").build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseBuilder.success("Logout successful");
    }


    private void addRefreshCookie(HttpServletResponse response, String token, String ttlString) {
        long ttlSeconds = Long.parseLong(ttlString.split(" ")[0]);

        ResponseCookie cookie = ResponseCookie.from("refreshToken", token)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(ttlSeconds)
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
