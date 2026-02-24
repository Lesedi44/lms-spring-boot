package com.geeks4learning.lms.repository;

import com.geeks4learning.lms.model.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    // Find by employee ID
    List<LeaveRequest> findByEmployeeId(Long employeeId);

    // Find by manager ID
    List<LeaveRequest> findByManagerId(Long managerId);

    // Find by status
    List<LeaveRequest> findByStatus(String status);

    // ADD THIS METHOD - Find by multiple employee IDs
    List<LeaveRequest> findByEmployeeIdIn(List<Long> employeeIds);

    // Optional: Find by employee ID and status
    List<LeaveRequest> findByEmployeeIdAndStatus(Long employeeId, String status);
}