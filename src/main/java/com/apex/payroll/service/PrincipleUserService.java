package com.apex.payroll.service;

import com.apex.payroll.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class PrincipleUserService {
    @PersistenceContext
    private EntityManager entityManager;

    public Optional<User> getCurrentUser() {
//        Long userId = getCurrentUserId();
        // TODO: Replace with actual user ID retrieval logic
        Long userId = 1L;
        if (userId == null) return Optional.empty();
        // For auditing, return a lightweight reference to avoid sharing persistent collections across sessions
        try {
            User ref = entityManager.getReference(User.class, userId);
            return Optional.of(ref);
        } catch (Exception e) {
            log.warn("Failed to load current user with ID {}: {}", userId, e.getMessage());
            return Optional.empty();
        }
    }

//    public Long getCurrentUserId() {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        if (authentication == null || !authentication.isAuthenticated()) {
//            log.debug("No authenticated user found for auditing");
//            return null;
//        }
//
//        Object principal = authentication.getPrincipal();
//
//        if (principal instanceof UserDetailsImpl userDetails) {
//            log.debug("Found logged user for userPId: {}, username: {}", userDetails.getId(), userDetails.getUsername());
//            return userDetails.getId();
//        }
//        return null;
//    }

}
