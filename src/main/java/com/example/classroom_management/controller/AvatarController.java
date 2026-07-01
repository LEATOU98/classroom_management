package com.example.classroom_management.controller;

import com.example.classroom_management.entity.User;
import com.example.classroom_management.repository.UserRepository;
import com.example.classroom_management.service.CloudinaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/profile")
public class AvatarController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    @PostMapping("/upload-avatar")
    public String uploadAvatar(@RequestParam("avatarFile") MultipartFile file,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {
        String oldAvatarUrl = null;

        try {
            User user = userRepository.findByUsername(principal.getName());

            if (user == null) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy user!");
                return "redirect:/profile";
            }

            // Lưu URL cũ để xóa sau
            oldAvatarUrl = user.getAvatarUrl();

            // Upload avatar mới lên Cloudinary TRƯỚC
            String newAvatarUrl = cloudinaryService.uploadAvatar(file);

            // Kiểm tra trùng ảnh
            if (oldAvatarUrl != null && oldAvatarUrl.equals(newAvatarUrl)) {
                redirectAttributes.addFlashAttribute("error", "Ảnh này giống với ảnh hiện tại!");
                return "redirect:/" + user.getRole().toString().toLowerCase() + "/profile";
            }

            // Lưu URL mới vào DB TRƯỚC
            user.setAvatarUrl(newAvatarUrl);
            userRepository.save(user);

            // Xóa avatar cũ SAU khi đã lưu thành công
            if (oldAvatarUrl != null && !oldAvatarUrl.isEmpty()) {
                try {
                    cloudinaryService.deleteAvatar(oldAvatarUrl);
                } catch (Exception e) {
                    // Không quan trọng nếu xóa lỗi
                }
            }

            redirectAttributes.addFlashAttribute("success", "Cập nhật avatar thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi upload: " + e.getMessage());
        }

        // Redirect về đúng profile theo role
        User user = userRepository.findByUsername(principal.getName());
        if (user != null) {
            return "redirect:/" + user.getRole().toString().toLowerCase() + "/profile";
        }
        return "redirect:/profile";
    }

    @PostMapping("/delete-avatar")
    public String deleteAvatar(Principal principal, RedirectAttributes redirectAttributes) {
        try {
            User user = userRepository.findByUsername(principal.getName());

            if (user == null) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy user!");
                return "redirect:/profile";
            }

            String oldAvatarUrl = user.getAvatarUrl();

            // Xóa URL trong DB TRƯỚC
            user.setAvatarUrl(null);
            userRepository.save(user);

            // Xóa trên Cloudinary SAU
            if (oldAvatarUrl != null && !oldAvatarUrl.isEmpty()) {
                try {
                    cloudinaryService.deleteAvatar(oldAvatarUrl);
                } catch (Exception e) {
                    // Không quan trọng nếu xóa lỗi
                }
            }

            redirectAttributes.addFlashAttribute("success", "Xóa avatar thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }

        // Redirect về đúng profile theo role
        User user = userRepository.findByUsername(principal.getName());
        if (user != null) {
            return "redirect:/" + user.getRole().toString().toLowerCase() + "/profile";
        }
        return "redirect:/profile";
    }
}