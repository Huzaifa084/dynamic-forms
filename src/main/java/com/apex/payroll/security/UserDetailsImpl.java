package com.apex.payroll.security;

import com.apex.payroll.model.User;
import lombok.Getter;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Getter
public class UserDetailsImpl implements UserDetails, Principal {

    @Serial
    private static final long serialVersionUID = 1L;

    @ToString.Include
    private final Long id;
    @ToString.Include
    private final String email;
    private final String username; // currently same as email
    private final String password; // hashed password
    private final boolean active;
    private final boolean deleted;
    private final Long tokenVersion;

    private static final List<GrantedAuthority> NO_AUTHORITIES = Collections.emptyList();

    public UserDetailsImpl(
            Long id,
            String email,
            String username,
            String password,
            boolean active,
            boolean deleted,
            Long tokenVersion
    ) {
        this.id = id;
        this.email = email;
        this.username = (username != null && !username.isBlank()) ? username : email;
        this.password = password;
        this.active = active;
        this.deleted = deleted;
        this.tokenVersion = tokenVersion == null ? 0L : tokenVersion;
    }

    public UserDetailsImpl(Long id, String email, String password, boolean active, Long tokenVersion) {
        this(id, email, email, password, active, false, tokenVersion);
    }

    public UserDetailsImpl(Long id, String email, String username, Long tokenVersion) {
        this(id, email, username, null, true, false, tokenVersion);
    }

    public static UserDetailsImpl from(User user) {
        if (user == null) return null;
        return new UserDetailsImpl(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                Boolean.TRUE.equals(user.getIsActive()),
                user.getTokenVersion()
        );
    }

    public Long getTokenVersion() {
        return tokenVersion;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return NO_AUTHORITIES;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getName() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active && !deleted;
    }
}
