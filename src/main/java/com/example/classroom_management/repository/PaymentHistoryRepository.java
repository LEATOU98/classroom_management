package com.example.classroom_management.repository;

import com.example.classroom_management.entity.PaymentHistory;
import com.example.classroom_management.entity.StudentFinance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, Long> {
    List<PaymentHistory> findByStudentFinanceOrderByPaymentDateDesc(StudentFinance studentFinance);
}