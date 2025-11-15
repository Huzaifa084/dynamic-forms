package com.apex.payroll.security;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PublicEndpoints {
    public static final List<String> PUBLIC_ENDPOINTS = List.of(
            "/api/auth/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/webjars/**",
            "/api/public/**",
            "/api/health",
            "/api/debug/**",
            "/mef-sdk/debug/**",
            "/index.html",
            "/styles.css",
            "/form1040.html",
            "/static/**",
            "/favicon.ico"
    );
}