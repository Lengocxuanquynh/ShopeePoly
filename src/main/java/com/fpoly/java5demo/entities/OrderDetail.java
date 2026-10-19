package com.fpoly.java5demo.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "order_details")
public class OrderDetail {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private int id;

	@Column(name = "price", nullable = false)
	private int price;

	@Column(name = "quantity", nullable = false)
	private int quantity;

	@Column(name = "product_name", length = 200, columnDefinition = "NVARCHAR(200)")
	private String productName;

	@Column(name = "product_image", length = 255)
	private String productImage;

	@ManyToOne
	@JoinColumn(name = "order_id", nullable = false)
	private Order order;

	@ManyToOne
	@JoinColumn(name = "prod_id", nullable = true)
	private Product product;

	public String getDisplayName() {
		if (productName != null && !productName.trim().isEmpty()) {
			return productName;
		}
		if (product != null && product.getName() != null) {
			return product.getName();
		}
		return "Sản phẩm";
	}

	public String getDisplayImage() {
		if (productImage != null && !productImage.trim().isEmpty()) {
			return productImage;
		}
		if (product != null && product.getImages() != null && !product.getImages().isEmpty()) {
			return product.getImages().get(0).getName();
		}
		return null;
	}

	public boolean isProductDeleted() {
		return product == null || !product.isStatus();
	}

	public boolean isPriceChanged() {
		return product != null && product.isStatus() && product.getPrice() != this.price;
	}

	public boolean isNameChanged() {
		return product != null && product.isStatus() && productName != null
				&& !productName.trim().isEmpty() && !product.getName().equals(this.productName);
	}

	public boolean isProductModified() {
		return product != null && product.isStatus() && (isPriceChanged() || isNameChanged());
	}
}

