package com.apex.payroll.security;

import com.apex.payroll.exception.InvalidTokenException;
import com.apex.payroll.exception.UnauthorizedException;
import com.apex.payroll.exception.UserNotFoundException;
import com.apex.payroll.model.User;
import com.apex.payroll.service.RefreshAllowListService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationProvider {
    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;
    private final RefreshAllowListService refreshAllowListService;

    public void authenticateToken(String jwtToken, HttpServletRequest request) {
        authenticateToken(jwtToken, request, false);
    }

    public void authenticateToken(String jwtToken, HttpServletRequest request, boolean isRefreshToken) {
        var claims   = jwtService.validate(jwtToken, isRefreshToken ? "REFRESH" : "ACCESS");
        Long userId  = jwtService.getUserId(claims);
        Long tokenV  = jwtService.getTokenVersion(claims);

        if (isRefreshToken) {
            String tokenId = jwtService.getTokenId(claims);
            // VALID only when current allow-listed id == tokenId
            if (!refreshAllowListService.isValidRefreshToken(userId, tokenId)) {
                throw new InvalidTokenException("Invalid or revoked refresh token");
            }
        }

        User user = userDetailsService.getUserById(userId);
        if (user == null) throw new UserNotFoundException("User not found");
        if (Boolean.FALSE.equals(user.getIsActive())) throw new UnauthorizedException("User account is disabled");
        if (!tokenV.equals(user.getTokenVersion())) throw new InvalidTokenException("Token has been revoked");

        Authentication existing = SecurityContextHolder.getContext().getAuthentication();
        if (existing != null && existing.getPrincipal() instanceof UserDetailsImpl) return;



    UserDetailsImpl details = new UserDetailsImpl(
        user.getId(),
        user.getEmail(),
        user.getEmail(),
        user.getPasswordHash(),
        Boolean.TRUE.equals(user.getIsActive()),
        Boolean.TRUE.equals(user.getDeleted()),
        user.getTokenVersion()
    );

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());

        if (request != null) {
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        }
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
