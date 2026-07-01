package com.example.classroom_management.repository;

import com.example.classroom_management.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
    List<User> findByRole(User.Role role);
    List<User> findByCreatedBy(User creator);
    boolean existsByUsername(String username);

    List<User> findByAssignedTeacher(User teacher);

    @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.phone) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<User> searchUsers(@Param("keyword") String keyword);

    @Query("SELECT u FROM User u WHERE u.role = :role AND " +
            "(LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.phone) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<User> searchUsersByRole(@Param("keyword") String keyword, @Param("role") User.Role role);

    @Query("SELECT u FROM User u WHERE u.role = 'STUDENT' AND u.assignedTeacher = :teacher")
    List<User> findStudentsByTeacher(@Param("teacher") User teacher);

    @Query("SELECT DISTINCT e.student FROM Enrollment e WHERE e.course.teacher = :teacher")
    List<User> findStudentsByTeacherCourses(@Param("teacher") User teacher);

    @Query("SELECT DISTINCT e.student FROM Enrollment e WHERE e.course = :course")
    List<User> findStudentsByCourse(@Param("course") com.example.classroom_management.entity.Course course);

    @Query("SELECT u FROM User u ORDER BY u.fullName ASC")
    List<User> findAllOrderByFullName();
}