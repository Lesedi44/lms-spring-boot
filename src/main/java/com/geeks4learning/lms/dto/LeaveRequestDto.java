package com.geeks4learning.lms.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.ToString;
import java.time.LocalDate;

@Data
@ToString
public class LeaveRequestDto {

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @NotBlank(message = "Leave type is required")
    @Pattern(regexp = "Annual|Sick|Family", message = "Leave type must be Annual, Sick, or Family")
    private String leaveType;

    @NotBlank(message = "Reason is required")
    @Size(min = 3, max = 500, message = "Reason must be between 3 and 500 characters")
    private String reason;
}