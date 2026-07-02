package com.example.classroom_management.repository;

import com.example.classroom_management.entity.Exam;
import com.example.classroom_management.entity.Course;
import com.example.classroom_management.entity.User;
import com.example.classroom_management.entity.StudyGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {
    List<Exam> findByTeacher(User teacher);
    List<Exam> findByCourse(Course course);
    List<Exam> findByGroup(StudyGroup group);
    List<Exam> findByCourseOrderByExamDateDesc(Course course);
    Exam findByExamCode(String examCode);
    
    @Query("SELECT e FROM Exam e JOIN FETCH e.course JOIN FETCH e.teacher ORDER BY e.examDate DESC")
    List<Exam> findAllWithDetails();
    
    @Query("SELECT e FROM Exam e JOIN FETCH e.course JOIN FETCH e.teacher WHERE e.teacher = :teacher ORDER BY e.examDate DESC")
    List<Exam> findByTeacherWithDetails(@Param("teacher") User teacher);
    
    @Query("SELECT e FROM Exam e JOIN FETCH e.course JOIN FETCH e.teacher WHERE " +
           "LOWER(e.examName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.course.courseName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Exam> searchExams(@Param("keyword") String keyword);
}