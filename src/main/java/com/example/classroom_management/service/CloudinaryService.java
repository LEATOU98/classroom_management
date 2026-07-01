package com.example.classroom_management.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

@Service
public class CloudinaryService {

    @Autowired
    private Cloudinary cloudinary;

    /**
     * Upload avatar với kiểm tra trùng lặp bằng hash
     */
    public String uploadAvatar(MultipartFile file) throws IOException {
        // Tính hash của file để kiểm tra trùng lặp
        String fileHash = calculateFileHash(file);

        // Upload với public_id là hash để Cloudinary tự động chống trùng
        Map uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder", "avatars",
                        "width", 300,
                        "height", 300,
                        "crop", "fill",
                        "gravity", "face",
                        "resource_type", "image",
                        "public_id", "avatar_" + fileHash, // Nếu trùng hash, Cloudinary sẽ ghi đè
                        "overwrite", true,                  // Cho phép ghi đè nếu trùng
                        "unique_filename", false            // Không tạo tên ngẫu nhiên
                )
        );

        return uploadResult.get("secure_url").toString();
    }

    /**
     * Xóa avatar cũ
     */
    public void deleteAvatar(String avatarUrl) throws IOException {
        if (avatarUrl != null && avatarUrl.contains("cloudinary")) {
            String publicId = extractPublicId(avatarUrl);
            if (publicId != null) {
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            }
        }
    }

    /**
     * Tính MD5 hash của file để kiểm tra trùng lặp
     */
    private String calculateFileHash(MultipartFile file) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(file.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return String.valueOf(System.currentTimeMillis());
        }
    }

    /**
     * Lấy public_id từ Cloudinary URL
     */
    private String extractPublicId(String url) {
        try {
            String[] parts = url.split("/upload/");
            if (parts.length > 1) {
                String path = parts[1];
                if (path.contains("/")) {
                    path = path.substring(path.indexOf("/") + 1);
                }
                int dotIndex = path.lastIndexOf(".");
                if (dotIndex > 0) {
                    path = path.substring(0, dotIndex);
                }
                return path;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}