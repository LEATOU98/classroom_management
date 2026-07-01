package com.example.classroom_management.controller;

import com.example.classroom_management.entity.*;
import com.example.classroom_management.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

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



    @Autowired
    private StudentFinanceRepository studentFinanceRepository;

    @Autowired
    private PaymentHistoryRepository paymentHistoryRepository;

    @Autowired
    private SessionRecordRepository sessionRecordRepository;

    // ============ QUẢN LÝ HỌC SINH & TÀI CHÍNH - ADMIN ============
    @GetMapping("/admin/students")
    public String adminStudents(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String financeStatus,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Long courseId,
            Model model, Authentication auth) {

        User admin = userRepository.findByUsername(auth.getName());
        List<User> students;

        // Tìm kiếm học sinh
        if (search != null && !search.isEmpty()) {
            students = userRepository.searchUsersByRole(search, User.Role.STUDENT);
        } else if (teacherId != null) {
            User teacher = userRepository.findById(teacherId).orElse(null);
            if (courseId != null) {
                Course course = courseRepository.findById(courseId).orElse(null);
                students = userRepository.findStudentsByCourse(course);
            } else {
                students = userRepository.findStudentsByTeacher(teacher);
            }
        } else {
            students = userRepository.findByRole(User.Role.STUDENT);
        }

        // Sắp xếp A-Z
        students.sort(Comparator.comparing(u -> u.getFullName() != null ? u.getLastName().toLowerCase() : ""));

        // Map tài chính cho từng học sinh
        Map<Long, List<StudentFinance>> financeMap = new HashMap<>();
        Map<Long, BigDecimal> totalDebtMap = new HashMap<>();

        for (User student : students) {
            List<StudentFinance> finances = studentFinanceRepository.findByStudentWithDetails(student);
            financeMap.put(student.getId(), finances);

            BigDecimal totalDebt = finances.stream()
                    .map(StudentFinance::getRemainingAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            totalDebtMap.put(student.getId(), totalDebt);
        }

        List<User> teachers = userRepository.findByRole(User.Role.TEACHER);
        List<Course> filteredCourses = new ArrayList<>();
        if (teacherId != null) {
            User teacher = userRepository.findById(teacherId).orElse(null);
            if (teacher != null) {
                filteredCourses = courseRepository.findByTeacher(teacher);
            }
        }

        model.addAttribute("currentUser", admin);
        model.addAttribute("students", students);
        model.addAttribute("financeMap", financeMap);
        model.addAttribute("totalDebtMap", totalDebtMap);
        model.addAttribute("teachers", teachers);
        model.addAttribute("filteredCourses", filteredCourses);
        model.addAttribute("searchKeyword", search);
        model.addAttribute("selectedFinanceStatus", financeStatus);
        model.addAttribute("selectedTeacherId", teacherId);
        model.addAttribute("selectedCourseId", courseId);

        return "admin/students";
    }

    @GetMapping("/admin/students/detail/{id}")
    @ResponseBody
    public Map<String, Object> getStudentFinanceDetail(@PathVariable Long id) {
        User student = userRepository.findById(id).orElseThrow();
        List<StudentFinance> finances = studentFinanceRepository.findByStudentWithDetails(student);

        Map<String, Object> detail = new HashMap<>();
        detail.put("studentId", student.getId());
        detail.put("studentName", student.getFullName());
        detail.put("email", student.getEmail());
        detail.put("phone", student.getPhone());
        detail.put("avatarUrl", student.getAvatarUrl());
        detail.put("note", student.getNote());

        BigDecimal totalDebt = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        List<Map<String, Object>> financeList = new ArrayList<>();

        for (StudentFinance sf : finances) {
            Map<String, Object> finance = new HashMap<>();
            finance.put("id", sf.getId());
            finance.put("courseName", sf.getCourse().getCourseName());
            finance.put("totalAmount", sf.getTotalAmount());
            finance.put("paidAmount", sf.getPaidAmount());
            finance.put("debtAmount", sf.getRemainingAmount());
            finance.put("sessionsCount", sf.getSessionsCount());
            finance.put("dueDate", sf.getDueDate() != null ? sf.getDueDate().toString() : "");
            finance.put("status", sf.getStatus().name());

            totalDebt = totalDebt.add(sf.getRemainingAmount());
            totalPaid = totalPaid.add(sf.getPaidAmount());
            financeList.add(finance);
        }

        detail.put("finances", financeList);
        detail.put("totalDebt", totalDebt);
        detail.put("totalPaid", totalPaid);

        return detail;
    }

    @PostMapping("/admin/students/pay")
    @ResponseBody
    public Map<String, Object> makePayment(
            @RequestParam Long financeId,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String note,
            Authentication auth) {

        User receiver = userRepository.findByUsername(auth.getName());
        StudentFinance finance = studentFinanceRepository.findById(financeId).orElseThrow();

        // Tạo payment history
        PaymentHistory payment = new PaymentHistory();
        payment.setStudentFinance(finance);
        payment.setAmount(amount);
        payment.setPaymentDate(LocalDate.now());
        payment.setNote(note);
        payment.setReceivedBy(receiver);
        paymentHistoryRepository.save(payment);

        // Cập nhật finance
        finance.setPaidAmount(finance.getPaidAmount().add(amount));
        finance.setLastPaymentDate(LocalDate.now());

        // Cập nhật trạng thái
        BigDecimal remaining = finance.getRemainingAmount();
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            finance.setStatus(StudentFinance.FinanceStatus.PAID);
        } else if (finance.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
            finance.setStatus(StudentFinance.FinanceStatus.PARTIAL);
        }

        studentFinanceRepository.save(finance);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("newPaidAmount", finance.getPaidAmount());
        result.put("remaining", finance.getRemainingAmount());
        result.put("newStatus", finance.getStatus().name());

        return result;
    }

    @PostMapping("/admin/students/record-session")
    @ResponseBody
    public Map<String, Object> recordSession(
            @RequestParam Long studentId,
            @RequestParam Long courseId,
            @RequestParam(required = false) String note) {

        User student = userRepository.findById(studentId).orElseThrow();
        Course course = courseRepository.findById(courseId).orElseThrow();

        // Tạo session record
        SessionRecord record = new SessionRecord();
        record.setStudent(student);
        record.setCourse(course);
        record.setSessionDate(LocalDate.now());
        record.setIsPresent(true);
        record.setNote(note);
        sessionRecordRepository.save(record);

        // Cập nhật/cập nhật finance
        StudentFinance finance = studentFinanceRepository.findByStudentAndCourse(student, course);
        if (finance == null) {
            finance = new StudentFinance();
            finance.setStudent(student);
            finance.setCourse(course);
            finance.setTotalAmount(BigDecimal.ZERO);
        }

        finance.setSessionsCount(finance.getSessionsCount() + 1);

        // Tính tiền: số buổi * giá mỗi buổi
        if (course.getPricePerSession() != null) {
            BigDecimal newTotal = course.getPricePerSession().multiply(new BigDecimal(finance.getSessionsCount()));
            finance.setTotalAmount(newTotal);
        }

        // Set due date nếu chưa có
        if (finance.getDueDate() == null && course.getPaymentDayOfMonth() != null) {
            LocalDate now = LocalDate.now();
            int paymentDay = course.getPaymentDayOfMonth();
            if (paymentDay == 0) {
                paymentDay = now.lengthOfMonth(); // Cuối tháng
            } else if (paymentDay == -1) {
                paymentDay = 1; // Đầu tháng
            }

            LocalDate dueDate = LocalDate.of(now.getYear(), now.getMonth(), Math.min(paymentDay, now.lengthOfMonth()));
            if (dueDate.isBefore(now)) {
                dueDate = dueDate.plusMonths(1);
            }
            finance.setDueDate(dueDate);
        }

        // Check overdue
        if (finance.getDueDate() != null && LocalDate.now().isAfter(finance.getDueDate())
                && finance.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0) {
            finance.setStatus(StudentFinance.FinanceStatus.OVERDUE);
        }

        studentFinanceRepository.save(finance);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("sessionsCount", finance.getSessionsCount());
        result.put("totalAmount", finance.getTotalAmount());
        result.put("remaining", finance.getRemainingAmount());

        return result;
    }

    // API lấy lịch sử đóng tiền
    @GetMapping("/admin/students/payment-history/{financeId}")
    @ResponseBody
    public List<Map<String, Object>> getPaymentHistory(@PathVariable Long financeId) {
        StudentFinance finance = studentFinanceRepository.findById(financeId).orElseThrow();
        List<PaymentHistory> histories = paymentHistoryRepository.findByStudentFinanceOrderByPaymentDateDesc(finance);

        return histories.stream().map(h -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", h.getId());
            map.put("amount", h.getAmount());
            map.put("paymentDate", h.getPaymentDate().toString());
            map.put("note", h.getNote());
            map.put("receivedBy", h.getReceivedBy() != null ? h.getReceivedBy().getFullName() : "");
            return map;
        }).collect(Collectors.toList());
    }



    // ============ LOGIN & DASHBOARD ============
    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth) {
        User currentUser = userRepository.findByUsername(auth.getName());
        switch (currentUser.getRole()) {
            case ADMIN: return "redirect:/admin/profile";
            case TEACHER: return "redirect:/teacher/profile";
            case STUDENT: return "redirect:/student/profile";
            default: return "redirect:/login";
        }
    }

    // ============ ADMIN PROFILE ============
    @GetMapping("/admin/profile")
    public String adminProfile(Model model, Authentication auth) {
        User admin = userRepository.findByUsername(auth.getName());
        model.addAttribute("currentUser", admin);
        model.addAttribute("user", admin);
        return "admin/profile";
    }

    @PostMapping("/admin/profile")
    public String updateAdminProfile(@ModelAttribute User updatedUser, Authentication auth) {
        User admin = userRepository.findByUsername(auth.getName());
        admin.setFullName(updatedUser.getFullName());
        admin.setEmail(updatedUser.getEmail());
        admin.setPhone(updatedUser.getPhone());
        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
            admin.setPassword(updatedUser.getPassword());
        }
        userRepository.save(admin);
        return "redirect:/admin/profile?success";
    }

    // ============ QUẢN LÝ USERS - ADMIN ============
    @GetMapping("/admin/users")
    public String adminUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Long courseId,
            Model model, Authentication auth) {

        User admin = userRepository.findByUsername(auth.getName());
        List<User> users;

        if (search != null && !search.isEmpty()) {
            if (role != null && !role.isEmpty() && !role.equals("ALL")) {
                users = userRepository.searchUsersByRole(search, User.Role.valueOf(role));
            } else {
                users = userRepository.searchUsers(search);
            }
        } else if ("STUDENT".equals(role) && teacherId != null) {
            User teacher = userRepository.findById(teacherId).orElse(null);
            if (courseId != null) {
                Course course = courseRepository.findById(courseId).orElse(null);
                users = userRepository.findStudentsByCourse(course);
            } else {
                users = userRepository.findStudentsByTeacher(teacher);
            }
        } else if (role != null && !role.isEmpty() && !role.equals("ALL")) {
            users = userRepository.findByRole(User.Role.valueOf(role));
        } else {
            users = userRepository.findAllOrderByFullName();
        }

        if (!users.isEmpty()) {
            users.sort(Comparator.comparing(u ->
                    u.getFullName() != null ? u.getLastName().toLowerCase() : ""));
        }

        List<User> teachers = userRepository.findByRole(User.Role.TEACHER);
        List<Course> filteredCourses = new ArrayList<>();
        if (teacherId != null) {
            User selectedTeacher = userRepository.findById(teacherId).orElse(null);
            if (selectedTeacher != null) {
                filteredCourses = courseRepository.findByTeacher(selectedTeacher);
            }
        }

        model.addAttribute("currentUser", admin);
        model.addAttribute("users", users);
        model.addAttribute("teachers", teachers);
        model.addAttribute("filteredCourses", filteredCourses);
        model.addAttribute("selectedRole", role);
        model.addAttribute("searchKeyword", search);
        model.addAttribute("selectedTeacherId", teacherId);
        model.addAttribute("selectedCourseId", courseId);
        model.addAttribute("roles", User.Role.values());

        return "admin/users";
    }

    @GetMapping("/admin/users/{id}")
    @ResponseBody
    public Map<String, Object> getUserDetail(@PathVariable Long id) {
        User user = userRepository.findById(id).orElseThrow();
        Map<String, Object> detail = new HashMap<>();
        detail.put("id", user.getId());
        detail.put("username", user.getUsername());
        detail.put("fullName", user.getFullName());
        detail.put("email", user.getEmail());
        detail.put("phone", user.getPhone());
        detail.put("role", user.getRole().name());
        detail.put("note", user.getNote());
        detail.put("createdAt", user.getFormattedCreatedAt());

        if (user.getAssignedTeacher() != null) {
            detail.put("assignedTeacher", user.getAssignedTeacher().getFullName());
        }

        if (user.getRole() == User.Role.STUDENT) {
            List<Enrollment> enrollments = enrollmentRepository.findByStudent(user);
            List<Map<String, String>> courses = enrollments.stream().map(e -> {
                Map<String, String> courseInfo = new HashMap<>();
                courseInfo.put("courseName", e.getCourse().getCourseName());
                courseInfo.put("teacherName", e.getCourse().getTeacher().getFullName());
                return courseInfo;
            }).collect(Collectors.toList());
            detail.put("courses", courses);
        }

        return detail;
    }

    @GetMapping("/admin/users/create")
    public String showCreateUserForm(Model model, Authentication auth) {
        User admin = userRepository.findByUsername(auth.getName());
        model.addAttribute("currentUser", admin);
        model.addAttribute("user", new User());
        model.addAttribute("roles", User.Role.values());
        model.addAttribute("teachers", userRepository.findByRole(User.Role.TEACHER));
        model.addAttribute("allCourses", courseRepository.findAll());
        return "admin/create-user";
    }

    @PostMapping("/admin/users/create")
    public String createUser(@ModelAttribute User user,
                             @RequestParam(required = false) Long assignedTeacherId,
                             @RequestParam(required = false) Long courseId,
                             Authentication auth) {
        User creator = userRepository.findByUsername(auth.getName());
        user.setCreatedBy(creator);

        if (assignedTeacherId != null) {
            user.setAssignedTeacher(userRepository.findById(assignedTeacherId).orElse(null));
        }

        userRepository.save(user);

        if (user.getRole() == User.Role.STUDENT && courseId != null) {
            Enrollment enrollment = new Enrollment();
            enrollment.setStudent(user);
            enrollment.setCourse(courseRepository.findById(courseId).orElse(null));
            enrollmentRepository.save(enrollment);
        }

        return "redirect:/admin/users";
    }

    @GetMapping("/admin/users/edit/{id}")
    public String showEditUserForm(@PathVariable Long id, Model model, Authentication auth) {
        User admin = userRepository.findByUsername(auth.getName());
        User user = userRepository.findById(id).orElseThrow();
        model.addAttribute("currentUser", admin);
        model.addAttribute("user", user);
        model.addAttribute("roles", User.Role.values());
        model.addAttribute("teachers", userRepository.findByRole(User.Role.TEACHER));

        if (user.getAssignedTeacher() != null) {
            model.addAttribute("assignedTeacherCourses",
                    courseRepository.findByTeacher(user.getAssignedTeacher()));
        }
        model.addAttribute("allCourses", courseRepository.findAll());

        return "admin/edit-user";
    }

    @PostMapping("/admin/users/edit/{id}")
    public String updateUser(@PathVariable Long id,
                             @ModelAttribute User updatedUser,
                             @RequestParam(required = false) Long assignedTeacherId,
                             @RequestParam(required = false) Long courseId) {
        User existingUser = userRepository.findById(id).orElseThrow();
        existingUser.setFullName(updatedUser.getFullName());
        existingUser.setEmail(updatedUser.getEmail());
        existingUser.setPhone(updatedUser.getPhone());
        existingUser.setRole(updatedUser.getRole());
        existingUser.setNote(updatedUser.getNote());

        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
            existingUser.setPassword(updatedUser.getPassword());
        }

        if (assignedTeacherId != null) {
            existingUser.setAssignedTeacher(userRepository.findById(assignedTeacherId).orElse(null));
        } else {
            existingUser.setAssignedTeacher(null);
        }

        userRepository.save(existingUser);

        if (existingUser.getRole() == User.Role.STUDENT && courseId != null) {
            Course course = courseRepository.findById(courseId).orElse(null);
            if (!enrollmentRepository.existsByStudentAndCourse(existingUser, course)) {
                Enrollment enrollment = new Enrollment();
                enrollment.setStudent(existingUser);
                enrollment.setCourse(course);
                enrollmentRepository.save(enrollment);
            }
        }

        return "redirect:/admin/users";
    }

    @GetMapping("/admin/users/delete/{id}")
    public String deleteUser(@PathVariable Long id) {
        userRepository.deleteById(id);
        return "redirect:/admin/users";
    }

    // ============ API HELPER ============
    @GetMapping("/admin/getTeachers")
    @ResponseBody
    public List<Map<String, Object>> getTeachers() {
        return userRepository.findByRole(User.Role.TEACHER).stream().map(t -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", t.getId());
            map.put("fullName", t.getFullName());
            return map;
        }).collect(Collectors.toList());
    }

    @GetMapping("/admin/getCoursesByTeacher/{teacherId}")
    @ResponseBody
    public List<Map<String, Object>> getCoursesByTeacher(@PathVariable Long teacherId) {
        User teacher = userRepository.findById(teacherId).orElse(null);
        if (teacher == null) return new ArrayList<>();

        return courseRepository.findByTeacher(teacher).stream().map(c -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", c.getId());
            map.put("courseName", c.getCourseName());
            return map;
        }).collect(Collectors.toList());
    }

    // ============ QUẢN LÝ KHÓA HỌC - ADMIN ============
    @GetMapping("/admin/courses")
    public String adminCourses(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long teacherId,
            Model model, Authentication auth) {

        User admin = userRepository.findByUsername(auth.getName());
        List<Course> courses;

        if (search != null && !search.isEmpty()) {
            if (teacherId != null) {
                User teacher = userRepository.findById(teacherId).orElse(null);
                courses = courseRepository.searchCoursesByTeacher(search, teacher);
            } else {
                courses = courseRepository.searchCourses(search);
            }
        } else if (teacherId != null) {
            User teacher = userRepository.findById(teacherId).orElse(null);
            courses = courseRepository.findByTeacher(teacher);
        } else {
            courses = courseRepository.findAllWithTeacher();
        }

        courses.sort(Comparator.comparing(c -> c.getCourseName() != null ? c.getCourseName().toLowerCase() : ""));

        List<User> teachers = userRepository.findByRole(User.Role.TEACHER);

        model.addAttribute("currentUser", admin);
        model.addAttribute("courses", courses);
        model.addAttribute("teachers", teachers);
        model.addAttribute("searchKeyword", search);
        model.addAttribute("selectedTeacherId", teacherId);

        return "admin/courses";
    }

    @GetMapping("/admin/courses/detail/{id}")
    @ResponseBody
    public Map<String, Object> getCourseDetail(@PathVariable Long id) {
        Course course = courseRepository.findById(id).orElseThrow();
        Map<String, Object> detail = new HashMap<>();
        detail.put("id", course.getId());
        detail.put("courseCode", course.getCourseCode());
        detail.put("courseName", course.getCourseName());
        detail.put("pricePerSession", course.getFormattedPrice());
        detail.put("startDate", course.getFormattedStartDate());
        detail.put("endDate", course.getFormattedEndDate());
        detail.put("paymentStartDate", course.getPaymentStartDate() != null ? course.getPaymentStartDate().toString() : "");
        detail.put("paymentDayOfMonth", course.getPaymentDayDisplay());
        detail.put("teacherName", course.getTeacher().getFullName());
        detail.put("teacherEmail", course.getTeacher().getEmail());
        detail.put("teacherPhone", course.getTeacher().getPhone());
        detail.put("createdAt", course.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        List<Enrollment> enrollments = enrollmentRepository.findByCourse(course);
        detail.put("studentCount", enrollments.size());

        return detail;
    }

    @GetMapping("/admin/courses/create")
    public String showAdminCreateCourseForm(Model model, Authentication auth) {
        User admin = userRepository.findByUsername(auth.getName());
        model.addAttribute("currentUser", admin);
        model.addAttribute("course", new Course());
        model.addAttribute("teachers", userRepository.findByRole(User.Role.TEACHER));
        return "admin/create-course";
    }

    @PostMapping("/admin/courses/create")
    public String adminCreateCourse(@ModelAttribute Course course,
                                    @RequestParam Long teacherId) {
        User teacher = userRepository.findById(teacherId).orElseThrow();
        course.setTeacher(teacher);
        courseRepository.save(course);
        return "redirect:/admin/courses";
    }

    @GetMapping("/admin/courses/edit/{id}")
    public String showAdminEditCourseForm(@PathVariable Long id, Model model, Authentication auth) {
        User admin = userRepository.findByUsername(auth.getName());
        Course course = courseRepository.findById(id).orElseThrow();
        model.addAttribute("currentUser", admin);
        model.addAttribute("course", course);
        model.addAttribute("teachers", userRepository.findByRole(User.Role.TEACHER));
        return "admin/edit-course";
    }

    @PostMapping("/admin/courses/edit/{id}")
    public String adminUpdateCourse(@PathVariable Long id,
                                    @ModelAttribute Course updatedCourse,
                                    @RequestParam Long teacherId) {
        Course course = courseRepository.findById(id).orElseThrow();
        course.setCourseName(updatedCourse.getCourseName());
        course.setPricePerSession(updatedCourse.getPricePerSession());
        course.setStartDate(updatedCourse.getStartDate());
        course.setEndDate(updatedCourse.getEndDate());
        course.setPaymentStartDate(updatedCourse.getPaymentStartDate());
        course.setPaymentDayOfMonth(updatedCourse.getPaymentDayOfMonth());
        course.setTeacher(userRepository.findById(teacherId).orElseThrow());
        courseRepository.save(course);
        return "redirect:/admin/courses";
    }

    @GetMapping("/admin/courses/delete/{id}")
    public String adminDeleteCourse(@PathVariable Long id) {
        courseRepository.deleteById(id);
        return "redirect:/admin/courses";
    }

    // ============ TEACHER PROFILE ============
    @GetMapping("/teacher/profile")
    public String teacherProfile(Model model, Authentication auth) {
        User teacher = userRepository.findByUsername(auth.getName());
        model.addAttribute("currentUser", teacher);
        model.addAttribute("user", teacher);
        model.addAttribute("courses", courseRepository.findByTeacher(teacher));
        return "teacher/profile";
    }

    @PostMapping("/teacher/profile")
    public String updateTeacherProfile(@ModelAttribute User updatedUser, Authentication auth) {
        User teacher = userRepository.findByUsername(auth.getName());
        teacher.setFullName(updatedUser.getFullName());
        teacher.setEmail(updatedUser.getEmail());
        teacher.setPhone(updatedUser.getPhone());
        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
            teacher.setPassword(updatedUser.getPassword());
        }
        userRepository.save(teacher);
        return "redirect:/teacher/profile?success";
    }

    // ============ TEACHER TẠO HỌC SINH ============
    @GetMapping("/teacher/students/create")
    public String showCreateStudentForm(Model model, Authentication auth) {
        User teacher = userRepository.findByUsername(auth.getName());
        model.addAttribute("currentUser", teacher);
        model.addAttribute("user", new User());
        model.addAttribute("myCourses", courseRepository.findByTeacher(teacher));
        return "teacher/create-student";
    }

    @PostMapping("/teacher/students/create")
    public String createStudent(@ModelAttribute User user,
                                @RequestParam(required = false) Long courseId,
                                Authentication auth) {
        User teacher = userRepository.findByUsername(auth.getName());
        user.setRole(User.Role.STUDENT);
        user.setAssignedTeacher(teacher);
        user.setCreatedBy(teacher);
        userRepository.save(user);

        if (courseId != null) {
            Course course = courseRepository.findById(courseId).orElse(null);
            if (course != null && course.getTeacher().getId().equals(teacher.getId())) {
                Enrollment enrollment = new Enrollment();
                enrollment.setStudent(user);
                enrollment.setCourse(course);
                enrollmentRepository.save(enrollment);
            }
        }

        return "redirect:/teacher/courses";
    }

    // ============ QUẢN LÝ KHÓA HỌC - TEACHER ============
    @GetMapping("/teacher/courses")
    public String teacherCourses(
            @RequestParam(required = false) String search,
            Model model, Authentication auth) {

        User teacher = userRepository.findByUsername(auth.getName());
        List<Course> courses;

        if (search != null && !search.isEmpty()) {
            courses = courseRepository.searchCoursesByTeacher(search, teacher);
        } else {
            courses = courseRepository.findByTeacher(teacher);
        }

        courses.sort(Comparator.comparing(c -> c.getCourseName() != null ? c.getCourseName().toLowerCase() : ""));

        model.addAttribute("currentUser", teacher);
        model.addAttribute("courses", courses);
        model.addAttribute("searchKeyword", search);

        return "teacher/courses";
    }

    @GetMapping("/teacher/courses/create")
    public String showTeacherCreateCourseForm(Model model, Authentication auth) {
        User teacher = userRepository.findByUsername(auth.getName());
        model.addAttribute("currentUser", teacher);
        model.addAttribute("course", new Course());
        return "teacher/create-course";
    }

    @PostMapping("/teacher/courses/create")
    public String teacherCreateCourse(@ModelAttribute Course course, Authentication auth) {
        User teacher = userRepository.findByUsername(auth.getName());
        course.setTeacher(teacher);
        courseRepository.save(course);
        return "redirect:/teacher/courses";
    }

    @GetMapping("/teacher/courses/{id}/students")
    public String viewCourseStudents(@PathVariable Long id, Model model, Authentication auth) {
        User teacher = userRepository.findByUsername(auth.getName());
        Course course = courseRepository.findById(id).orElseThrow();

        if (!course.getTeacher().getId().equals(teacher.getId())) {
            return "redirect:/teacher/courses";
        }

        model.addAttribute("currentUser", teacher);
        model.addAttribute("course", course);
        model.addAttribute("enrollments", enrollmentRepository.findByCourse(course));
        return "teacher/course-students";
    }

    @GetMapping("/teacher/courses/edit/{id}")
    public String showTeacherEditCourseForm(@PathVariable Long id, Model model, Authentication auth) {
        User teacher = userRepository.findByUsername(auth.getName());
        Course course = courseRepository.findById(id).orElseThrow();

        if (!course.getTeacher().getId().equals(teacher.getId())) {
            return "redirect:/teacher/courses";
        }

        model.addAttribute("currentUser", teacher);
        model.addAttribute("course", course);
        return "teacher/edit-course";
    }

    @PostMapping("/teacher/courses/edit/{id}")
    public String teacherUpdateCourse(@PathVariable Long id, @ModelAttribute Course updatedCourse) {
        Course course = courseRepository.findById(id).orElseThrow();
        course.setCourseName(updatedCourse.getCourseName());
        course.setPricePerSession(updatedCourse.getPricePerSession());
        course.setStartDate(updatedCourse.getStartDate());
        course.setEndDate(updatedCourse.getEndDate());
        course.setPaymentStartDate(updatedCourse.getPaymentStartDate());
        course.setPaymentDayOfMonth(updatedCourse.getPaymentDayOfMonth());
        courseRepository.save(course);
        return "redirect:/teacher/courses";
    }

    @GetMapping("/teacher/courses/delete/{id}")
    public String teacherDeleteCourse(@PathVariable Long id, Authentication auth) {
        Course course = courseRepository.findById(id).orElseThrow();
        User teacher = userRepository.findByUsername(auth.getName());

        if (course.getTeacher().getId().equals(teacher.getId())) {
            courseRepository.deleteById(id);
        }
        return "redirect:/teacher/courses";
    }

    // ============ STUDENT ============
    @GetMapping("/student/profile")
    public String studentProfile(Model model, Authentication auth) {
        User student = userRepository.findByUsername(auth.getName());
        model.addAttribute("currentUser", student);
        model.addAttribute("user", student);
        model.addAttribute("enrollments", enrollmentRepository.findByStudent(student));
        return "student/profile";
    }

    @PostMapping("/student/profile")
    public String updateStudentProfile(@ModelAttribute User updatedUser, Authentication auth) {
        User student = userRepository.findByUsername(auth.getName());
        student.setFullName(updatedUser.getFullName());
        student.setEmail(updatedUser.getEmail());
        student.setPhone(updatedUser.getPhone());
        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
            student.setPassword(updatedUser.getPassword());
        }
        userRepository.save(student);
        return "redirect:/student/profile?success";
    }

    // ============ QUẢN LÝ NHÓM HỌC - ADMIN ============
    @Autowired
    private StudyGroupRepository studyGroupRepository;

    @Autowired
    private GroupMembershipRepository groupMembershipRepository;

    @Autowired
    private ExtraSessionRepository extraSessionRepository;

    @GetMapping("/admin/groups")
    public String adminGroups(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Long courseId,
            Model model, Authentication auth) {

        User admin = userRepository.findByUsername(auth.getName());
        List<StudyGroup> groups;

        if (search != null && !search.isEmpty()) {
            groups = studyGroupRepository.searchGroups(search);
        } else if (courseId != null) {
            Course course = courseRepository.findById(courseId).orElse(null);
            if (teacherId != null) {
                User teacher = userRepository.findById(teacherId).orElse(null);
                groups = studyGroupRepository.findByTeacherAndCourse(teacher, course);
            } else {
                groups = studyGroupRepository.findByCourse(course);
            }
        } else if (teacherId != null) {
            User teacher = userRepository.findById(teacherId).orElse(null);
            groups = studyGroupRepository.findByTeacher(teacher);
        } else {
            groups = studyGroupRepository.findAllWithDetails();
        }

        groups.sort(Comparator.comparing(g -> g.getGroupName() != null ? g.getGroupName().toLowerCase() : ""));

        List<User> teachers = userRepository.findByRole(User.Role.TEACHER);
        List<Course> allCourses = courseRepository.findAll();
        List<Course> filteredCourses = new ArrayList<>();

        if (teacherId != null) {
            User teacher = userRepository.findById(teacherId).orElse(null);
            if (teacher != null) {
                filteredCourses = courseRepository.findByTeacher(teacher);
            }
        }

        model.addAttribute("currentUser", admin);
        model.addAttribute("groups", groups);
        model.addAttribute("teachers", teachers);
        model.addAttribute("allCourses", allCourses);
        model.addAttribute("filteredCourses", filteredCourses);
        model.addAttribute("searchKeyword", search);
        model.addAttribute("selectedTeacherId", teacherId);
        model.addAttribute("selectedCourseId", courseId);

        return "admin/groups";
    }

    @GetMapping("/admin/groups/detail/{id}")
    @ResponseBody
    public Map<String, Object> getGroupDetail(@PathVariable Long id) {
        StudyGroup group = studyGroupRepository.findById(id).orElseThrow();
        Map<String, Object> detail = new HashMap<>();
        detail.put("id", group.getId());
        detail.put("groupName", group.getGroupName());
        detail.put("description", group.getDescription());
        detail.put("courseName", group.getCourse().getCourseName());
        detail.put("teacherName", group.getTeacher().getFullName());
        detail.put("createdAt", group.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        List<GroupMembership> members = groupMembershipRepository.findMembersByGroup(group);
        detail.put("memberCount", members.size());

        List<Map<String, Object>> memberList = members.stream().map(m -> {
            Map<String, Object> member = new HashMap<>();
            member.put("id", m.getId());
            member.put("studentId", m.getStudent().getId());
            member.put("studentName", m.getStudent().getFullName());
            member.put("joinedAt", m.getJoinedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            return member;
        }).collect(Collectors.toList());
        detail.put("members", memberList);

        return detail;
    }

    @GetMapping("/admin/groups/create")
    public String showAdminCreateGroupForm(Model model, Authentication auth) {
        User admin = userRepository.findByUsername(auth.getName());
        model.addAttribute("currentUser", admin);
        model.addAttribute("group", new StudyGroup());
        model.addAttribute("teachers", userRepository.findByRole(User.Role.TEACHER));
        model.addAttribute("allCourses", courseRepository.findAll());
        return "admin/create-group";
    }

    @PostMapping("/admin/groups/create")
    public String adminCreateGroup(@ModelAttribute StudyGroup group,
                                   @RequestParam Long teacherId,
                                   @RequestParam Long courseId) {
        group.setTeacher(userRepository.findById(teacherId).orElseThrow());
        group.setCourse(courseRepository.findById(courseId).orElseThrow());
        studyGroupRepository.save(group);
        return "redirect:/admin/groups";
    }

    @GetMapping("/admin/groups/edit/{id}")
    public String showAdminEditGroupForm(@PathVariable Long id, Model model, Authentication auth) {
        User admin = userRepository.findByUsername(auth.getName());
        StudyGroup group = studyGroupRepository.findById(id).orElseThrow();
        model.addAttribute("currentUser", admin);
        model.addAttribute("group", group);
        model.addAttribute("teachers", userRepository.findByRole(User.Role.TEACHER));
        model.addAttribute("allCourses", courseRepository.findAll());

        // Load courses của teacher hiện tại
        List<Course> teacherCourses = courseRepository.findByTeacher(group.getTeacher());
        model.addAttribute("teacherCourses", teacherCourses);

        return "admin/edit-group";
    }

    @PostMapping("/admin/groups/edit/{id}")
    public String adminUpdateGroup(@PathVariable Long id,
                                   @ModelAttribute StudyGroup updatedGroup,
                                   @RequestParam Long teacherId,
                                   @RequestParam Long courseId) {
        StudyGroup group = studyGroupRepository.findById(id).orElseThrow();
        group.setGroupName(updatedGroup.getGroupName());
        group.setDescription(updatedGroup.getDescription());
        group.setTeacher(userRepository.findById(teacherId).orElseThrow());
        group.setCourse(courseRepository.findById(courseId).orElseThrow());
        studyGroupRepository.save(group);
        return "redirect:/admin/groups";
    }

    @GetMapping("/admin/groups/delete/{id}")
    public String adminDeleteGroup(@PathVariable Long id) {
        studyGroupRepository.deleteById(id);
        return "redirect:/admin/groups";
    }

    @GetMapping("/admin/groups/{id}/members")
    public String adminGroupMembers(@PathVariable Long id, Model model, Authentication auth) {
        User admin = userRepository.findByUsername(auth.getName());
        StudyGroup group = studyGroupRepository.findById(id).orElseThrow();
        List<GroupMembership> members = groupMembershipRepository.findMembersByGroup(group);

        // Học sinh chưa trong nhóm này
        List<User> allStudents = userRepository.findByRole(User.Role.STUDENT);
        List<User> availableStudents = allStudents.stream()
                .filter(s -> members.stream().noneMatch(m -> m.getStudent().getId().equals(s.getId())))
                .collect(Collectors.toList());

        model.addAttribute("currentUser", admin);
        model.addAttribute("group", group);
        model.addAttribute("members", members);
        model.addAttribute("availableStudents", availableStudents);

        return "admin/group-members";
    }

    @PostMapping("/admin/groups/{id}/add-student")
    public String addStudentToGroup(@PathVariable Long id, @RequestParam Long studentId) {
        StudyGroup group = studyGroupRepository.findById(id).orElseThrow();
        User student = userRepository.findById(studentId).orElseThrow();

        if (!groupMembershipRepository.existsByStudentAndStudyGroup(student, group)) {
            GroupMembership membership = new GroupMembership();
            membership.setStudent(student);
            membership.setStudyGroup(group);
            groupMembershipRepository.save(membership);
        }

        return "redirect:/admin/groups/" + id + "/members";
    }

    @GetMapping("/admin/groups/remove-member/{membershipId}")
    public String removeMemberFromGroup(@PathVariable Long membershipId) {
        GroupMembership membership = groupMembershipRepository.findById(membershipId).orElseThrow();
        Long groupId = membership.getStudyGroup().getId();
        groupMembershipRepository.deleteById(membershipId);
        return "redirect:/admin/groups/" + groupId + "/members";
    }

    @PostMapping("/admin/groups/transfer-student")
    public String transferStudent(@RequestParam Long studentId,
                                  @RequestParam Long fromGroupId,
                                  @RequestParam Long toGroupId) {

        // Xóa khỏi nhóm cũ
        List<GroupMembership> memberships = groupMembershipRepository.findByStudent(
                userRepository.findById(studentId).orElseThrow());

        memberships.stream()
                .filter(m -> m.getStudyGroup().getId().equals(fromGroupId))
                .findFirst()
                .ifPresent(m -> groupMembershipRepository.deleteById(m.getId()));

        // Thêm vào nhóm mới
        StudyGroup toGroup = studyGroupRepository.findById(toGroupId).orElseThrow();
        if (!groupMembershipRepository.existsByStudentAndStudyGroup(
                userRepository.findById(studentId).orElseThrow(), toGroup)) {
            GroupMembership newMembership = new GroupMembership();
            newMembership.setStudent(userRepository.findById(studentId).orElseThrow());
            newMembership.setStudyGroup(toGroup);
            groupMembershipRepository.save(newMembership);
        }

        return "redirect:/admin/groups/" + fromGroupId + "/members";
    }

    // ============ QUẢN LÝ NHÓM HỌC - TEACHER ============
    @GetMapping("/teacher/groups")
    public String teacherGroups(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long courseId,
            Model model, Authentication auth) {

        User teacher = userRepository.findByUsername(auth.getName());
        List<StudyGroup> groups;

        if (search != null && !search.isEmpty()) {
            groups = studyGroupRepository.searchGroups(search);
            // Lọc chỉ lấy group của teacher này
            groups = groups.stream()
                    .filter(g -> g.getTeacher().getId().equals(teacher.getId()))
                    .collect(Collectors.toList());
        } else if (courseId != null) {
            Course course = courseRepository.findById(courseId).orElse(null);
            groups = studyGroupRepository.findByTeacherAndCourse(teacher, course);
        } else {
            groups = studyGroupRepository.findByTeacherWithCourse(teacher);
        }

        groups.sort(Comparator.comparing(g -> g.getGroupName() != null ? g.getGroupName().toLowerCase() : ""));

        List<Course> myCourses = courseRepository.findByTeacher(teacher);

        model.addAttribute("currentUser", teacher);
        model.addAttribute("groups", groups);
        model.addAttribute("myCourses", myCourses);
        model.addAttribute("searchKeyword", search);
        model.addAttribute("selectedCourseId", courseId);

        return "teacher/groups";
    }

    @GetMapping("/teacher/groups/create")
    public String showTeacherCreateGroupForm(Model model, Authentication auth) {
        User teacher = userRepository.findByUsername(auth.getName());
        model.addAttribute("currentUser", teacher);
        model.addAttribute("group", new StudyGroup());
        model.addAttribute("myCourses", courseRepository.findByTeacher(teacher));
        return "teacher/create-group";
    }

    @PostMapping("/teacher/groups/create")
    public String teacherCreateGroup(@ModelAttribute StudyGroup group,
                                     @RequestParam Long courseId,
                                     Authentication auth) {
        User teacher = userRepository.findByUsername(auth.getName());
        group.setTeacher(teacher);
        group.setCourse(courseRepository.findById(courseId).orElseThrow());
        studyGroupRepository.save(group);
        return "redirect:/teacher/groups";
    }

    @GetMapping("/teacher/groups/{id}/members")
    public String teacherGroupMembers(@PathVariable Long id, Model model, Authentication auth) {
        User teacher = userRepository.findByUsername(auth.getName());
        StudyGroup group = studyGroupRepository.findById(id).orElseThrow();

        if (!group.getTeacher().getId().equals(teacher.getId())) {
            return "redirect:/teacher/groups";
        }

        List<GroupMembership> members = groupMembershipRepository.findMembersByGroup(group);
        List<User> allStudents = userRepository.findByRole(User.Role.STUDENT);
        List<User> availableStudents = allStudents.stream()
                .filter(s -> members.stream().noneMatch(m -> m.getStudent().getId().equals(s.getId())))
                .collect(Collectors.toList());

        model.addAttribute("currentUser", teacher);
        model.addAttribute("group", group);
        model.addAttribute("members", members);
        model.addAttribute("availableStudents", availableStudents);

        return "teacher/group-members";
    }


}
