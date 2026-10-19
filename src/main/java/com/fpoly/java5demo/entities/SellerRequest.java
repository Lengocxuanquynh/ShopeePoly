package com.fpoly.java5demo.entities;

import java.sql.Date;

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
@Table(name = "seller_requests")
public class SellerRequest {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private int id;

	@Column(name = "shop_name", length = 200, nullable = false, columnDefinition = "NVARCHAR(200)")
	private String shopName;

	@Column(name = "phone", length = 20, nullable = false)
	private String phone;

	@Column(name = "address", length = 500, nullable = false, columnDefinition = "NVARCHAR(500)")
	private String address;

	@Column(name = "category_name", length = 100, columnDefinition = "NVARCHAR(100)")
	private String categoryName;

	@Column(name = "created_at", nullable = false)
	private Date createdAt;

	@Column(name = "status", nullable = false)
	private int status = 0; // 0: Chờ duyệt (Pending), 1: Đã duyệt (Approved), 2: Từ chối (Rejected)

	@Column(name = "note", length = 500, columnDefinition = "NVARCHAR(500)")
	private String note;

	@Column(name = "description", length = 1000, columnDefinition = "NVARCHAR(1000)")
	private String description;

	@Column(name = "avatar", length = 255)
	private String avatar;

	@ManyToOne
	@JoinColumn(name = "user_id", nullable = false)
	private User user;
}
