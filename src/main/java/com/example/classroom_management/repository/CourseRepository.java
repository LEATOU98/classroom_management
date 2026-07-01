package com.example.classroom_management.repository;

import com.example.classroom_management.entity.Course;
import com.example.classroom_management.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByTeacher(User teacher);
    Course findByCourseCode(String courseCode);

    @Query("SELECT c FROM Course c JOIN FETCH c.teacher ORDER BY c.courseName ASC")
    List<Course> findAllWithTeacher();

    @Query("SELECT c FROM Course c JOIN FETCH c.teacher WHERE c.teacher = :teacher ORDER BY c.courseName ASC")
    List<Course> findByTeacherWithDetails(@Param("teacher") User teacher);

    @Query("SELECT c FROM Course c JOIN FETCH c.teacher WHERE " +
            "LOWER(c.courseName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.courseCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.teacher.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Course> searchCourses(@Param("keyword") String keyword);

    @Query("SELECT c FROM Course c JOIN FETCH c.teacher WHERE c.teacher = :teacher AND " +
            "(LOWER(c.courseName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.courseCode) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Course> searchCoursesByTeacher(@Param("keyword") String keyword, @Param("teacher") User teacher);
}