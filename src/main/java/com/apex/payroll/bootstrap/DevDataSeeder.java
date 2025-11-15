package com.apex.payroll.bootstrap;

import com.apex.payroll.enums.AuthProviderType;
import com.apex.payroll.enums.Role;
import com.apex.payroll.model.User;
import com.apex.payroll.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DevDataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        long userCount = userRepository.count();
        if (userCount == 0) {
            // Create a default developer user
            User user = User.builder()
                    .publicId(UUID.randomUUID())
                    .email("devuser@yopmail.com")
                    // For dev environments, passwordHash can be any placeholder
                    .passwordHash(passwordEncoder.encode("pass123"))
                    .firstName("Dev")
                    .lastName("User")
                    .username("devuser")
                    .authProviderType(AuthProviderType.LOCAL)
                    .role(Role.ADMIN)
                    .isActive(true)
                    .deleted(false)
                    .build();

            user = userRepository.save(user);
            log.info("Seeded default dev user with id={} email={}", user.getId(), user.getEmail());

            // Put into SecurityContext for immediate dev use
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
            var authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.info("Set SecurityContext authentication for dev user id={}", user.getId());
        } else {
            // If users exist, set the first one as the current authentication for dev convenience
            userRepository.findAll().stream().findFirst().ifPresent(existing -> {
                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + existing.getRole().name()));
                var authentication = new UsernamePasswordAuthenticationToken(existing, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("SecurityContext initialized with existing user id={} email={}", existing.getId(), existing.getEmail());
            });
        }
    }
}
