package com.fpoly.java5demo.jpas;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.fpoly.java5demo.entities.Notification;

@Repository
public interface NotificationJPA extends JpaRepository<Notification, Integer> {
	List<Notification> findByUserIdOrderByCreatedAtDesc(int userId);

	long countByUserIdAndReadFalse(int userId);
}
