package com.geeks4learning.lms.service;

import com.geeks4learning.lms.model.User;
import com.geeks4learning.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;

    public User authenticate(String username, String password) {
        log.debug("Attempting to authenticate user: {}", username);

        User user = userRepository.findByUsername(username).orElse(null);

        if (user == null) {
            log.warn("User not found: {}", username);
            return null;
        }

        log.debug("Found user: {}, role: {}", user.getUsername(), user.getRole());

        // Simple password check (in production, use BCrypt)
        if (password.equals(user.getPassword())) {
            log.info("User authenticated: {} ({})", user.getUsername(), user.getRole());
            return user;
        }

        log.warn("Invalid password for user: {}", username);
        return null;
    }

    public boolean isAdmin(User user) {
        if (user == null) return false;
        boolean result = "ADMIN".equals(user.getRole());
        log.debug("isAdmin check for {}: {}", user.getUsername(), result);
        return result;
    }

    public boolean isManager(User user) {
        if (user == null) return false;
        boolean result = "MANAGER".equals(user.getRole()) || "ADMIN".equals(user.getRole());
        log.debug("isManager check for {}: {}", user.getUsername(), result);
        return result;
    }

    public boolean isEmployee(User user) {
        if (user == null) return false;
        boolean result = "EMPLOYEE".equals(user.getRole());
        log.debug("isEmployee check for {}: {}", user.getUsername(), result);
        return result;
    }

    public boolean hasAccess(User user, String requiredRole) {
        if (user == null) return false;

        switch (requiredRole.toUpperCase()) {
            case "ADMIN":
                return isAdmin(user);
            case "MANAGER":
                return isManager(user);
            case "EMPLOYEE":
                return isEmployee(user);
            default:
                return false;
        }
    }
}