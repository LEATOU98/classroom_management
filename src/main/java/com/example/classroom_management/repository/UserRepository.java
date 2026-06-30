package com.example.classroom_management.repository;

import com.example.classroom_management.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
    List<User> findByRole(User.Role role);
    List<User> findByCreatedBy(User creator);
    boolean existsByUsername(String username);
}