package com.example.classroom_management.repository;

import com.example.classroom_management.entity.StudentFinance;
import com.example.classroom_management.entity.User;
import com.example.classroom_management.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StudentFinanceRepository extends JpaRepository<StudentFinance, Long> {
    List<StudentFinance> findByStudent(User student);
    List<StudentFinance> findByCourse(Course course);
    StudentFinance findByStudentAndCourse(User student, Course course);
    List<StudentFinance> findByStatus(StudentFinance.FinanceStatus status);
    
    @Query("SELECT sf FROM StudentFinance sf JOIN FETCH sf.student JOIN FETCH sf.course WHERE sf.student = :student")
    List<StudentFinance> findByStudentWithDetails(@Param("student") User student);
    
    @Query("SELECT sf FROM StudentFinance sf JOIN FETCH sf.student JOIN FETCH sf.course WHERE sf.course.teacher = :teacher")
    List<StudentFinance> findByTeacher(@Param("teacher") User teacher);
}