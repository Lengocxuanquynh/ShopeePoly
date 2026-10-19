package com.fpoly.java5demo.beans;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class ProductFormBean {
	private Integer id;

	@NotBlank(message = "Tên sản phẩm không được để trống")
	private String name;

	@Min(value = 1000, message = "Giá sản phẩm phải từ 1.000 VNĐ trở lên")
	private int price = 10000;

	@Min(value = 0, message = "Số lượng không được âm")
	private int quantity = 1;

	@Min(value = 1, message = "Vui lòng chọn danh mục hợp lệ")
	private int category;

	private int status = 1; // 1: Hiển thị, 2: Ẩn

	private List<MultipartFile> images;
}
