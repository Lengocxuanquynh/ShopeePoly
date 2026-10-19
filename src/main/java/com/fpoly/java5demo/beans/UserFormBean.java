package com.fpoly.java5demo.beans;

import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserFormBean {
	private Integer id;

	@NotBlank(message = "Tên đăng nhập không được để trống")
	private String username;

	private String password = "";

	@NotBlank(message = "Họ và tên không được để trống")
	private String name;

	@NotBlank(message = "Email không được để trống")
	@Email(message = "Định dạng email không hợp lệ")
	private String email;

	private int role = 0; // 0: Khách hàng, 1: Quản trị viên
	private boolean active = true;
}
