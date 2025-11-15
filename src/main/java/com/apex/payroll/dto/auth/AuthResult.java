package com.apex.payroll.dto.auth;

public sealed interface AuthResult
        permits LoginResponse, PreAuthTokenResponse {}