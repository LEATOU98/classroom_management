package com.example.classroom_management.controller;

import com.example.classroom_management.entity.*;
import com.example.classroom_management.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class MainController {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CourseRepository courseRepository;
    
    @Autowired
    private EnrollmentRepository enrollmentRepository;
    
    @Autowired
    private SessionRepository sessionRepository;
    
    // ============ TRANG ĐĂNG NHẬP ============
    @GetMapping("/login")
    public String login() {
        return "login";
    }
    
    // ============ DASHBOARD CHÍNH ============
    @GetMapping("/dashboard")
    public String dashboard(Authentication auth) {
        User currentUser = userRepository.findByUsername(auth.getName());
        
        switch (currentUser.getRole()) {
            case ADMIN:
                return "redirect:/admin/dashboard";
            case TEACHER:
                return "redirect:/teacher/dashboard";
            case STUDENT:
                return "redirect:/student/dashboard";
            default:
                return "redirect:/login";
        }
    }
    
    // ============ ADMIN CONTROLLER ============
    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model, Authentication auth) {
        User admin = userRepository.findByUsername(auth.getName());
        List<User> allUsers = userRepository.findAll();
        
        model.addAttribute("admin", admin);
        model.addAttribute("users", allUsers);
        return "admin/dashboard";
    }
    
    @GetMapping("/admin/users/create")
    public String showCreateUserForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("roles", User.Role.values());
        return "admin/create-user";
    }
    
    @PostMapping("/admin/users/create")
    public String createUser(@ModelAttribute User user, Authentication auth) {
        User creator = userRepository.findByUsername(auth.getName());
        user.setCreatedBy(creator);
        userRepository.save(user);
        return "redirect:/admin/dashboard";
    }
    
    @GetMapping("/admin/users/edit/{id}")
    public String showEditUserForm(@PathVariable Long id, Model model) {
        User user = userRepository.findById(id).orElseThrow(() -> 
            new RuntimeException("Không tìm thấy user với ID: " + id));
        model.addAttribute("user", user);
        model.addAttribute("roles", User.Role.values());
        return "admin/edit-user";
    }
    
    @PostMapping("/admin/users/edit/{id}")
    public String updateUser(@PathVariable Long id, @ModelAttribute User updatedUser) {
        User existingUser = userRepository.findById(id).orElseThrow();
        existingUser.setFullName(updatedUser.getFullName());
        existingUser.setEmail(updatedUser.getEmail());
        existingUser.setPhone(updatedUser.getPhone());
        existingUser.setRole(updatedUser.getRole());
        existingUser.setPassword(updatedUser.getPassword());
        userRepository.save(existingUser);
        return "redirect:/admin/dashboard";
    }
    
    @GetMapping("/admin/users/delete/{id}")
    public String deleteUser(@PathVariable Long id) {
        userRepository.deleteById(id);
        return "redirect:/admin/dashboard";
    }
    
    @GetMapping("/admin/courses")
    public String adminViewAllCourses(Model model) {
        model.addAttribute("courses", courseRepository.findAll());
        return "admin/courses";
    }
    
    // ============ TEACHER CONTROLLER ============
    @GetMapping("/teacher/dashboard")
    public String teacherDashboard(Model model, Authentication auth) {
        User teacher = userRepository.findByUsername(auth.getName());
        List<Course> myCourses = courseRepository.findByTeacher(teacher);
        
        model.addAttribute("teacher", teacher);
        model.addAttribute("courses", myCourses);
        return "teacher/dashboard";
    }
    
    @GetMapping("/teacher/courses/create")
    public String showCreateCourseForm(Model model) {
        model.addAttribute("course", new Course());
        return "teacher/create-course";
    }
    
    @PostMapping("/teacher/courses/create")
    public String createCourse(@ModelAttribute Course course, Authentication auth) {
        User teacher = userRepository.findByUsername(auth.getName());
        course.setTeacher(teacher);
        courseRepository.save(course);
        return "redirect:/teacher/dashboard";
    }
    
    @GetMapping("/teacher/courses/edit/{id}")
    public String showEditCourseForm(@PathVariable Long id, Model model, Authentication auth) {
        Course course = courseRepository.findById(id).orElseThrow();
        User teacher = userRepository.findByUsername(auth.getName());
        
        // Kiểm tra xem có phải giáo viên của khóa học không
        if (!course.getTeacher().getId().equals(teacher.getId())) {
            return "redirect:/teacher/dashboard";
        }
        
        model.addAttribute("course", course);
        return "teacher/edit-course";
    }
    
    @PostMapping("/teacher/courses/edit/{id}")
    public String updateCourse(@PathVariable Long id, @ModelAttribute Course updatedCourse) {
        Course course = courseRepository.findById(id).orElseThrow();
        course.setCourseName(updatedCourse.getCourseName());
        course.setPricePerSession(updatedCourse.getPricePerSession());
        course.setStartDate(updatedCourse.getStartDate());
        course.setEndDate(updatedCourse.getEndDate());
        course.setPaymentStartDate(updatedCourse.getPaymentStartDate());
        course.setPaymentDayOfMonth(updatedCourse.getPaymentDayOfMonth());
        courseRepository.save(course);
        return "redirect:/teacher/dashboard";
    }
    
    @GetMapping("/teacher/courses/delete/{id}")
    public String deleteCourse(@PathVariable Long id, Authentication auth) {
        Course course = courseRepository.findById(id).orElseThrow();
        User teacher = userRepository.findByUsername(auth.getName());
        
        if (course.getTeacher().getId().equals(teacher.getId())) {
            courseRepository.deleteById(id);
        }
        return "redirect:/teacher/dashboard";
    }
    
    @GetMapping("/teacher/courses/{id}/students")
    public String viewCourseStudents(@PathVariable Long id, Model model, Authentication auth) {
        Course course = courseRepository.findById(id).orElseThrow();
        User teacher = userRepository.findByUsername(auth.getName());
        
        if (!course.getTeacher().getId().equals(teacher.getId())) {
            return "redirect:/teacher/dashboard";
        }
        
        List<Enrollment> enrollments = enrollmentRepository.findByCourse(course);
        model.addAttribute("course", course);
        model.addAttribute("enrollments", enrollments);
        return "teacher/course-students";
    }
    
    @GetMapping("/teacher/profile")
    public String teacherProfile(Model model, Authentication auth) {
        User teacher = userRepository.findByUsername(auth.getName());
        model.addAttribute("user", teacher);
        return "teacher/profile";
    }
    
    @PostMapping("/teacher/profile")
    public String updateTeacherProfile(@ModelAttribute User updatedUser, Authentication auth) {
        User teacher = userRepository.findByUsername(auth.getName());
        teacher.setFullName(updatedUser.getFullName());
        teacher.setEmail(updatedUser.getEmail());
        teacher.setPhone(updatedUser.getPhone());
        teacher.setPassword(updatedUser.getPassword());
        userRepository.save(teacher);
        return "redirect:/teacher/dashboard";
    }
    
    // ============ STUDENT CONTROLLER ============
    @GetMapping("/student/dashboard")
    public String studentDashboard(Model model, Authentication auth) {
        User student = userRepository.findByUsername(auth.getName());
        List<Enrollment> myEnrollments = enrollmentRepository.findByStudent(student);
        
        model.addAttribute("student", student);
        model.addAttribute("enrollments", myEnrollments);
        return "student/dashboard";
    }
    
    @GetMapping("/student/profile")
    public String studentProfile(Model model, Authentication auth) {
        User student = userRepository.findByUsername(auth.getName());
        model.addAttribute("user", student);
        return "student/profile";
    }
    
    @PostMapping("/student/profile")
    public String updateStudentProfile(@ModelAttribute User updatedUser, Authentication auth) {
        User student = userRepository.findByUsername(auth.getName());
        student.setFullName(updatedUser.getFullName());
        student.setEmail(updatedUser.getEmail());
        student.setPhone(updatedUser.getPhone());
        student.setPassword(updatedUser.getPassword());
        userRepository.save(student);
        return "redirect:/student/dashboard";
    }
}