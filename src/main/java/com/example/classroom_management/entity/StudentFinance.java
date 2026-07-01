package com.example.classroom_management.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_finances")
@Data
public class StudentFinance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;
    
    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO; // Tổng tiền phải đóng
    
    @Column(name = "paid_amount", precision = 10, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO; // Đã đóng
    
    @Column(name = "debt_amount", precision = 10, scale = 2)
    private BigDecimal debtAmount = BigDecimal.ZERO; // Còn nợ
    
    @Column(name = "sessions_count")
    private Integer sessionsCount = 0; // Số buổi đã học
    
    @Column(name = "last_payment_date")
    private LocalDate lastPaymentDate; // Ngày đóng tiền gần nhất
    
    @Column(name = "due_date")
    private LocalDate dueDate; // Ngày đến hạn đóng
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FinanceStatus status = FinanceStatus.UNPAID;
    
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
    
    public enum FinanceStatus {
        UNPAID, // Chưa đóng
        PARTIAL, // Đóng 1 phần
        PAID, // Đã đóng đủ
        OVERDUE // Quá hạn
    }
    
    public BigDecimal getRemainingAmount() {
        return totalAmount.subtract(paidAmount);
    }
}