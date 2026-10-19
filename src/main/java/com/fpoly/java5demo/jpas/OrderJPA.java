package com.fpoly.java5demo.jpas;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fpoly.java5demo.entities.Order;

public interface OrderJPA extends JpaRepository<Order, Integer> {
	List<Order> findByUserIdOrderByDateDesc(int userId);

	List<Order> findAllByOrderByDateDesc();

	List<Order> findByStatusOrderByDateDesc(int status);

	List<Order> findByUserIdAndStatusOrderByDateDesc(int userId, int status);
 
	boolean existsByUserId(int userId);

	@org.springframework.data.jpa.repository.Query("SELECT DISTINCT o FROM Order o JOIN o.orderDetails od WHERE od.product.seller.id = :sellerId ORDER BY o.date DESC")
	List<Order> findBySellerId(@org.springframework.data.repository.query.Param("sellerId") int sellerId);
}

