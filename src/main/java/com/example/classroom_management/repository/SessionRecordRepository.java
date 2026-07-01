package com.example.classroom_management.repository;

import com.example.classroom_management.entity.SessionRecord;
import com.example.classroom_management.entity.User;
import com.example.classroom_management.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.time.LocalDate;

@Repository
public interface SessionRecordRepository extends JpaRepository<SessionRecord, Long> {
    List<SessionRecord> findByStudent(User student);
    List<SessionRecord> findByStudentAndCourse(User student, Course course);
    List<SessionRecord> findByStudentAndSessionDate(User student, LocalDate date);
    Long countByStudentAndCourseAndIsPresent(User student, Course course, Boolean isPresent);
}