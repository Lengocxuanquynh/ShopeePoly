package com.fpoly.java5demo.beans;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ShopProfileBean {

	@NotBlank(message = "Tên gian hàng không được để trống!")
	@Size(min = 3, max = 100, message = "Tên gian hàng phải từ 3 đến 100 ký tự!")
	private String shopName;

	@NotBlank(message = "Số điện thoại hotline gian hàng không được để trống!")
	@Pattern(regexp = "^(0|\\+84)[0-9]{9,10}$", message = "Số điện thoại không đúng định dạng (Ví dụ: 0987654321)!")
	private String phone;

	@NotBlank(message = "Địa chỉ kho / xuất hàng không được để trống!")
	@Size(min = 5, max = 255, message = "Địa chỉ phải từ 5 đến 255 ký tự!")
	private String address;

	@Size(max = 1000, message = "Mô tả gian hàng tối đa 1000 ký tự!")
	private String description;

	private String categoryName;

	private MultipartFile avatarFile;

	private String currentAvatar;
}
