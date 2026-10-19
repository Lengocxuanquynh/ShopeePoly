package com.fpoly.java5demo.entities;

import java.sql.Date;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "orders")
public class Order {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private int id;

	@Column(name = "address", length = 500, nullable = false, columnDefinition = "NVARCHAR(500)")
	private String address;

	@Column(name = "payment_method", length = 50, columnDefinition = "NVARCHAR(50)")
	private String paymentMethod = "COD";

	@Column(name = "total_price", nullable = false)
	private int totalPrice;

	@Column(name = "date", nullable = false)
	private Date date;

	@Column(name = "status", nullable = false)
	private int status = 0;

	@ManyToOne
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@OneToMany(mappedBy = "order")
	List<OrderDetail> orderDetails;

	public String getSellerNames() {
		try {
			if (orderDetails == null || !org.hibernate.Hibernate.isInitialized(orderDetails) || orderDetails.isEmpty()) {
				return "Gian Hàng Hệ Thống";
			}
			java.util.Set<String> sellers = new java.util.LinkedHashSet<>();
			for (OrderDetail od : orderDetails) {
				if (od.getProduct() != null && od.getProduct().getSeller() != null
						&& od.getProduct().getSeller().getName() != null) {
					sellers.add(od.getProduct().getSeller().getName());
				}
			}
			if (sellers.isEmpty()) {
				return "Gian Hàng Hệ Thống";
			}
			return String.join(", ", sellers);
		} catch (Exception e) {
			return "Gian Hàng Hệ Thống";
		}
	}
}
