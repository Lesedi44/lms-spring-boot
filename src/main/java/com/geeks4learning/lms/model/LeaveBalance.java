package com.geeks4learning.lms.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "leave_balances")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveBalance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", unique = true, nullable = false)
    private Long employeeId;

    @Column(name = "annual_leave")
    private Integer annualLeave = 15;

    @Column(name = "sick_leave")
    private Integer sickLeave = 10;

    @Column(name = "family_leave")
    private Integer familyLeave = 5;

    public Integer getBalance(String leaveType) {
        return switch (leaveType) {
            case "Annual" -> annualLeave;
            case "Sick" -> sickLeave;
            case "Family" -> familyLeave;
            default -> 0;
        };
    }

    public void deductBalance(String leaveType, int days) {
        switch (leaveType) {
            case "Annual" -> annualLeave -= days;
            case "Sick" -> sickLeave -= days;
            case "Family" -> familyLeave -= days;
        }
    }
}