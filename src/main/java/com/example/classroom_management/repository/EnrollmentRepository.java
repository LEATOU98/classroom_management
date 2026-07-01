package com.example.classroom_management.repository;

import com.example.classroom_management.entity.Enrollment;
import com.example.classroom_management.entity.Course;
import com.example.classroom_management.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByStudent(User student);
    List<Enrollment> findByCourse(Course course);
    boolean existsByStudentAndCourse(User student, Course course);

    @Query("SELECT e FROM Enrollment e JOIN FETCH e.student JOIN FETCH e.course c JOIN FETCH c.teacher")
    List<Enrollment> findAllWithDetails();

    @Query("SELECT e FROM Enrollment e JOIN FETCH e.student JOIN FETCH e.course c JOIN FETCH c.teacher WHERE c.teacher = :teacher")
    List<Enrollment> findByTeacher(@Param("teacher") User teacher);

    @Query("SELECT e FROM Enrollment e JOIN FETCH e.student JOIN FETCH e.course c JOIN FETCH c.teacher WHERE c.teacher = :teacher AND c = :course")
    List<Enrollment> findByTeacherAndCourse(@Param("teacher") User teacher, @Param("course") Course course);
}