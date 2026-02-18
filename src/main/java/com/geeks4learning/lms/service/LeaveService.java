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

    public int calculateWorkingDays(LocalDate startDate, LocalDate endDate) {
        int workingDays = 0;
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            if (current.getDayOfWeek().getValue() < 6) { // Monday=1 to Friday=5
                workingDays++;
            }
            current = current.plusDays(1);
        }
        return workingDays;
    }

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

        // Notify manager
        if (employee.getManagerId() != null) {
            userRepository.findById(employee.getManagerId())
                    .ifPresent(manager -> {
                        emailService.sendEmailNotification(
                                manager.getName(),
                                String.format("Leave Request #%d from %s", savedRequest.getId(), employee.getName()),
                                String.format("%s requested %d days of %s leave (%s to %s)",
                                        employee.getName(), workingDays, dto.getLeaveType(),
                                        dto.getStartDate(), dto.getEndDate())
                        );
                        log.debug("Manager {} notified", manager.getName());
                    });
        }

        log.info("[LEAVE] Request #{} submitted by {} ({} working days)",
                savedRequest.getId(), employee.getName(), workingDays);

        return savedRequest;
    }

    @Transactional
    public LeaveRequest updateLeave(Long managerId, Long requestId, UpdateLeaveDto dto) {
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

        request.setStatus(dto.getStatus());
        request.setComments(dto.getComments());
        request.setUpdatedAt(LocalDateTime.now());
        request.setApprovedBy(manager.getName());

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

        LeaveRequest updatedRequest = leaveRequestRepository.save(request);

        // Notify employee
        userRepository.findById(request.getEmployeeId())
                .ifPresent(employee -> {
                    emailService.sendEmailNotification(
                            employee.getName(),
                            String.format("Leave Request #%d %s", request.getId(), dto.getStatus()),
                            String.format("Your %s leave (%s – %s) has been %s by %s. %s",
                                    request.getLeaveType(), request.getStartDate(), request.getEndDate(),
                                    dto.getStatus().toLowerCase(), manager.getName(),
                                    dto.getComments() != null ? "Note: " + dto.getComments() : "")
                    );
                });

        log.info("[LEAVE] Request #{} {} by {}", request.getId(), dto.getStatus(), manager.getName());

        return updatedRequest;
    }

    public LeaveBalance getLeaveBalance(Long employeeId) {
        return leaveBalanceRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
    }

    public List<LeaveRequest> getAllLeaveRequests(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if ("MANAGER".equals(user.getRole()) || "ADMIN".equals(user.getRole())) {
            return leaveRequestRepository.findAll();
        } else {
            return leaveRequestRepository.findByEmployeeId(userId);
        }
    }

    public List<ReportDto> getLeaveReport(Long managerId) {
        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new RuntimeException("Manager not found"));

        if (!"MANAGER".equals(manager.getRole()) && !"ADMIN".equals(manager.getRole())) {
            throw new RuntimeException("Unauthorized: Manager access required");
        }

        List<User> employees = userRepository.findByRole("EMPLOYEE");
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