package com.geeks4learning.lms.controller;

import com.geeks4learning.lms.dto.LoginDto;
import com.geeks4learning.lms.model.User;
import com.geeks4learning.lms.service.AuthService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginDto loginDto, HttpSession session) {
        log.debug("Login attempt for username: {}", loginDto.getUsername());

        User user = authService.authenticate(loginDto.getUsername(), loginDto.getPassword());

        if (user != null) {
            // Store user info in session
            session.setAttribute("userId", user.getId());
            session.setAttribute("username", user.getUsername());
            session.setAttribute("role", user.getRole());
            session.setAttribute("name", user.getName());

            log.info("User logged in successfully: {} with role: {}", user.getUsername(), user.getRole());

            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "Login successful",
                    "user", Map.of(
                            "id", user.getId(),
                            "name", user.getName(),
                            "username", user.getUsername(),
                            "role", user.getRole(),
                            "department", user.getDepartment()
                    )
            );
            return ResponseEntity.ok(response);
        }

        log.warn("Failed login attempt for username: {}", loginDto.getUsername());
        return ResponseEntity.status(401)
                .body(Map.of("success", false, "message", "Invalid username or password"));
    }

    @GetMapping("/session")
    public ResponseEntity<?> getSession(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");

        if (userId != null) {
            Map<String, Object> userInfo = Map.of(
                    "id", userId,
                    "username", session.getAttribute("username"),
                    "name", session.getAttribute("name"),
                    "role", session.getAttribute("role")
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "authenticated", true,
                    "user", userInfo
            ));
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "authenticated", false
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        String username = (String) session.getAttribute("username");
        session.invalidate();
        log.info("User logged out: {}", username);
        return ResponseEntity.ok(Map.of("success", true, "message", "Logged out successfully"));
    }
}