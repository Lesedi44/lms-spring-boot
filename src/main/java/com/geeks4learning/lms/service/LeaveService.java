package com.geeks4learning.lms.service;

import com.geeks4learning.lms.dto.LeaveRequestDto;
import com.geeks4learning.lms.dto.ReportDto;
import com.geeks4learning.lms.dto.UpdateLeaveDto;
import com.geeks4learning.lms.model.LeaveBalance;
import com.geeks4learning.lms.model.LeaveRequest;
import com.geeks4learning.lms.model.User;
import com.geeks4learning.lms.repository.LeaveBalanceRepository;
import com.geeks4learning.lms.repository.LeaveRequestRepository;
import com.geeks4learning.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaveService {

    private final UserRepository userRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmailService emailService;

    /**
     * Calculate working days between two dates (excluding weekends)
     * @param startDate Start date
     * @param endDate End date
     * @return Number of working days
     */
    public int calculateWorkingDays(LocalDate startDate, LocalDate endDate) {
        int workingDays = 0;
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            // Monday=1 to Friday=5 (weekend = 6,7)
            if (current.getDayOfWeek().getValue() < 6) {
                workingDays++;
            }
            current = current.plusDays(1);
        }
        return workingDays;
    }

    /**
     * Submit a new leave request (Employee only)
     * @param userId Employee ID
     * @param dto Leave request data
     * @return Created leave request
     */
    @Transactional
    public LeaveRequest submitLeave(Long userId, LeaveRequestDto dto) {
        log.debug("=== LEAVE SERVICE SUBMIT ===");
        log.debug("Submitting leave for userId: {}", userId);

        User employee = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("Employee not found with id: {}", userId);
                    return new RuntimeException("Employee not found");
                });

        log.debug("Found employee: {}, role: {}", employee.getUsername(), employee.getRole());

        // Check for EMPLOYEE role (uppercase)
        if (!"EMPLOYEE".equals(employee.getRole())) {
            log.error("User {} has role {} but EMPLOYEE required", employee.getUsername(), employee.getRole());
            throw new RuntimeException("Unauthorized: Employee access required. Your role is: " + employee.getRole());
        }

        // Validate dates
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new RuntimeException("End date cannot be before start date");
        }

        int workingDays = calculateWorkingDays(dto.getStartDate(), dto.getEndDate());
        log.debug("Working days calculated: {}", workingDays);

        // Check leave balance
        LeaveBalance balance = leaveBalanceRepository.findByEmployeeId(userId)
                .orElseThrow(() -> {
                    log.error("Leave balance not found for employee: {}", userId);
                    return new RuntimeException("Leave balance not found");
                });

        int availableBalance = balance.getBalance(dto.getLeaveType());
        log.debug("Available balance for {}: {}", dto.getLeaveType(), availableBalance);

        if (availableBalance < workingDays) {
            throw new RuntimeException(String.format(
                    "Insufficient %s leave balance. Available: %d days, Requested: %d days",
                    dto.getLeaveType(), availableBalance, workingDays));
        }

        // Create leave request
        LeaveRequest request = new LeaveRequest();
        request.setEmployeeId(userId);
        request.setEmployeeName(employee.getName());
        request.setManagerId(employee.getManagerId());
        request.setStartDate(dto.getStartDate());
        request.setEndDate(dto.getEndDate());
        request.setLeaveType(dto.getLeaveType());
        request.setReason(dto.getReason());
        request.setWorkingDays(workingDays);
        request.setSubmittedAt(LocalDateTime.now());
        request.setStatus("Pending");

        LeaveRequest savedRequest = leaveRequestRepository.save(request);
        log.info("Leave request saved with id: {}", savedRequest.getId());

        // Notify manager via email
        if (employee.getManagerId() != null) {
            userRepository.findById(employee.getManagerId())
                    .ifPresent(manager -> {
                        String subject = String.format("🔔 New Leave Request #%d from %s",
                                savedRequest.getId(), employee.getName());
                        String body = String.format(
                                "%s has requested %d days of %s leave from %s to %s.\n\n" +
                                        "Reason: %s\n\n" +
                                        "Please log in to the Leave Management System to review and approve/reject this request.",
                                employee.getName(),
                                workingDays,
                                dto.getLeaveType(),
                                dto.getStartDate(),
                                dto.getEndDate(),
                                dto.getReason()
                        );

                        emailService.sendEmailNotification(manager.getEmail(), subject, body);
                        log.debug("Manager {} notified via email", manager.getName());
                    });
        }

        log.info("[LEAVE] Request #{} submitted by {} ({} working days)",
                savedRequest.getId(), employee.getName(), workingDays);

        return savedRequest;
    }

    /**
     * Approve or reject a leave request (Manager/Admin only)
     * @param managerId Manager/Admin ID
     * @param requestId Leave request ID
     * @param dto Update data (status, comments)
     * @return Updated leave request
     */
    @Transactional
    public LeaveRequest updateLeave(Long managerId, Long requestId, UpdateLeaveDto dto) {
        log.debug("=== LEAVE SERVICE UPDATE ===");
        log.debug("Updating leave request: {} by manager: {}", requestId, managerId);

        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new RuntimeException("Manager not found"));

        if (!"MANAGER".equals(manager.getRole()) && !"ADMIN".equals(manager.getRole())) {
            throw new RuntimeException("Unauthorized: Manager access required");
        }

        LeaveRequest request = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Leave request not found"));

        if (!"Pending".equals(request.getStatus())) {
            throw new RuntimeException("Request already " + request.getStatus());
        }

        // Update request
        request.setStatus(dto.getStatus());
        request.setComments(dto.getComments());
        request.setUpdatedAt(LocalDateTime.now());
        request.setApprovedBy(manager.getName());

        LeaveRequest updatedRequest = leaveRequestRepository.save(request);

        // Get employee details for email
        User employee = userRepository.findById(request.getEmployeeId()).orElse(null);

        // Deduct balance if approved
        if ("Approved".equals(dto.getStatus())) {
            LeaveBalance balance = leaveBalanceRepository.findByEmployeeId(request.getEmployeeId())
                    .orElseThrow(() -> new RuntimeException("Leave balance not found"));

            balance.deductBalance(request.getLeaveType(), request.getWorkingDays());
            leaveBalanceRepository.save(balance);

            log.info("[BALANCE] {} new {} balance: {} days",
                    request.getEmployeeName(), request.getLeaveType(),
                    balance.getBalance(request.getLeaveType()));
        }

        // Send email notification to employee
        if (employee != null) {
            sendLeaveStatusEmail(employee, request, manager, dto.getComments());
        }

        log.info("[LEAVE] Request #{} {} by {}", request.getId(), dto.getStatus(), manager.getName());

        return updatedRequest;
    }

    /**
     * Send email notification about leave request status change
     * @param employee Employee user
     * @param request Leave request
     * @param manager Manager who updated
     * @param comments Optional comments
     */
    private void sendLeaveStatusEmail(User employee, LeaveRequest request, User manager, String comments) {
        String subject = String.format("📋 Leave Request #%d %s", request.getId(), request.getStatus());

        String statusEmoji = "Approved".equals(request.getStatus()) ? "✅" : "❌";
        String statusText = "Approved".equals(request.getStatus()) ? "APPROVED" : "REJECTED";

        StringBuilder body = new StringBuilder();
        body.append(String.format("%s Your leave request has been %s!\n\n", statusEmoji, statusText));
        body.append("=====================================\n");
        body.append(String.format("Request ID:     #%d\n", request.getId()));
        body.append(String.format("Employee:       %s\n", employee.getName()));
        body.append(String.format("Leave Type:     %s\n", request.getLeaveType()));
        body.append(String.format("Start Date:     %s\n", request.getStartDate()));
        body.append(String.format("End Date:       %s\n", request.getEndDate()));
        body.append(String.format("Working Days:   %d\n", request.getWorkingDays()));
        body.append(String.format("Reason:         %s\n", request.getReason()));
        body.append("=====================================\n");
        body.append(String.format("Reviewed by:    %s\n", manager.getName()));

        if (comments != null && !comments.trim().isEmpty()) {
            body.append(String.format("Comments:       \"%s\"\n", comments));
        }

        body.append("\nYou can view all your leave requests by logging into the Leave Management System.\n");
        body.append("\nThis is an automated message. Please do not reply to this email.");

        emailService.sendEmailNotification(employee.getEmail(), subject, body.toString());
        log.debug("Status email sent to employee: {}", employee.getEmail());
    }

    /**
     * Get leave balance for an employee
     * @param employeeId Employee ID
     * @return Leave balance
     */
    public LeaveBalance getLeaveBalance(Long employeeId) {
        return leaveBalanceRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
    }

    /**
     * Get all leave requests based on user role
     * @param userId User ID
     * @return List of leave requests
     */
    public List<LeaveRequest> getAllLeaveRequests(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if ("MANAGER".equals(user.getRole())) {
            // Get employees under this manager
            List<User> teamMembers = userRepository.findByManagerId(userId);
            List<Long> teamMemberIds = teamMembers.stream()
                    .map(User::getId)
                    .collect(Collectors.toList());

            // Include the manager's own requests too
            teamMemberIds.add(userId);

            return leaveRequestRepository.findByEmployeeIdIn(teamMemberIds);
        } else if ("ADMIN".equals(user.getRole())) {
            return leaveRequestRepository.findAll();
        } else {
            return leaveRequestRepository.findByEmployeeId(userId);
        }
    }

    /**
     * Generate leave report for manager
     * @param managerId Manager ID
     * @return List of report DTOs
     */
    public List<ReportDto> getLeaveReport(Long managerId) {
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new RuntimeException("Manager not found"));

        if (!"MANAGER".equals(manager.getRole()) && !"ADMIN".equals(manager.getRole())) {
            throw new RuntimeException("Unauthorized: Manager access required");
        }

        List<User> employees;
        if ("ADMIN".equals(manager.getRole())) {
            employees = userRepository.findByRole("EMPLOYEE");
        } else {
            employees = userRepository.findByManagerId(managerId);
        }

        List<ReportDto> report = new ArrayList<>();

        for (User emp : employees) {
            List<LeaveRequest> empRequests = leaveRequestRepository.findByEmployeeId(emp.getId());
            List<LeaveRequest> approved = empRequests.stream()
                    .filter(r -> "Approved".equals(r.getStatus()))
                    .collect(Collectors.toList());

            int totalDays = approved.stream()
                    .mapToInt(LeaveRequest::getWorkingDays)
                    .sum();

            long pendingCount = empRequests.stream()
                    .filter(r -> "Pending".equals(r.getStatus()))
                    .count();

            LeaveBalance balance = leaveBalanceRepository.findByEmployeeId(emp.getId()).orElse(null);

            Map<String, Integer> balanceMap = new HashMap<>();
            if (balance != null) {
                balanceMap.put("Annual", balance.getAnnualLeave());
                balanceMap.put("Sick", balance.getSickLeave());
                balanceMap.put("Family", balance.getFamilyLeave());
            }

            report.add(new ReportDto(
                    emp.getName(),
                    emp.getDepartment(),
                    (long) approved.size(),
                    totalDays,
                    balanceMap,
                    pendingCount
            ));
        }

        return report;
    }
}