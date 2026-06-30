package com.example.classroom_management.repository;

import com.example.classroom_management.entity.Course;
import com.example.classroom_management.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByTeacher(User teacher);
    Course findByCourseCode(String courseCode);
}