package com.geeks4learning.lms.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.geeks4learning.lms.dto.LeaveRequestDto;
import com.geeks4learning.lms.dto.LoginDto;
import com.geeks4learning.lms.model.LeaveRequest;
import com.geeks4learning.lms.model.User;
import com.geeks4learning.lms.repository.LeaveRequestRepository;
import com.geeks4learning.lms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class LeaveControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    private MockHttpSession employeeSession;
    private MockHttpSession managerSession;
    private LeaveRequestDto validRequest;

    @BeforeEach
    void setUp() throws Exception {
        // Create valid leave request
        validRequest = new LeaveRequestDto();
        validRequest.setStartDate(LocalDate.of(2026, 3, 2));
        validRequest.setEndDate(LocalDate.of(2026, 3, 6));
        validRequest.setLeaveType("Annual");
        validRequest.setReason("Integration test vacation");

        // Create employee session by actually logging in
        employeeSession = loginAndGetSession("john", "employee123");

        // Create manager session
        managerSession = loginAndGetSession("mike", "manager123");
    }

    private MockHttpSession loginAndGetSession(String username, String password) throws Exception {
        LoginDto loginDto = new LoginDto();
        loginDto.setUsername(username);
        loginDto.setPassword(password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andReturn();

        return (MockHttpSession) result.getRequest().getSession();
    }

    // ========== TEST 1: Submit Leave - Success ==========
    @Test
    @DisplayName("Integration Test: Submit Leave - Success")
    void testSubmitLeave_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/leave")
                        .session(employeeSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Leave request submitted successfully"))
                .andExpect(jsonPath("$.requestId").exists());

        // Verify in database
        User user = userRepository.findByUsername("john").orElseThrow();
        assertTrue(leaveRequestRepository.findByEmployeeId(user.getId()).size() > 0);
    }

    // ========== TEST 2: Submit Leave - Without Login ==========
    @Test
    @DisplayName("Integration Test: Submit Leave - Without Login")
    void testSubmitLeave_WithoutLogin() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/leave")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Please login first"));
    }

    // ========== TEST 3: Submit Leave - As Manager (Should Fail) ==========
    @Test
    @DisplayName("Integration Test: Submit Leave - As Manager (Should Fail)")
    void testSubmitLeave_AsManager() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/leave")
                        .session(managerSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Employee access required")));
    }

    // ========== TEST 4: Get Leave Balance ==========
    @Test
    @DisplayName("Integration Test: Get Leave Balance")
    void testGetLeaveBalance() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/leave/balance")
                        .session(employeeSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.balance.annual").exists())
                .andExpect(jsonPath("$.balance.sick").exists())
                .andExpect(jsonPath("$.balance.family").exists());
    }

    // ========== TEST 5: Submit Then Approve Leave ==========
    @Test
    @DisplayName("Integration Test: Submit Then Approve Leave")
    void testSubmitThenApproveLeave() throws Exception {
        // Step 1: Submit leave as employee
        MvcResult submitResult = mockMvc.perform(post("/api/leave")
                        .session(employeeSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String response = submitResult.getResponse().getContentAsString();
        Long requestId = objectMapper.readTree(response).get("requestId").asLong();

        // Step 2: Approve as manager
        String approveBody = "{\"status\":\"Approved\",\"comments\":\"Approved in integration test\"}";

        mockMvc.perform(put("/api/leave/" + requestId)
                        .session(managerSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(approveBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Leave request approved successfully"));

        // Step 3: Verify in database
        LeaveRequest approved = leaveRequestRepository.findById(requestId).orElseThrow();
        assertEquals("Approved", approved.getStatus());
        assertEquals("Mike Adams", approved.getApprovedBy());
    }

    // ========== TEST 6: Get All Leave - Employee ==========
    @Test
    @DisplayName("Integration Test: Get All Leave - Employee")
    void testGetAllLeave_Employee() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/leave")
                        .session(employeeSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.count").exists());
    }

    // ========== TEST 7: Get All Leave - Manager ==========
    @Test
    @DisplayName("Integration Test: Get All Leave - Manager")
    void testGetAllLeave_Manager() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/leave")
                        .session(managerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.count").exists());
    }
}