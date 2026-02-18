package com.geeks4learning.lms.repository;

import com.geeks4learning.lms.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findById(Long id);
    List<User> findByRole(String role);
    List<User> findByManagerId(Long managerId);
    boolean existsByUsername(String username);
}