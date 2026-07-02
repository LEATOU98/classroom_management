package com.example.classroom_management.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_scores")
@Data
public class StudentScore {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    // SỬA: Dùng columnDefinition thay vì precision/scale
    @Column(name = "score", columnDefinition = "DOUBLE")
    private Double score;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "is_graded")
    private Boolean isGraded = false;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "graded_by")
    private User gradedBy;

    @Column(name = "graded_at")
    private LocalDateTime gradedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getStatusDisplay() {
        if (!isGraded) return "Chưa chấm";
        if (score == null) return "Chưa có điểm";
        if (exam != null && exam.getPassScore() != null && score >= exam.getPassScore()) return "Đạt";
        if (exam != null && exam.getPassScore() != null) return "Không đạt";
        return "Đã chấm";
    }

    public String getStatusClass() {
        if (!isGraded) return "badge-warning";
        if (score == null) return "badge-secondary";
        if (exam != null && exam.getPassScore() != null && score >= exam.getPassScore()) return "badge-success";
        return "badge-danger";
    }
}