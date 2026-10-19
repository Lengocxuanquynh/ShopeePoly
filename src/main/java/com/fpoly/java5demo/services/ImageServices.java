package com.fpoly.java5demo.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageServices {

	private String uploadDir = "uploads";

	private static final java.util.List<String> ALLOWED_EXTENSIONS = 
			java.util.Arrays.asList(".jpg", ".jpeg", ".png", ".webp", ".gif");

	public String save(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			return null;
		}

		// 1. Kiểm tra MIME Content-Type
		String contentType = file.getContentType();
		if (contentType == null || !contentType.startsWith("image/")) {
			throw new RuntimeException("Chỉ cho phép tải lên định dạng hình ảnh!");
		}

		// 2. Kiểm tra tên file và whitelist đuôi mở rộng để chống Web Shell / RCE
		String originalName = file.getOriginalFilename();
		if (originalName == null || !originalName.contains(".")) {
			throw new RuntimeException("Tên file không hợp lệ hoặc thiếu định dạng mở rộng!");
		}

		String extension = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
		if (!ALLOWED_EXTENSIONS.contains(extension)) {
			throw new RuntimeException(String.format("Định dạng file %s không được hỗ trợ! Chỉ chấp nhận: %s", extension, ALLOWED_EXTENSIONS));
		}

		try {
			// Tạo thư mục nếu chưa tồn tại
			Files.createDirectories(Paths.get(uploadDir));

			String fileName = String.format("%d_%s%s", new Date().getTime(),
					java.util.UUID.randomUUID().toString().substring(0, 8), extension);
			Path targetPath = Paths.get(uploadDir, fileName);

			Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

			return fileName;

		} catch (IOException e) {
			throw new RuntimeException(String.format("Không thể lưu trữ file ảnh: %s", e.getMessage()), e);
		}
	}
}
