package com.example.classroom_management.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "courses")
@Data
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "course_code", unique = true, length = 50)
    private String courseCode;
    
    @Column(name = "course_name", nullable = false, length = 200)
    private String courseName;
    
    @Column(name = "price_per_session", precision = 10, scale = 2)
    private BigDecimal pricePerSession;
    
    @Column(name = "start_date")
    private LocalDate startDate;
    
    @Column(name = "payment_start_date")
    private LocalDate paymentStartDate;
    
    @Column(name = "end_date")
    private LocalDate endDate;
    
    @Column(name = "payment_day_of_month")
    private Integer paymentDayOfMonth;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (courseCode == null || courseCode.isEmpty()) {
            courseCode = "COURSE-" + System.currentTimeMillis();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}