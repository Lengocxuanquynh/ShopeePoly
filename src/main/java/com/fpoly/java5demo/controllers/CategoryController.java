package com.fpoly.java5demo.controllers;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fpoly.java5demo.beans.CategoryFormBean;
import com.fpoly.java5demo.entities.Category;
import com.fpoly.java5demo.services.CategoryServices;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin")
public class CategoryController {

	@Autowired
	private CategoryServices categoryServices;

	@GetMapping("/categories")
	public String listCategories() {
		return "redirect:/admin/category-form";
	}

	@GetMapping("/category-form")
	public String categoryAdd(Model model) {
		model.addAttribute("bean", new CategoryFormBean());
		model.addAttribute("categories", categoryServices.getList());
		return "admin/category-form";
	}

	@GetMapping("/category-form/{id}")
	public String categoryUpdate(Model model, @PathVariable(name = "id") int id) {
		Category category = categoryServices.getById(id);
		if (category == null) {
			return "redirect:/admin/category-form";
		}

		CategoryFormBean bean = new CategoryFormBean();
		bean.setName(category.getName());
		bean.setId(Optional.of(id));

		model.addAttribute("bean", bean);
		model.addAttribute("categories", categoryServices.getList());
		return "admin/category-form";
	}

	@PostMapping("/category-form")
	public String categorySave(@Valid @ModelAttribute("bean") CategoryFormBean bean, Errors errors,
			Model model, RedirectAttributes redirectAttributes) {
		if (errors.hasErrors()) {
			model.addAttribute("categories", categoryServices.getList());
			return "admin/category-form";
		}

		Category category = bean.convertBeanToEntity();
		String error;
		if (bean.getId() != null && bean.getId().isPresent()) {
			error = categoryServices.updateCategory(category);
		} else {
			error = categoryServices.addCategory(category);
		}

		if (error != null) {
			model.addAttribute("errorMessage", error);
			model.addAttribute("categories", categoryServices.getList());
			return "admin/category-form";
		}

		redirectAttributes.addFlashAttribute("successMessage", "Lưu danh mục thành công!");
		return "redirect:/admin/category-form";
	}

	@PostMapping("/category/delete/{id}")
	public String deleteCategory(@PathVariable("id") int id, RedirectAttributes redirectAttributes) {
		boolean ok = categoryServices.deleteCategory(id);
		if (ok) {
			redirectAttributes.addFlashAttribute("successMessage", "Xóa danh mục thành công!");
		} else {
			redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa danh mục đã chứa sản phẩm!");
		}
		return "redirect:/admin/category-form";
	}

	@ModelAttribute("categories")
	public List<Category> getCategories() {
		return categoryServices.getList();
	}
}
