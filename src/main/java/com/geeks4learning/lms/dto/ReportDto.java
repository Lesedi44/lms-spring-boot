package com.geeks4learning.lms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.Map;

@Data
@AllArgsConstructor
public class ReportDto {
    private String employee;
    private String department;
    private Long totalApproved;
    private Integer totalDaysTaken;
    private Map<String, Integer> balance;
    private Long pendingRequests;
}