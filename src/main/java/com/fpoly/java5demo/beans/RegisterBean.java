package com.fpoly.java5demo.beans;

import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class RegisterBean {
	@NotBlank(message = "Tên đăng nhập không được để trống")
	@jakarta.validation.constraints.Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "Tên đăng nhập không được chứa khoảng cách (dấu cách) hoặc ký tự đặc biệt")
	@Length(min = 3, max = 50, message = "Tên đăng nhập phải từ 3 đến 50 ký tự")
	private String username;

	@NotBlank(message = "Mật khẩu không được để trống")
	@Length(min = 6, message = "Mật khẩu phải từ 6 ký tự trở lên")
	private String password;

	@NotBlank(message = "Email không được để trống")
	@Email(message = "Định dạng email không hợp lệ")
	private String email;

	@NotBlank(message = "Họ và tên không được để trống")
	private String name;
}
