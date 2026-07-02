package com.example.classroom_management.repository;

import com.example.classroom_management.entity.StudentScore;
import com.example.classroom_management.entity.Exam;
import com.example.classroom_management.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StudentScoreRepository extends JpaRepository<StudentScore, Long> {
    List<StudentScore> findByExam(Exam exam);
    List<StudentScore> findByStudent(User student);
    StudentScore findByExamAndStudent(Exam exam, User student);
    List<StudentScore> findByExamAndIsGraded(Exam exam, Boolean isGraded);
    
    @Query("SELECT ss FROM StudentScore ss JOIN FETCH ss.exam JOIN FETCH ss.student WHERE ss.exam = :exam ORDER BY ss.student.fullName ASC")
    List<StudentScore> findByExamWithStudent(@Param("exam") Exam exam);
    
    @Query("SELECT ss FROM StudentScore ss JOIN FETCH ss.exam e JOIN FETCH e.course WHERE ss.student = :student ORDER BY e.examDate DESC")
    List<StudentScore> findByStudentWithExam(@Param("student") User student);
    
    @Query("SELECT AVG(ss.score) FROM StudentScore ss WHERE ss.student = :student AND ss.isGraded = true")
    Double getAverageScore(@Param("student") User student);
}