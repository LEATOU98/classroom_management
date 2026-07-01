package com.example.classroom_management.controller;

import com.example.classroom_management.entity.*;
import com.example.classroom_management.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
}

//package com.example.classroom_management.controller;
//
//import com.example.classroom_management.entity.*;
//import com.example.classroom_management.repository.*;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.core.Authentication;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.*;
//
//import java.time.format.DateTimeFormatter;
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Controller
//public class MainController {
//
//    @Autowired
//    private UserRepository userRepository;
//
//    @Autowired
//    private CourseRepository courseRepository;
//
//    @Autowired
//    private EnrollmentRepository enrollmentRepository;
//
//    @Autowired
//    private SessionRepository sessionRepository;
//
//    // ============ LOGIN & DASHBOARD ============
//    @GetMapping("/login")
//    public String login() {
//        return "login";
//    }
//
//    @GetMapping("/dashboard")
//    public String dashboard(Authentication auth) {
//        User currentUser = userRepository.findByUsername(auth.getName());
//        switch (currentUser.getRole()) {
//            case ADMIN: return "redirect:/admin/profile";
//            case TEACHER: return "redirect:/teacher/profile";
//            case STUDENT: return "redirect:/student/profile";
//            default: return "redirect:/login";
//        }
//    }
//
//    // ============ ADMIN ============
//    @GetMapping("/admin/profile")
//    public String adminProfile(Model model, Authentication auth) {
//        User admin = userRepository.findByUsername(auth.getName());
//        model.addAttribute("currentUser", admin);
//        model.addAttribute("user", admin);
//        return "admin/profile";
//    }
//
//    @PostMapping("/admin/profile")
//    public String updateAdminProfile(@ModelAttribute User updatedUser, Authentication auth) {
//        User admin = userRepository.findByUsername(auth.getName());
//        admin.setFullName(updatedUser.getFullName());
//        admin.setEmail(updatedUser.getEmail());
//        admin.setPhone(updatedUser.getPhone());
//        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
//            admin.setPassword(updatedUser.getPassword());
//        }
//        userRepository.save(admin);
//        return "redirect:/admin/profile?success";
//    }
//
//    @GetMapping("/admin/users")
//    public String adminUsers(
//            @RequestParam(required = false) String role,
//            @RequestParam(required = false) String search,
//            @RequestParam(required = false) Long teacherId,
//            @RequestParam(required = false) Long courseId,
//            Model model, Authentication auth) {
//
//        User admin = userRepository.findByUsername(auth.getName());
//        List<User> users;
//
//        // Logic lọc thông minh
//        if (search != null && !search.isEmpty()) {
//            if (role != null && !role.isEmpty() && !role.equals("ALL")) {
//                users = userRepository.searchUsersByRole(search, User.Role.valueOf(role));
//            } else {
//                users = userRepository.searchUsers(search);
//            }
//        } else if ("STUDENT".equals(role) && teacherId != null) {
//            User teacher = userRepository.findById(teacherId).orElse(null);
//            if (courseId != null) {
//                Course course = courseRepository.findById(courseId).orElse(null);
//                users = userRepository.findStudentsByCourse(course);
//            } else {
//                users = userRepository.findStudentsByTeacher(teacher);
//            }
//        } else if (role != null && !role.isEmpty() && !role.equals("ALL")) {
//            users = userRepository.findByRole(User.Role.valueOf(role));
//        } else {
//            users = userRepository.findAllOrderByFullName();
//        }
//
//        // Luôn sắp xếp A-Z
//        if (!users.isEmpty()) {
//            users.sort(Comparator.comparing(u ->
//                    u.getFullName() != null ? u.getLastName().toLowerCase() : ""));
//        }
//
//        List<User> teachers = userRepository.findByRole(User.Role.TEACHER);
//        List<Course> filteredCourses = new ArrayList<>();
//        if (teacherId != null) {
//            User selectedTeacher = userRepository.findById(teacherId).orElse(null);
//            if (selectedTeacher != null) {
//                filteredCourses = courseRepository.findByTeacher(selectedTeacher);
//            }
//        }
//
//        model.addAttribute("currentUser", admin);
//        model.addAttribute("users", users);
//        model.addAttribute("teachers", teachers);
//        model.addAttribute("filteredCourses", filteredCourses);
//        model.addAttribute("selectedRole", role);
//        model.addAttribute("searchKeyword", search);
//        model.addAttribute("selectedTeacherId", teacherId);
//        model.addAttribute("selectedCourseId", courseId);
//        model.addAttribute("roles", User.Role.values());
//
//        return "admin/users";
//    }
//
//    @GetMapping("/admin/users/{id}")
//    @ResponseBody
//    public Map<String, Object> getUserDetail(@PathVariable Long id) {
//        User user = userRepository.findById(id).orElseThrow();
//        Map<String, Object> detail = new HashMap<>();
//        detail.put("id", user.getId());
//        detail.put("username", user.getUsername());
//        detail.put("fullName", user.getFullName());
//        detail.put("email", user.getEmail());
//        detail.put("phone", user.getPhone());
//        detail.put("role", user.getRole().name());
//        detail.put("note", user.getNote());
//        detail.put("createdAt", user.getFormattedCreatedAt());
//
//        if (user.getAssignedTeacher() != null) {
//            detail.put("assignedTeacher", user.getAssignedTeacher().getFullName());
//        }
//
//        if (user.getRole() == User.Role.STUDENT) {
//            List<Enrollment> enrollments = enrollmentRepository.findByStudent(user);
//            List<Map<String, String>> courses = enrollments.stream().map(e -> {
//                Map<String, String> courseInfo = new HashMap<>();
//                courseInfo.put("courseName", e.getCourse().getCourseName());
//                courseInfo.put("teacherName", e.getCourse().getTeacher().getFullName());
//                return courseInfo;
//            }).collect(Collectors.toList());
//            detail.put("courses", courses);
//        }
//
//        return detail;
//    }
//
//    @GetMapping("/admin/users/create")
//    public String showCreateUserForm(Model model, Authentication auth) {
//        User admin = userRepository.findByUsername(auth.getName());
//        model.addAttribute("currentUser", admin);
//        model.addAttribute("user", new User());
//        model.addAttribute("roles", User.Role.values());
//        model.addAttribute("teachers", userRepository.findByRole(User.Role.TEACHER));
//        model.addAttribute("allCourses", courseRepository.findAll());
//        return "admin/create-user";
//    }
//
//    @PostMapping("/admin/users/create")
//    public String createUser(@ModelAttribute User user,
//                             @RequestParam(required = false) Long assignedTeacherId,
//                             @RequestParam(required = false) Long courseId,
//                             Authentication auth) {
//        User creator = userRepository.findByUsername(auth.getName());
//        user.setCreatedBy(creator);
//
//        if (assignedTeacherId != null) {
//            user.setAssignedTeacher(userRepository.findById(assignedTeacherId).orElse(null));
//        }
//
//        userRepository.save(user);
//
//        // Nếu là student và có chọn khóa học, tự động enroll
//        if (user.getRole() == User.Role.STUDENT && courseId != null) {
//            Enrollment enrollment = new Enrollment();
//            enrollment.setStudent(user);
//            enrollment.setCourse(courseRepository.findById(courseId).orElse(null));
//            enrollmentRepository.save(enrollment);
//        }
//
//        return "redirect:/admin/users";
//    }
//
//    @GetMapping("/admin/users/edit/{id}")
//    public String showEditUserForm(@PathVariable Long id, Model model, Authentication auth) {
//        User admin = userRepository.findByUsername(auth.getName());
//        User user = userRepository.findById(id).orElseThrow();
//        model.addAttribute("currentUser", admin);
//        model.addAttribute("user", user);
//        model.addAttribute("roles", User.Role.values());
//        model.addAttribute("teachers", userRepository.findByRole(User.Role.TEACHER));
//
//        // Load courses của assigned teacher
//        if (user.getAssignedTeacher() != null) {
//            model.addAttribute("assignedTeacherCourses",
//                    courseRepository.findByTeacher(user.getAssignedTeacher()));
//        }
//        model.addAttribute("allCourses", courseRepository.findAll());
//
//        return "admin/edit-user";
//    }
//
//    @PostMapping("/admin/users/edit/{id}")
//    public String updateUser(@PathVariable Long id,
//                             @ModelAttribute User updatedUser,
//                             @RequestParam(required = false) Long assignedTeacherId,
//                             @RequestParam(required = false) Long courseId) {
//        User existingUser = userRepository.findById(id).orElseThrow();
//        existingUser.setFullName(updatedUser.getFullName());
//        existingUser.setEmail(updatedUser.getEmail());
//        existingUser.setPhone(updatedUser.getPhone());
//        existingUser.setRole(updatedUser.getRole());
//        existingUser.setNote(updatedUser.getNote());
//
//        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
//            existingUser.setPassword(updatedUser.getPassword());
//        }
//
//        if (assignedTeacherId != null) {
//            existingUser.setAssignedTeacher(userRepository.findById(assignedTeacherId).orElse(null));
//        } else {
//            existingUser.setAssignedTeacher(null);
//        }
//
//        userRepository.save(existingUser);
//
//        // Cập nhật enrollment nếu có chọn course
//        if (existingUser.getRole() == User.Role.STUDENT && courseId != null) {
//            Course course = courseRepository.findById(courseId).orElse(null);
//            if (!enrollmentRepository.existsByStudentAndCourse(existingUser, course)) {
//                Enrollment enrollment = new Enrollment();
//                enrollment.setStudent(existingUser);
//                enrollment.setCourse(course);
//                enrollmentRepository.save(enrollment);
//            }
//        }
//
//        return "redirect:/admin/users";
//    }
//
//    @GetMapping("/admin/users/delete/{id}")
//    public String deleteUser(@PathVariable Long id) {
//        userRepository.deleteById(id);
//        return "redirect:/admin/users";
//    }
//
//    @GetMapping("/admin/courses")
//    public String adminViewAllCourses(Model model, Authentication auth) {
//        User admin = userRepository.findByUsername(auth.getName());
//        model.addAttribute("currentUser", admin);
//        model.addAttribute("courses", courseRepository.findAll());
//        return "admin/courses";
//    }
//
//    @GetMapping("/admin/getTeachers")
//    @ResponseBody
//    public List<Map<String, Object>> getTeachers() {
//        return userRepository.findByRole(User.Role.TEACHER).stream().map(t -> {
//            Map<String, Object> map = new HashMap<>();
//            map.put("id", t.getId());
//            map.put("fullName", t.getFullName());
//            return map;
//        }).collect(Collectors.toList());
//    }
//
//    @GetMapping("/admin/getCoursesByTeacher/{teacherId}")
//    @ResponseBody
//    public List<Map<String, Object>> getCoursesByTeacher(@PathVariable Long teacherId) {
//        User teacher = userRepository.findById(teacherId).orElse(null);
//        if (teacher == null) return new ArrayList<>();
//
//        return courseRepository.findByTeacher(teacher).stream().map(c -> {
//            Map<String, Object> map = new HashMap<>();
//            map.put("id", c.getId());
//            map.put("courseName", c.getCourseName());
//            return map;
//        }).collect(Collectors.toList());
//    }
//
//    // ============ TEACHER ============
//    @GetMapping("/teacher/profile")
//    public String teacherProfile(Model model, Authentication auth) {
//        User teacher = userRepository.findByUsername(auth.getName());
//        model.addAttribute("currentUser", teacher);
//        model.addAttribute("user", teacher);
//        model.addAttribute("courses", courseRepository.findByTeacher(teacher));
//        return "teacher/profile";
//    }
//
//    @PostMapping("/teacher/profile")
//    public String updateTeacherProfile(@ModelAttribute User updatedUser, Authentication auth) {
//        User teacher = userRepository.findByUsername(auth.getName());
//        teacher.setFullName(updatedUser.getFullName());
//        teacher.setEmail(updatedUser.getEmail());
//        teacher.setPhone(updatedUser.getPhone());
//        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
//            teacher.setPassword(updatedUser.getPassword());
//        }
//        userRepository.save(teacher);
//        return "redirect:/teacher/profile?success";
//    }
//
//    @GetMapping("/teacher/students/create")
//    public String showCreateStudentForm(Model model, Authentication auth) {
//        User teacher = userRepository.findByUsername(auth.getName());
//        model.addAttribute("currentUser", teacher);
//        model.addAttribute("user", new User());
//        model.addAttribute("myCourses", courseRepository.findByTeacher(teacher));
//        return "teacher/create-student";
//    }
//
//    @PostMapping("/teacher/students/create")
//    public String createStudent(@ModelAttribute User user,
//                                @RequestParam(required = false) Long courseId,
//                                Authentication auth) {
//        User teacher = userRepository.findByUsername(auth.getName());
//        user.setRole(User.Role.STUDENT);
//        user.setAssignedTeacher(teacher);
//        user.setCreatedBy(teacher);
//        userRepository.save(user);
//
//        if (courseId != null) {
//            Course course = courseRepository.findById(courseId).orElse(null);
//            if (course != null && course.getTeacher().getId().equals(teacher.getId())) {
//                Enrollment enrollment = new Enrollment();
//                enrollment.setStudent(user);
//                enrollment.setCourse(course);
//                enrollmentRepository.save(enrollment);
//            }
//        }
//
//        return "redirect:/teacher/courses";
//    }
//
//    @GetMapping("/teacher/courses/create")
//    public String showTeacherCreateCourseForm(Model model, Authentication auth) {
//        User teacher = userRepository.findByUsername(auth.getName());
//        model.addAttribute("currentUser", teacher);
//        model.addAttribute("course", new Course());
//        return "teacher/create-course";
//    }
//
//    @PostMapping("/teacher/courses/create")
//    public String createCourse(@ModelAttribute Course course, Authentication auth) {
//        User teacher = userRepository.findByUsername(auth.getName());
//        course.setTeacher(teacher);
//        courseRepository.save(course);
//        return "redirect:/teacher/courses";
//    }
//
//    @GetMapping("/teacher/courses/{id}/students")
//    public String viewCourseStudents(@PathVariable Long id, Model model, Authentication auth) {
//        User teacher = userRepository.findByUsername(auth.getName());
//        Course course = courseRepository.findById(id).orElseThrow();
//
//        if (!course.getTeacher().getId().equals(teacher.getId())) {
//            return "redirect:/teacher/courses";
//        }
//
//        model.addAttribute("currentUser", teacher);
//        model.addAttribute("course", course);
//        model.addAttribute("enrollments", enrollmentRepository.findByCourse(course));
//        return "teacher/course-students";
//    }
//
//    // ============ STUDENT ============
//    @GetMapping("/student/profile")
//    public String studentProfile(Model model, Authentication auth) {
//        User student = userRepository.findByUsername(auth.getName());
//        model.addAttribute("currentUser", student);
//        model.addAttribute("user", student);
//        model.addAttribute("enrollments", enrollmentRepository.findByStudent(student));
//        return "student/profile";
//    }
//
//    @PostMapping("/student/profile")
//    public String updateStudentProfile(@ModelAttribute User updatedUser, Authentication auth) {
//        User student = userRepository.findByUsername(auth.getName());
//        student.setFullName(updatedUser.getFullName());
//        student.setEmail(updatedUser.getEmail());
//        student.setPhone(updatedUser.getPhone());
//        if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
//            student.setPassword(updatedUser.getPassword());
//        }
//        userRepository.save(student);
//        return "redirect:/student/profile?success";
//    }
//
//
//
//    // ============ QUẢN LÝ KHÓA HỌC - ADMIN ============
//    @GetMapping("/admin/courses")
//    public String adminCourses(
//            @RequestParam(required = false) String search,
//            @RequestParam(required = false) Long teacherId,
//            Model model, Authentication auth) {
//
//        User admin = userRepository.findByUsername(auth.getName());
//        List<Course> courses;
//
//        if (search != null && !search.isEmpty()) {
//            if (teacherId != null) {
//                User teacher = userRepository.findById(teacherId).orElse(null);
//                courses = courseRepository.searchCoursesByTeacher(search, teacher);
//            } else {
//                courses = courseRepository.searchCourses(search);
//            }
//        } else if (teacherId != null) {
//            User teacher = userRepository.findById(teacherId).orElse(null);
//            courses = courseRepository.findByTeacher(teacher);
//        } else {
//            courses = courseRepository.findAllWithTeacher();
//        }
//
//        // Sắp xếp theo tên khóa học
//        courses.sort(Comparator.comparing(c -> c.getCourseName() != null ? c.getCourseName().toLowerCase() : ""));
//
//        List<User> teachers = userRepository.findByRole(User.Role.TEACHER);
//
//        model.addAttribute("currentUser", admin);
//        model.addAttribute("courses", courses);
//        model.addAttribute("teachers", teachers);
//        model.addAttribute("searchKeyword", search);
//        model.addAttribute("selectedTeacherId", teacherId);
//
//        return "admin/courses";
//    }
//
//    @GetMapping("/admin/courses/detail/{id}")
//    @ResponseBody
//    public Map<String, Object> getCourseDetail(@PathVariable Long id) {
//        Course course = courseRepository.findById(id).orElseThrow();
//        Map<String, Object> detail = new HashMap<>();
//        detail.put("id", course.getId());
//        detail.put("courseCode", course.getCourseCode());
//        detail.put("courseName", course.getCourseName());
//        detail.put("pricePerSession", course.getFormattedPrice());
//        detail.put("startDate", course.getFormattedStartDate());
//        detail.put("endDate", course.getFormattedEndDate());
//        detail.put("paymentStartDate", course.getPaymentStartDate() != null ? course.getPaymentStartDate().toString() : "");
//        detail.put("paymentDayOfMonth", course.getPaymentDayDisplay());
//        detail.put("teacherName", course.getTeacher().getFullName());
//        detail.put("teacherEmail", course.getTeacher().getEmail());
//        detail.put("teacherPhone", course.getTeacher().getPhone());
//        detail.put("createdAt", course.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
//
//        // Số lượng học viên
//        List<Enrollment> enrollments = enrollmentRepository.findByCourse(course);
//        detail.put("studentCount", enrollments.size());
//
//        return detail;
//    }
//
//    @GetMapping("/admin/courses/create")
//    public String showAdminCreateCourseForm(Model model, Authentication auth) {
//        User admin = userRepository.findByUsername(auth.getName());
//        model.addAttribute("currentUser", admin);
//        model.addAttribute("course", new Course());
//        model.addAttribute("teachers", userRepository.findByRole(User.Role.TEACHER));
//        return "admin/create-course";
//    }
//
//    @PostMapping("/admin/courses/create")
//    public String adminCreateCourse(@ModelAttribute Course course,
//                                    @RequestParam Long teacherId) {
//        User teacher = userRepository.findById(teacherId).orElseThrow();
//        course.setTeacher(teacher);
//        courseRepository.save(course);
//        return "redirect:/admin/courses";
//    }
//
//    @GetMapping("/admin/courses/edit/{id}")
//    public String showAdminEditCourseForm(@PathVariable Long id, Model model, Authentication auth) {
//        User admin = userRepository.findByUsername(auth.getName());
//        Course course = courseRepository.findById(id).orElseThrow();
//        model.addAttribute("currentUser", admin);
//        model.addAttribute("course", course);
//        model.addAttribute("teachers", userRepository.findByRole(User.Role.TEACHER));
//        return "admin/edit-course";
//    }
//
//    @PostMapping("/admin/courses/edit/{id}")
//    public String adminUpdateCourse(@PathVariable Long id,
//                                    @ModelAttribute Course updatedCourse,
//                                    @RequestParam Long teacherId) {
//        Course course = courseRepository.findById(id).orElseThrow();
//        course.setCourseName(updatedCourse.getCourseName());
//        course.setPricePerSession(updatedCourse.getPricePerSession());
//        course.setStartDate(updatedCourse.getStartDate());
//        course.setEndDate(updatedCourse.getEndDate());
//        course.setPaymentStartDate(updatedCourse.getPaymentStartDate());
//        course.setPaymentDayOfMonth(updatedCourse.getPaymentDayOfMonth());
//        course.setTeacher(userRepository.findById(teacherId).orElseThrow());
//        courseRepository.save(course);
//        return "redirect:/admin/courses";
//    }
//
//    @GetMapping("/admin/courses/delete/{id}")
//    public String adminDeleteCourse(@PathVariable Long id) {
//        courseRepository.deleteById(id);
//        return "redirect:/admin/courses";
//    }
//
//    // ============ QUẢN LÝ KHÓA HỌC - TEACHER ============
//    @GetMapping("/teacher/courses")
//    public String teacherCourses(
//            @RequestParam(required = false) String search,
//            Model model, Authentication auth) {
//
//        User teacher = userRepository.findByUsername(auth.getName());
//        List<Course> courses;
//
//        if (search != null && !search.isEmpty()) {
//            courses = courseRepository.searchCoursesByTeacher(search, teacher);
//        } else {
//            courses = courseRepository.findByTeacher(teacher);
//        }
//
//        courses.sort(Comparator.comparing(c -> c.getCourseName() != null ? c.getCourseName().toLowerCase() : ""));
//
//        model.addAttribute("currentUser", teacher);
//        model.addAttribute("courses", courses);
//        model.addAttribute("searchKeyword", search);
//
//        return "teacher/courses";
//    }
//
//
//
//    @PostMapping("/teacher/courses/create")
//    public String teacherCreateCourse(@ModelAttribute Course course, Authentication auth) {
//        User teacher = userRepository.findByUsername(auth.getName());
//        course.setTeacher(teacher);
//        courseRepository.save(course);
//        return "redirect:/teacher/courses";
//    }
//
//    @GetMapping("/teacher/courses/edit/{id}")
//    public String showTeacherEditCourseForm(@PathVariable Long id, Model model, Authentication auth) {
//        User teacher = userRepository.findByUsername(auth.getName());
//        Course course = courseRepository.findById(id).orElseThrow();
//
//        if (!course.getTeacher().getId().equals(teacher.getId())) {
//            return "redirect:/teacher/courses";
//        }
//
//        model.addAttribute("currentUser", teacher);
//        model.addAttribute("course", course);
//        return "teacher/edit-course";
//    }
//
//    @PostMapping("/teacher/courses/edit/{id}")
//    public String teacherUpdateCourse(@PathVariable Long id, @ModelAttribute Course updatedCourse) {
//        Course course = courseRepository.findById(id).orElseThrow();
//        course.setCourseName(updatedCourse.getCourseName());
//        course.setPricePerSession(updatedCourse.getPricePerSession());
//        course.setStartDate(updatedCourse.getStartDate());
//        course.setEndDate(updatedCourse.getEndDate());
//        course.setPaymentStartDate(updatedCourse.getPaymentStartDate());
//        course.setPaymentDayOfMonth(updatedCourse.getPaymentDayOfMonth());
//        courseRepository.save(course);
//        return "redirect:/teacher/courses";
//    }
//
//    @GetMapping("/teacher/courses/delete/{id}")
//    public String teacherDeleteCourse(@PathVariable Long id, Authentication auth) {
//        Course course = courseRepository.findById(id).orElseThrow();
//        User teacher = userRepository.findByUsername(auth.getName());
//
//        if (course.getTeacher().getId().equals(teacher.getId())) {
//            courseRepository.deleteById(id);
//        }
//        return "redirect:/teacher/courses";
//    }
//}