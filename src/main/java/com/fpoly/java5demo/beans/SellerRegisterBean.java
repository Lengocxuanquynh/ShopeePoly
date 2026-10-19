package com.fpoly.java5demo.beans;

import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SellerRegisterBean {

	@NotBlank(message = "Tên gian hàng không được để trống")
	@Length(max = 200, message = "Tên gian hàng không được vượt quá 200 ký tự")
	private String shopName;

	@NotBlank(message = "Số điện thoại không được để trống")
	@Pattern(regexp = "^(0[0-9]{9})$", message = "Số điện thoại không hợp lệ (gồm 10 chữ số bắt đầu bằng 0)")
	private String phone;

	private Integer categoryId;

	@NotBlank(message = "Địa chỉ kho hàng không được để trống")
	@Length(max = 500, message = "Địa chỉ không được vượt quá 500 ký tự")
	private String address;

	@AssertTrue(message = "Bạn cần đồng ý với Điều khoản và Chính sách Người bán")
	private boolean termsAgree = true;
}
