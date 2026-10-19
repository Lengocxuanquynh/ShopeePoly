package com.fpoly.java5demo.jpas;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.fpoly.java5demo.entities.SellerRequest;

public interface SellerRequestJPA extends JpaRepository<SellerRequest, Integer> {

	Optional<SellerRequest> findFirstByUserIdOrderByIdDesc(int userId);

	Optional<SellerRequest> findFirstByUserIdAndStatusOrderByIdDesc(int userId, int status);

	List<SellerRequest> findAllByOrderByCreatedAtDesc();

	List<SellerRequest> findByStatusOrderByCreatedAtDesc(int status);

	long countByStatus(int status);
}
