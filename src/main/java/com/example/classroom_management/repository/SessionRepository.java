package com.example.classroom_management.repository;

import com.example.classroom_management.entity.Session;
import com.example.classroom_management.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {
    List<Session> findByCourse(Course course);
    List<Session> findByCourseOrderBySessionDateAsc(Course course);
}