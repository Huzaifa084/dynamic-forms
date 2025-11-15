package com.apex.payroll.security;

import com.apex.payroll.exception.UserNotFoundException;
import com.apex.payroll.model.User;
import com.apex.payroll.repository.UserRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {
        if (email.isBlank()) {
            throw new UsernameNotFoundException("Email must be provided");
        }
        String normalized = email.trim().toLowerCase();
        log.debug("Loading user by email: {}", normalized);
        User user = userRepository.findByEmail(normalized)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + normalized));
        return UserDetailsImpl.from(user);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + id));
    }
}
