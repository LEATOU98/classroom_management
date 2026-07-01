package com.example.classroom_management.repository;

import com.example.classroom_management.entity.ExtraSession;
import com.example.classroom_management.entity.StudyGroup;
import com.example.classroom_management.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ExtraSessionRepository extends JpaRepository<ExtraSession, Long> {
    List<ExtraSession> findByStudent(User student);
    List<ExtraSession> findByStudyGroup(StudyGroup group);
    List<ExtraSession> findByStudentAndStudyGroup(User student, StudyGroup group);
}