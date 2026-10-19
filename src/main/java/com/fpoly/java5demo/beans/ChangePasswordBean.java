package com.fpoly.java5demo.beans;

import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordBean {
	@NotBlank(message = "Mật khẩu hiện tại không được để trống")
	private String currentPassword;

	@NotBlank(message = "Mật khẩu mới không được để trống")
	@Length(min = 6, message = "Mật khẩu mới phải từ 6 ký tự trở lên")
	private String newPassword;

	@NotBlank(message = "Xác nhận mật khẩu mới không được để trống")
	private String confirmPassword;
}
