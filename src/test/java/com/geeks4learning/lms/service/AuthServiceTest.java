package com.geeks4learning.lms.service;

import com.geeks4learning.lms.model.User;
import com.geeks4learning.lms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    private User employee;
    private User manager;
    private User admin;

    @BeforeEach
    void setUp() {
        employee = new User();
        employee.setId(1L);
        employee.setUsername("john");
        employee.setPassword("employee123");
        employee.setRole("EMPLOYEE");
        employee.setName("John Doe");

        manager = new User();
        manager.setId(2L);
        manager.setUsername("mike");
        manager.setPassword("manager123");
        manager.setRole("MANAGER");
        manager.setName("Mike Adams");

        admin = new User();
        admin.setId(3L);
        admin.setUsername("admin");
        admin.setPassword("admin123");
        admin.setRole("ADMIN");
        admin.setName("System Admin");
    }

    @Test
    @DisplayName("Authenticate - Success (Employee)")
    void testAuthenticate_Success_Employee() {
        // Arrange
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(employee));

        // Act
        User result = authService.authenticate("john", "employee123");

        // Assert
        assertNotNull(result);
        assertEquals("john", result.getUsername());
        assertEquals("EMPLOYEE", result.getRole());
    }

    @Test
    @DisplayName("Authenticate - Success (Manager)")
    void testAuthenticate_Success_Manager() {
        // Arrange
        when(userRepository.findByUsername("mike")).thenReturn(Optional.of(manager));

        // Act
        User result = authService.authenticate("mike", "manager123");

        // Assert
        assertNotNull(result);
        assertEquals("mike", result.getUsername());
        assertEquals("MANAGER", result.getRole());
    }

    @Test
    @DisplayName("Authenticate - Wrong Password")
    void testAuthenticate_WrongPassword() {
        // Arrange
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(employee));

        // Act
        User result = authService.authenticate("john", "wrongpassword");

        // Assert
        assertNull(result);
    }

    @Test
    @DisplayName("Authenticate - User Not Found")
    void testAuthenticate_UserNotFound() {
        // Arrange
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // Act
        User result = authService.authenticate("unknown", "anypassword");

        // Assert
        assertNull(result);
    }

    @Test
    @DisplayName("Role Check - isEmployee")
    void testIsEmployee() {
        assertTrue(authService.isEmployee(employee));
        assertFalse(authService.isEmployee(manager));
        assertFalse(authService.isEmployee(admin));
        assertFalse(authService.isEmployee(null));
    }

    @Test
    @DisplayName("Role Check - isManager")
    void testIsManager() {
        assertFalse(authService.isManager(employee));
        assertTrue(authService.isManager(manager));
        assertTrue(authService.isManager(admin)); // Admin is also considered manager
        assertFalse(authService.isManager(null));
    }

    @Test
    @DisplayName("Role Check - isAdmin")
    void testIsAdmin() {
        assertFalse(authService.isAdmin(employee));
        assertFalse(authService.isAdmin(manager));
        assertTrue(authService.isAdmin(admin));
        assertFalse(authService.isAdmin(null));
    }

    @Test
    @DisplayName("Has Access - Check various roles")
    void testHasAccess() {
        assertTrue(authService.hasAccess(employee, "EMPLOYEE"));
        assertFalse(authService.hasAccess(employee, "MANAGER"));
        assertFalse(authService.hasAccess(employee, "ADMIN"));

        assertTrue(authService.hasAccess(manager, "MANAGER"));
        assertTrue(authService.hasAccess(admin, "ADMIN"));
        assertTrue(authService.hasAccess(admin, "MANAGER")); // Admin has manager access

        assertFalse(authService.hasAccess(null, "EMPLOYEE"));
    }
}