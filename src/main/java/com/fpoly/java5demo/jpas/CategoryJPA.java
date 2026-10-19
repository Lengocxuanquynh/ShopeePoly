package com.fpoly.java5demo.jpas;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.fpoly.java5demo.entities.Category;

public interface CategoryJPA extends JpaRepository<Category, Integer> {

	Optional<Category> findByNameIgnoreCase(String name);

	Optional<Category> findByNameIgnoreCaseAndIdNot(String name, int id);

	default Optional<Category> checkNameExist(String name) {
		return findByNameIgnoreCase(name != null ? name.trim() : null);
	}

	default Optional<Category> checkNameExist(String name, int id) {
		return findByNameIgnoreCaseAndIdNot(name != null ? name.trim() : null, id);
	}
}
