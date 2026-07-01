package com.example.classroom_management.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
    private Integer paymentDayOfMonth; // 1-27 hoặc 0 cho cuối tháng

    @ManyToOne(fetch = FetchType.EAGER)
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

    public String getFormattedStartDate() {
        if (startDate != null) {
            return startDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }
        return "";
    }

    public String getFormattedEndDate() {
        if (endDate != null) {
            return endDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }
        return "";
    }

    public String getPaymentDayDisplay() {
        if (paymentDayOfMonth == null) return "Chưa đặt";
        if (paymentDayOfMonth == 0) return "Cuối tháng";
        if (paymentDayOfMonth == -1) return "Đầu tháng";
        return "Ngày " + paymentDayOfMonth;
    }

    public String getFormattedPrice() {
        if (pricePerSession != null) {
            return String.format("%,.0f VNĐ", pricePerSession);
        }
        return "0 VNĐ";
    }
}