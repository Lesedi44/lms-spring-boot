package com.geeks4learning.lms.service;

import com.geeks4learning.lms.dto.LeaveRequestDto;
import com.geeks4learning.lms.dto.UpdateLeaveDto;
import com.geeks4learning.lms.model.LeaveBalance;
import com.geeks4learning.lms.model.LeaveRequest;
import com.geeks4learning.lms.model.User;
import com.geeks4learning.lms.repository.LeaveBalanceRepository;
import com.geeks4learning.lms.repository.LeaveRequestRepository;
import com.geeks4learning.lms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LeaveServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LeaveBalanceRepository leaveBalanceRepository;

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private LeaveService leaveService;

    @Captor
    private ArgumentCaptor<LeaveRequest> leaveRequestCaptor;

    private User employee;
    private User manager;
    private LeaveBalance balance;
    private LeaveRequestDto requestDto;
    private LeaveRequest pendingRequest;

    @BeforeEach
    void setUp() {
        // Setup employee
        employee = new User();
        employee.setId(1L);
        employee.setUsername("john");
        employee.setName("John Doe");
        employee.setRole("EMPLOYEE");
        employee.setDepartment("Engineering");
        employee.setManagerId(3L);

        // Setup manager
        manager = new User();
        manager.setId(3L);
        manager.setUsername("mike");
        manager.setName("Mike Adams");
        manager.setRole("MANAGER");
        manager.setDepartment("Engineering");

        // Setup leave balance
        balance = new LeaveBalance();
        balance.setId(1L);
        balance.setEmployeeId(1L);
        balance.setAnnualLeave(15);
        balance.setSickLeave(10);
        balance.setFamilyLeave(5);

        // Setup request DTO
        requestDto = new LeaveRequestDto();
        requestDto.setStartDate(LocalDate.of(2026, 3, 2)); // Monday
        requestDto.setEndDate(LocalDate.of(2026, 3, 6));   // Friday
        requestDto.setLeaveType("Annual");
        requestDto.setReason("Family vacation");

        // Setup pending request
        pendingRequest = new LeaveRequest();
        pendingRequest.setId(100L);
        pendingRequest.setEmployeeId(1L);
        pendingRequest.setEmployeeName("John Doe");
        pendingRequest.setStartDate(LocalDate.of(2026, 3, 2));
        pendingRequest.setEndDate(LocalDate.of(2026, 3, 6));
        pendingRequest.setLeaveType("Annual");
        pendingRequest.setWorkingDays(5);
        pendingRequest.setStatus("Pending");
        pendingRequest.setSubmittedAt(LocalDateTime.now());
    }

    // ========== TEST 1: Submit Leave - Success ==========
    @Test
    @DisplayName("Submit Leave - Success")
    void testSubmitLeave_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveBalanceRepository.findByEmployeeId(1L)).thenReturn(Optional.of(balance));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(invocation -> {
            LeaveRequest saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        // Act
        LeaveRequest result = leaveService.submitLeave(1L, requestDto);

        // Assert
        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals("John Doe", result.getEmployeeName());
        assertEquals(5, result.getWorkingDays());
        assertEquals("Pending", result.getStatus());

        verify(leaveRequestRepository, times(1)).save(any(LeaveRequest.class));
        verify(emailService, times(1)).sendEmailNotification(anyString(), anyString(), anyString());
    }

    // ========== TEST 2: Submit Leave - Insufficient Balance ==========
    @Test
    @DisplayName("Submit Leave - Insufficient Balance")
    void testSubmitLeave_InsufficientBalance() {
        // Arrange
        balance.setAnnualLeave(3); // Only 3 days left
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveBalanceRepository.findByEmployeeId(1L)).thenReturn(Optional.of(balance));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            leaveService.submitLeave(1L, requestDto);
        });

        assertTrue(exception.getMessage().contains("Insufficient Annual leave balance"));
        verify(leaveRequestRepository, never()).save(any(LeaveRequest.class));
    }

    // ========== TEST 3: Submit Leave - User Not Found ==========
    @Test
    @DisplayName("Submit Leave - User Not Found")
    void testSubmitLeave_UserNotFound() {
        // Arrange
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            leaveService.submitLeave(99L, requestDto);
        });

        assertEquals("Employee not found", exception.getMessage());
    }

    // ========== TEST 4: Submit Leave - Wrong Role ==========
    @Test
    @DisplayName("Submit Leave - Wrong Role (Manager trying to submit)")
    void testSubmitLeave_WrongRole() {
        // Arrange
        employee.setRole("MANAGER");
        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            leaveService.submitLeave(1L, requestDto);
        });

        assertTrue(exception.getMessage().contains("Employee access required"));
    }

    // ========== TEST 5: Submit Leave - Invalid Dates ==========
    @Test
    @DisplayName("Submit Leave - End Date Before Start Date")
    void testSubmitLeave_InvalidDates() {
        // Arrange
        requestDto.setStartDate(LocalDate.of(2026, 3, 10));
        requestDto.setEndDate(LocalDate.of(2026, 3, 5));

        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveBalanceRepository.findByEmployeeId(1L)).thenReturn(Optional.of(balance));

        // Act
        LeaveRequest result = leaveService.submitLeave(1L, requestDto);

        // Assert - Should calculate 0 or negative days
        assertTrue(result.getWorkingDays() <= 0);
    }

    // ========== TEST 6: Approve Leave - Success ==========
    @Test
    @DisplayName("Approve Leave - Success")
    void testApproveLeave_Success() {
        // Arrange
        UpdateLeaveDto updateDto = new UpdateLeaveDto();
        updateDto.setStatus("Approved");
        updateDto.setComments("Approved by manager");

        when(userRepository.findById(3L)).thenReturn(Optional.of(manager));
        when(leaveRequestRepository.findById(100L)).thenReturn(Optional.of(pendingRequest));
        when(leaveBalanceRepository.findByEmployeeId(1L)).thenReturn(Optional.of(balance));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(pendingRequest);

        int originalBalance = balance.getAnnualLeave();

        // Act
        LeaveRequest result = leaveService.updateLeave(3L, 100L, updateDto);

        // Assert
        assertEquals("Approved", result.getStatus());
        assertEquals("Mike Adams", result.getApprovedBy());

        // Verify balance was deducted
        ArgumentCaptor<LeaveBalance> balanceCaptor = ArgumentCaptor.forClass(LeaveBalance.class);
        verify(leaveBalanceRepository).save(balanceCaptor.capture());

        LeaveBalance updatedBalance = balanceCaptor.getValue();
        assertEquals(originalBalance - 5, updatedBalance.getAnnualLeave());
    }

    // ========== TEST 7: Reject Leave - Success ==========
    @Test
    @DisplayName("Reject Leave - Success")
    void testRejectLeave_Success() {
        // Arrange
        UpdateLeaveDto updateDto = new UpdateLeaveDto();
        updateDto.setStatus("Rejected");
        updateDto.setComments("Not approved");

        when(userRepository.findById(3L)).thenReturn(Optional.of(manager));
        when(leaveRequestRepository.findById(100L)).thenReturn(Optional.of(pendingRequest));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenReturn(pendingRequest);

        // Act
        LeaveRequest result = leaveService.updateLeave(3L, 100L, updateDto);

        // Assert
        assertEquals("Rejected", result.getStatus());
        assertEquals("Mike Adams", result.getApprovedBy());

        // Verify balance was NOT deducted
        verify(leaveBalanceRepository, never()).save(any(LeaveBalance.class));
    }

    // ========== TEST 8: Approve Leave - Already Processed ==========
    @Test
    @DisplayName("Approve Leave - Already Processed")
    void testApproveLeave_AlreadyProcessed() {
        // Arrange
        pendingRequest.setStatus("Approved");
        UpdateLeaveDto updateDto = new UpdateLeaveDto();
        updateDto.setStatus("Rejected");

        when(userRepository.findById(3L)).thenReturn(Optional.of(manager));
        when(leaveRequestRepository.findById(100L)).thenReturn(Optional.of(pendingRequest));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            leaveService.updateLeave(3L, 100L, updateDto);
        });

        assertTrue(exception.getMessage().contains("already Approved"));
    }

    // ========== TEST 9: Get Leave Balance ==========
    @Test
    @DisplayName("Get Leave Balance - Success")
    void testGetLeaveBalance() {
        // Arrange
        when(leaveBalanceRepository.findByEmployeeId(1L)).thenReturn(Optional.of(balance));

        // Act
        LeaveBalance result = leaveService.getLeaveBalance(1L);

        // Assert
        assertNotNull(result);
        assertEquals(15, result.getAnnualLeave());
        assertEquals(10, result.getSickLeave());
        assertEquals(5, result.getFamilyLeave());
    }

    // ========== TEST 10: Get All Leave Requests - Employee ==========
    @Test
    @DisplayName("Get All Leave Requests - Employee sees only own requests")
    void testGetAllLeaveRequests_Employee() {
        // Arrange
        List<LeaveRequest> employeeRequests = Arrays.asList(pendingRequest);

        when(userRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(leaveRequestRepository.findByEmployeeId(1L)).thenReturn(employeeRequests);

        // Act
        List<LeaveRequest> results = leaveService.getAllLeaveRequests(1L);

        // Assert
        assertEquals(1, results.size());
        assertEquals(100L, results.get(0).getId());
        verify(leaveRequestRepository, never()).findAll();
    }

    // ========== TEST 11: Get All Leave Requests - Manager ==========
    @Test
    @DisplayName("Get All Leave Requests - Manager sees all requests")
    void testGetAllLeaveRequests_Manager() {
        // Arrange
        List<LeaveRequest> allRequests = Arrays.asList(pendingRequest);

        when(userRepository.findById(3L)).thenReturn(Optional.of(manager));
        when(leaveRequestRepository.findAll()).thenReturn(allRequests);

        // Act
        List<LeaveRequest> results = leaveService.getAllLeaveRequests(3L);

        // Assert
        assertEquals(1, results.size());
        verify(leaveRequestRepository, times(1)).findAll();
    }
}