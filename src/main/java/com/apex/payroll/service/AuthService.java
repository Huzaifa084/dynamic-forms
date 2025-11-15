package com.apex.payroll.service;

import com.apex.payroll.dto.auth.LoginRequest;
import com.apex.payroll.dto.auth.LoginResponse;
import com.apex.payroll.exception.InvalidTokenException;
import com.apex.payroll.exception.UserNotFoundException;
import com.apex.payroll.model.User;
import com.apex.payroll.repository.UserRepository;
import com.apex.payroll.security.JwtConfig;
import com.apex.payroll.security.JwtService;
import com.apex.payroll.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final JwtConfig jwtConfig;
    private final RefreshAllowListService refreshAllowListService;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest req) {
        log.info("Login attempt: email: {}", req.getEmail());
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        UserDetailsImpl principal = (UserDetailsImpl) auth.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new UserNotFoundException("User not found."));
        ensureActive(user);

        String accessToken = jwtService.buildToken(JwtConfig.TOKEN_TYPE_ACCESS, jwtConfig.getAccessTtlMillis());
        String refreshToken = jwtService.buildToken(JwtConfig.TOKEN_TYPE_REFRESH, jwtConfig.getRefreshTtlMillis());

        String refreshId = jwtService.getTokenId(jwtService.validate(refreshToken, JwtConfig.TOKEN_TYPE_REFRESH));

        refreshAllowListService.storeRefreshTokenId(user.getId(), refreshId);

        return buildLoginResponse(user, accessToken, refreshToken);
    }

    public void logout() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl details) {
            refreshAllowListService.revokeRefreshToken(details.getId());
            userRepository.incrementTokenVersion(details.getId());
        }
        SecurityContextHolder.clearContext();
        log.info("User logged out");
    }

    @Transactional(readOnly = true)
    public LoginResponse refresh(String refreshToken) {
        var claims = jwtService.validate(refreshToken, JwtConfig.TOKEN_TYPE_REFRESH);
        Long userId = jwtService.getUserId(claims);
        Long tokenVer = jwtService.getTokenVersion(claims);
        String tokenId = jwtService.getTokenId(claims);

        // VALID only when tokenId == stored allow-listed id:
        if (!refreshAllowListService.isValidRefreshToken(userId, tokenId)) {
            throw new InvalidTokenException("Invalid or revoked refresh token");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found."));
        if (!tokenVer.equals(user.getTokenVersion())) {
            throw new InvalidTokenException("Token has been revoked");
        }


        UserDetailsImpl details = new UserDetailsImpl(
                user.getId(), user.getEmail(), user.getEmail(), user.getTokenVersion()
        );
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities())
        );

        String newAccess = jwtService.buildToken(JwtConfig.TOKEN_TYPE_ACCESS, jwtConfig.getAccessTtlMillis());
        String newRefresh = jwtService.buildToken(JwtConfig.TOKEN_TYPE_REFRESH, jwtConfig.getRefreshTtlMillis());

        String newRefreshId = jwtService.getTokenId(jwtService.validate(newRefresh, JwtConfig.TOKEN_TYPE_REFRESH));
        refreshAllowListService.rotateRefreshToken(userId, newRefreshId);

        return buildLoginResponse(user, newAccess, newRefresh);
    }

    private LoginResponse buildLoginResponse(User user, String accessToken, String refreshToken) {

        return LoginResponse.builder()
                .userId(user.getPublicId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .accessToken(accessToken)
                .accessTokenExpiresIn((jwtConfig.getAccessTtlMillis() / 1000) + " seconds")
                .refreshToken(refreshToken)
                .refreshTokenExpiresIn((jwtConfig.getRefreshTtlMillis() / 1000) + " seconds")
                .build();
    }

    private void ensureActive(User user) {
        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new UserNotFoundException("User not found.");
        }
    }
}
