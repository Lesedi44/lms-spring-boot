package com.geeks4learning.lms.config;

import com.geeks4learning.lms.model.LeaveBalance;
import com.geeks4learning.lms.model.User;
import com.geeks4learning.lms.repository.LeaveBalanceRepository;
import com.geeks4learning.lms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("Initializing database...");

        // Clear existing data
        leaveBalanceRepository.deleteAll();
        userRepository.deleteAll();

        // Create admin user
        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword("admin123");
        admin.setName("System Admin");
        admin.setRole("ADMIN");
        admin.setDepartment("IT");
        admin.setManagerId(null);
        admin.setCreatedAt(LocalDateTime.now());
        userRepository.save(admin);
        log.info("Created admin user: {}", admin.getUsername());

        // Create manager users
        User mike = new User();
        mike.setUsername("mike");
        mike.setPassword("manager123");
        mike.setName("Mike Adams");
        mike.setRole("MANAGER");
        mike.setDepartment("Engineering");
        mike.setManagerId(null);
        mike.setCreatedAt(LocalDateTime.now());
        userRepository.save(mike);
        log.info("Created manager: {}", mike.getUsername());

        User sarah = new User();
        sarah.setUsername("sarah");
        sarah.setPassword("manager123");
        sarah.setName("Sarah Brown");
        sarah.setRole("MANAGER");
        sarah.setDepartment("Marketing");
        sarah.setManagerId(null);
        sarah.setCreatedAt(LocalDateTime.now());
        userRepository.save(sarah);
        log.info("Created manager: {}", sarah.getUsername());

        // Create employee users
        User john = new User();
        john.setUsername("john");
        john.setPassword("employee123");
        john.setName("John Doe");
        john.setRole("EMPLOYEE");
        john.setDepartment("Engineering");
        john.setManagerId(mike.getId());
        john.setCreatedAt(LocalDateTime.now());
        userRepository.save(john);
        log.info("Created employee: {}", john.getUsername());

        User jane = new User();
        jane.setUsername("jane");
        jane.setPassword("employee123");
        jane.setName("Jane Smith");
        jane.setRole("EMPLOYEE");
        jane.setDepartment("Marketing");
        jane.setManagerId(sarah.getId());
        jane.setCreatedAt(LocalDateTime.now());
        userRepository.save(jane);
        log.info("Created employee: {}", jane.getUsername());

        // When creating users, add email addresses
        john.setEmail("john.doe@example.com");
        jane.setEmail("jane.smith@example.com");
        mike.setEmail("mike.adams@example.com");
        sarah.setEmail("sarah.brown@example.com");
        admin.setEmail("admin@example.com");

        // Create leave balances
        LeaveBalance johnBalance = new LeaveBalance();
        johnBalance.setEmployeeId(john.getId());
        johnBalance.setAnnualLeave(15);
        johnBalance.setSickLeave(10);
        johnBalance.setFamilyLeave(5);
        leaveBalanceRepository.save(johnBalance);

        LeaveBalance janeBalance = new LeaveBalance();
        janeBalance.setEmployeeId(jane.getId());
        janeBalance.setAnnualLeave(15);
        janeBalance.setSickLeave(10);
        janeBalance.setFamilyLeave(5);
        leaveBalanceRepository.save(janeBalance);

        log.info("Database initialization complete");
        log.info("Total users: {}", userRepository.count());
        log.info("Total balances: {}", leaveBalanceRepository.count());
    }
}