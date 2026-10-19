package com.fpoly.java5demo.entities;

import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "notifications")
public class Notification {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private int id;

	@ManyToOne
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "title", nullable = false, length = 200, columnDefinition = "NVARCHAR(200)")
	private String title;

	@Column(name = "content", nullable = false, length = 500, columnDefinition = "NVARCHAR(500)")
	private String content;

	@Column(name = "link", length = 255)
	private String link;

	@Column(name = "type", length = 50)
	private String type; // ORDER, SELLER_REQUEST, SYSTEM

	@Column(name = "is_read", nullable = false)
	private boolean read = false;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "created_at", nullable = false)
	private Date createdAt = new Date();
}
