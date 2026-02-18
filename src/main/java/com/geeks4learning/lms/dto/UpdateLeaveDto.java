package com.geeks4learning.lms.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UpdateLeaveDto {
    @NotBlank(message = "Status is required")
    @Pattern(regexp = "Approved|Rejected", message = "Status must be Approved or Rejected")
    private String status;

    private String comments;
}