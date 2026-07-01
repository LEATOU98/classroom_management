package com.example.classroom_management.repository;

import com.example.classroom_management.entity.StudyGroup;
import com.example.classroom_management.entity.Course;
import com.example.classroom_management.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StudyGroupRepository extends JpaRepository<StudyGroup, Long> {
    List<StudyGroup> findByTeacher(User teacher);
    List<StudyGroup> findByCourse(Course course);
    List<StudyGroup> findByTeacherAndCourse(User teacher, Course course);
    
    @Query("SELECT sg FROM StudyGroup sg JOIN FETCH sg.course WHERE sg.teacher = :teacher ORDER BY sg.groupName ASC")
    List<StudyGroup> findByTeacherWithCourse(@Param("teacher") User teacher);
    
    @Query("SELECT sg FROM StudyGroup sg JOIN FETCH sg.course JOIN FETCH sg.teacher ORDER BY sg.groupName ASC")
    List<StudyGroup> findAllWithDetails();
    
    @Query("SELECT sg FROM StudyGroup sg JOIN FETCH sg.course JOIN FETCH sg.teacher WHERE " +
           "LOWER(sg.groupName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(sg.course.courseName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(sg.teacher.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<StudyGroup> searchGroups(@Param("keyword") String keyword);
}