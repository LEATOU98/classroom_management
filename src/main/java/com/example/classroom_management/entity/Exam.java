package com.example.classroom_management.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "exams")
@Data
public class Exam {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exam_code", unique = true, length = 50)
    private String examCode;

    @Column(name = "exam_name", nullable = false, length = 200)
    private String examName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "exam_date")
    private LocalDateTime examDate;

    // SỬA: Dùng columnDefinition thay vì precision/scale
    @Column(name = "max_score", columnDefinition = "DOUBLE DEFAULT 10.0")
    private Double maxScore = 10.0;

    // SỬA: Dùng columnDefinition thay vì precision/scale
    @Column(name = "pass_score", columnDefinition = "DOUBLE DEFAULT 5.0")
    private Double passScore = 5.0;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "group_id")
    private StudyGroup group;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (examCode == null || examCode.isEmpty()) {
            examCode = "EXAM-" + System.currentTimeMillis();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}