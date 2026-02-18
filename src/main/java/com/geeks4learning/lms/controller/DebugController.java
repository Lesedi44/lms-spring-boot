package com.geeks4learning.lms.controller;

import com.geeks4learning.lms.model.User;
import com.geeks4learning.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class DebugController {

    private final UserRepository userRepository;

    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/check-role/{username}")
    public String checkUserRole(@PathVariable String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return "User not found: " + username;
        }
        return String.format("User: %s, Role: '%s', Role length: %d, Is employee: %b",
                user.getUsername(),
                user.getRole(),
                user.getRole().length(),
                "EMPLOYEE".equals(user.getRole()));
    }

    @GetMapping("/fix-roles")
    public String fixRoles() {
        List<User> users = userRepository.findAll();
        StringBuilder result = new StringBuilder();
        int fixedCount = 0;

        for (User user : users) {
            String oldRole = user.getRole();
            String newRole = null;

            if ("employee".equalsIgnoreCase(oldRole)) {
                newRole = "EMPLOYEE";
            } else if ("manager".equalsIgnoreCase(oldRole)) {
                newRole = "MANAGER";
            } else if ("admin".equalsIgnoreCase(oldRole)) {
                newRole = "ADMIN";
            }

            if (newRole != null && !newRole.equals(oldRole)) {
                user.setRole(newRole);
                userRepository.save(user);
                result.append(String.format("Fixed %s: %s -> %s\n", user.getUsername(), oldRole, newRole));
                fixedCount++;
            }
        }

        if (fixedCount == 0) {
            result.append("All roles are already correct (uppercase)");
        } else {
            result.append(String.format("\nTotal fixed: %d users", fixedCount));
        }

        return result.toString();
    }
}