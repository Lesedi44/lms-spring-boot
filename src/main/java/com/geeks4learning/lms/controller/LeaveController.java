package com.geeks4learning.lms.controller;

import com.geeks4learning.lms.dto.LeaveRequestDto;
import com.geeks4learning.lms.dto.UpdateLeaveDto;
import com.geeks4learning.lms.model.LeaveBalance;
import com.geeks4learning.lms.model.LeaveRequest;
import com.geeks4learning.lms.model.User;
import com.geeks4learning.lms.repository.UserRepository;
import com.geeks4learning.lms.service.AuthService;
import com.geeks4learning.lms.service.LeaveService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@RequiredArgsConstructor
@Slf4j
public class LeaveController {

    private final LeaveService leaveService;
    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping("/leave")
    public ResponseEntity<?> submitLeave(
            HttpSession session,
            @Valid @RequestBody LeaveRequestDto requestDto) {

        Long userId = (Long) session.getAttribute("userId");
        String username = (String) session.getAttribute("username");

        log.debug("=== LEAVE CONTROLLER SUBMIT ===");
        log.debug("Submit leave attempt - User ID: {}, Username: {}", userId, username);

        if (userId == null) {
            log.warn("Submit leave failed: No user in session");
            return ResponseEntity.status(401)
                    .body(Map.of("success", false, "message", "Please login first"));
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("Submit leave failed: User not found in database - ID: {}", userId);
            return ResponseEntity.status(401)
                    .body(Map.of("success", false, "message", "User not found"));
        }

        log.debug("User from database - Username: {}, Role: {}", user.getUsername(), user.getRole());

        if (!"EMPLOYEE".equals(user.getRole())) {
            log.warn("Submit leave failed: User {} is not an employee. Role: {}", username, user.getRole());
            return ResponseEntity.status(403)
                    .body(Map.of(
                            "success", false,
                            "message", "Employee access required. Your role is: " + user.getRole()
                    ));
        }

        try {
            LeaveRequest request = leaveService.submitLeave(userId, requestDto);
            log.info("Leave request submitted successfully for user: {}", username);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Leave request submitted successfully");
            response.put("requestId", request.getId());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error submitting leave request for user {}: {}", username, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/leave/{id}")
    public ResponseEntity<?> updateLeave(
            HttpSession session,
            @PathVariable Long id,
            @Valid @RequestBody UpdateLeaveDto updateDto) {

        Long userId = (Long) session.getAttribute("userId");
        String username = (String) session.getAttribute("username");

        log.debug("Update leave attempt - User ID: {}, Request ID: {}, Status: {}", userId, id, updateDto.getStatus());

        if (userId == null) {
            log.warn("Update leave failed: No user in session");
            return ResponseEntity.status(401)
                    .body(Map.of("success", false, "message", "Please login first"));
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("Update leave failed: User not found in database - ID: {}", userId);
            return ResponseEntity.status(401)
                    .body(Map.of("success", false, "message", "User not found"));
        }

        if (!"MANAGER".equals(user.getRole()) && !"ADMIN".equals(user.getRole())) {
            log.warn("Update leave failed: User {} is not a manager. Role: {}", username, user.getRole());
            return ResponseEntity.status(403)
                    .body(Map.of(
                            "success", false,
                            "message", "Manager access required. Your role is: " + user.getRole()
                    ));
        }

        try {
            LeaveRequest request = leaveService.updateLeave(userId, id, updateDto);
            log.info("Leave request {} updated successfully by user: {}", id, username);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Leave request " + updateDto.getStatus().toLowerCase() + " successfully",
                    "requestId", request.getId(),
                    "status", request.getStatus()
            ));

        } catch (Exception e) {
            log.error("Error updating leave request {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/leave/balance")
    public ResponseEntity<?> getMyLeaveBalance(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        String username = (String) session.getAttribute("username");

        log.debug("Get balance attempt - User ID: {}, Username: {}", userId, username);

        if (userId == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("success", false, "message", "Please login first"));
        }

        try {
            LeaveBalance balance = leaveService.getLeaveBalance(userId);
            log.debug("Balance retrieved for user: {}", username);

            Map<String, Object> balanceMap = new HashMap<>();
            balanceMap.put("annual", balance.getAnnualLeave());
            balanceMap.put("sick", balance.getSickLeave());
            balanceMap.put("family", balance.getFamilyLeave());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "balance", balanceMap
            ));
        } catch (Exception e) {
            log.error("Error getting balance for user {}: {}", username, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/leave/balance/{employeeId}")
    public ResponseEntity<?> getLeaveBalance(
            HttpSession session,
            @PathVariable Long employeeId) {

        Long userId = (Long) session.getAttribute("userId");
        String role = (String) session.getAttribute("role");

        log.debug("Get balance for employee {} - Requested by user: {}, Role: {}", employeeId, userId, role);

        if (userId == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("success", false, "message", "Please login first"));
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("success", false, "message", "User not found"));
        }

        if (!"MANAGER".equals(user.getRole()) && !"ADMIN".equals(user.getRole())) {
            return ResponseEntity.status(403)
                    .body(Map.of("success", false, "message", "Manager access required"));
        }

        try {
            LeaveBalance balance = leaveService.getLeaveBalance(employeeId);

            User employee = userRepository.findById(employeeId).orElse(null);
            String employeeName = employee != null ? employee.getName() : "Unknown";

            Map<String, Object> balanceMap = new HashMap<>();
            balanceMap.put("annual", balance.getAnnualLeave());
            balanceMap.put("sick", balance.getSickLeave());
            balanceMap.put("family", balance.getFamilyLeave());

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "employeeId", employeeId,
                    "employeeName", employeeName,
                    "balance", balanceMap
            ));
        } catch (Exception e) {
            log.error("Error getting balance for employee {}: {}", employeeId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/leave")
    public ResponseEntity<?> getAllLeave(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        String username = (String) session.getAttribute("username");
        String role = (String) session.getAttribute("role");

        log.debug("Get all leave requests - User: {}, Role: {}", username, role);

        if (userId == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("success", false, "message", "Please login first"));
        }

        try {
            List<LeaveRequest> requests = leaveService.getAllLeaveRequests(userId);
            log.debug("Retrieved {} leave requests for user: {}", requests.size(), username);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "count", requests.size(),
                    "requests", requests
            ));
        } catch (Exception e) {
            log.error("Error getting leave requests for user {}: {}", username, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/reports/leave")
    public ResponseEntity<?> getLeaveReport(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        String username = (String) session.getAttribute("username");

        log.debug("Get leave report attempt - User: {}", username);

        if (userId == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("success", false, "message", "Please login first"));
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("success", false, "message", "User not found"));
        }

        if (!"MANAGER".equals(user.getRole()) && !"ADMIN".equals(user.getRole())) {
            log.warn("Get report failed: User {} is not a manager", username);
            return ResponseEntity.status(403)
                    .body(Map.of("success", false, "message", "Manager access required"));
        }

        try {
            var report = leaveService.getLeaveReport(userId);
            log.info("Leave report generated for manager: {}", username);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "generatedAt", java.time.LocalDateTime.now().toString(),
                    "report", report
            ));
        } catch (Exception e) {
            log.error("Error generating report for user {}: {}", username, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/debug/session")
    public ResponseEntity<?> debugSession(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");

        Map<String, Object> sessionInfo = new HashMap<>();
        sessionInfo.put("sessionId", session.getId());
        sessionInfo.put("userId", userId);
        sessionInfo.put("username", session.getAttribute("username"));
        sessionInfo.put("role", session.getAttribute("role"));
        sessionInfo.put("name", session.getAttribute("name"));

        if (userId != null) {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                sessionInfo.put("database_role", user.getRole());
                sessionInfo.put("isEmployee", "EMPLOYEE".equals(user.getRole()));
                sessionInfo.put("isManager", "MANAGER".equals(user.getRole()) || "ADMIN".equals(user.getRole()));
                sessionInfo.put("isAdmin", "ADMIN".equals(user.getRole()));
            }
        }

        return ResponseEntity.ok(sessionInfo);
    }

    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Leave Management System",
                "timestamp", java.time.LocalDateTime.now().toString()
        ));
    }
}