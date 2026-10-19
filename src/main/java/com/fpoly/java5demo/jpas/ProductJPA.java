package com.fpoly.java5demo.jpas;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.fpoly.java5demo.entities.Product;

public interface ProductJPA extends JpaRepository<Product, Integer> {
	List<Product> findByStatusTrue();

	List<Product> findByCategoryIdAndStatusTrue(int categoryId);

	List<Product> findByNameContainingIgnoreCaseAndStatusTrue(String name);

	List<Product> findBySellerId(int sellerId);

	List<Product> findBySellerIdAndStatusTrue(int sellerId);

	@Query("SELECT p FROM Product p WHERE p.status = true ORDER BY p.id DESC")
	List<Product> findTopRecentProducts();

	@org.springframework.data.jpa.repository.Modifying
	@Query("UPDATE Product p SET p.quantity = p.quantity - :qty WHERE p.id = :id AND p.quantity >= :qty")
	int deductStock(@org.springframework.data.repository.query.Param("id") int id, @org.springframework.data.repository.query.Param("qty") int qty);
}

