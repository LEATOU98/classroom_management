package com.example.classroom_management.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class SidebarInterceptor implements HandlerInterceptor {

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                           Object handler, ModelAndView modelAndView) throws Exception {

        if (modelAndView == null) return;

        String requestURI = request.getRequestURI();
        String sidebarCurrentPage = "";
        String sidebarSubPage = "";

        // Admin routes
        if (requestURI.startsWith("/admin/")) {
            if (requestURI.equals("/admin/profile")) {
                sidebarCurrentPage = "profile";
            }
            else if (requestURI.startsWith("/admin/users")) {
                sidebarCurrentPage = "users";
                if (requestURI.equals("/admin/users")) sidebarSubPage = "users-list";
                else if (requestURI.contains("/create")) sidebarSubPage = "users-create";
                else sidebarSubPage = "users-list";
            }
            else if (requestURI.startsWith("/admin/students")) {
                sidebarCurrentPage = "students";
                sidebarSubPage = "students-list";
            }
            else if (requestURI.startsWith("/admin/courses")) {
                sidebarCurrentPage = "courses";
                if (requestURI.equals("/admin/courses")) sidebarSubPage = "courses-list";
                else if (requestURI.contains("/create")) sidebarSubPage = "courses-create";
                else sidebarSubPage = "courses-list";
            }
            else if (requestURI.startsWith("/admin/groups")) {
                sidebarCurrentPage = "groups";
                if (requestURI.equals("/admin/groups")) sidebarSubPage = "groups-list";
                else if (requestURI.contains("/create")) sidebarSubPage = "groups-create";
                else sidebarSubPage = "groups-list";
            }
            // ===== THÊM: Admin Exams =====
            else if (requestURI.startsWith("/admin/exams")) {
                sidebarCurrentPage = "exams";
                if (requestURI.equals("/admin/exams")) sidebarSubPage = "exams-list";
                else if (requestURI.contains("/create")) sidebarSubPage = "exams-create";
                else sidebarSubPage = "exams-list";
            }
        }
        // Teacher routes
        else if (requestURI.startsWith("/teacher/")) {
            if (requestURI.equals("/teacher/profile")) {
                sidebarCurrentPage = "profile";
            }
            else if (requestURI.startsWith("/teacher/courses")) {
                sidebarCurrentPage = "courses";
                if (requestURI.equals("/teacher/courses")) sidebarSubPage = "courses-list";
                else if (requestURI.contains("/create")) sidebarSubPage = "courses-create";
                else sidebarSubPage = "courses-list";
            }
            else if (requestURI.startsWith("/teacher/groups")) {
                sidebarCurrentPage = "groups";
                if (requestURI.equals("/teacher/groups")) sidebarSubPage = "groups-list";
                else if (requestURI.contains("/create")) sidebarSubPage = "groups-create";
                else sidebarSubPage = "groups-list";
            }
            else if (requestURI.startsWith("/teacher/students")) {
                sidebarCurrentPage = "students";
                if (requestURI.equals("/teacher/students")) sidebarSubPage = "students-list";
                else if (requestURI.contains("/create")) sidebarSubPage = "students-create";
                else sidebarSubPage = "students-list";
            }
            else if (requestURI.startsWith("/teacher/finances")) {
                sidebarCurrentPage = "finances";
            }
            // ===== THÊM: Teacher Exams =====
            else if (requestURI.startsWith("/teacher/exams")) {
                sidebarCurrentPage = "exams";
                if (requestURI.equals("/teacher/exams")) sidebarSubPage = "exams-list";
                else if (requestURI.contains("/create")) sidebarSubPage = "exams-create";
                else sidebarSubPage = "exams-list";
            }
        }
        // Student routes
        else if (requestURI.startsWith("/student/")) {
            if (requestURI.equals("/student/profile")) sidebarCurrentPage = "profile";
            else if (requestURI.startsWith("/student/courses")) sidebarCurrentPage = "courses";
            else if (requestURI.startsWith("/student/groups")) sidebarCurrentPage = "groups";
            else if (requestURI.startsWith("/student/schedule")) sidebarCurrentPage = "schedule";
            else if (requestURI.startsWith("/student/finance")) sidebarCurrentPage = "finance";
                // ===== THÊM: Student Grades (ĐÃ CÓ) =====
            else if (requestURI.startsWith("/student/grades")) sidebarCurrentPage = "grades";
        }

        modelAndView.addObject("sidebarCurrentPage", sidebarCurrentPage);
        modelAndView.addObject("sidebarSubPage", sidebarSubPage);
    }
}