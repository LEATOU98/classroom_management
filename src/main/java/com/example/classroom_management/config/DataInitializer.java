package com.example.classroom_management.config;

import com.example.classroom_management.entity.User;
import com.example.classroom_management.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Override
    public void run(String... args) {
        if (userRepository.findByUsername("admin") == null) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword("admin123");
            admin.setFullName("Quản trị viên");
            admin.setEmail("admin@school.com");
            admin.setPhone("0123456789");
            admin.setRole(User.Role.ADMIN);
            userRepository.save(admin);
            System.out.println(">>> Đã tạo tài khoản admin mặc định: admin/admin123");
        }
    }
}