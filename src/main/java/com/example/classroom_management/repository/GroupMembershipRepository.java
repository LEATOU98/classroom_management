package com.example.classroom_management.repository;

import com.example.classroom_management.entity.GroupMembership;
import com.example.classroom_management.entity.StudyGroup;
import com.example.classroom_management.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GroupMembershipRepository extends JpaRepository<GroupMembership, Long> {
    List<GroupMembership> findByStudent(User student);
    List<GroupMembership> findByStudyGroup(StudyGroup group);
    List<GroupMembership> findByStudentAndIsActive(User student, Boolean isActive);
    boolean existsByStudentAndStudyGroup(User student, StudyGroup group);
    
    @Query("SELECT gm FROM GroupMembership gm JOIN FETCH gm.student JOIN FETCH gm.studyGroup sg JOIN FETCH sg.course WHERE gm.studyGroup = :group")
    List<GroupMembership> findMembersByGroup(@Param("group") StudyGroup group);
    
    @Query("SELECT gm FROM GroupMembership gm JOIN FETCH gm.student JOIN FETCH gm.studyGroup sg JOIN FETCH sg.course JOIN FETCH sg.teacher WHERE gm.student = :student")
    List<GroupMembership> findStudentGroups(@Param("student") User student);
}