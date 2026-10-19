package com.fpoly.java5demo.controllers;

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

import com.fpoly.java5demo.beans.UserFormBean;
import com.fpoly.java5demo.entities.User;
import com.fpoly.java5demo.services.UserServices;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin")
public class AdminUserController {

	@Autowired
	private UserServices userServices;

	@GetMapping({ "/users", "/user-form", "/user", "/user-list" })
	public String userForm(Model model) {
		model.addAttribute("bean", new UserFormBean());
		model.addAttribute("users", userServices.getAllUsers());
		return "admin/user-form";
	}

	@GetMapping("/user-form/{id}")
	public String userEdit(@PathVariable("id") int id, RedirectAttributes redirectAttributes) {
		redirectAttributes.addFlashAttribute("errorMessage",
				"Quản trị viên không được phép sửa thông tin của người dùng! Bạn chỉ có quyền Khóa hoặc Mở khóa tài khoản.");
		return "redirect:/admin/user-form";
	}

	@PostMapping("/user-form")
	public String userSave(@Valid @ModelAttribute("bean") UserFormBean bean, Errors errors,
			Model model, RedirectAttributes redirectAttributes) {
		boolean isUpdate = (bean.getId() != null && bean.getId() > 0);

		if (isUpdate) {
			redirectAttributes.addFlashAttribute("errorMessage",
					"Quản trị viên không được phép sửa thông tin của người dùng!");
			return "redirect:/admin/user-form";
		}

		// Chỉ tạo mới người dùng: Bắt buộc nhập mật khẩu
		if (bean.getPassword() == null || bean.getPassword().trim().length() < 6) {
			errors.rejectValue("password", "required", "Mật khẩu cho tài khoản mới phải từ 6 ký tự trở lên");
		}

		if (errors.hasErrors()) {
			model.addAttribute("users", userServices.getAllUsers());
			return "admin/user-form";
		}

		String error = userServices.saveUserFromBean(bean);
		if (error != null) {
			model.addAttribute("errorMessage", error);
			model.addAttribute("users", userServices.getAllUsers());
			return "admin/user-form";
		}

		redirectAttributes.addFlashAttribute("successMessage",
				String.format("Tạo tài khoản '%s' thành công!", bean.getUsername()));
		return "redirect:/admin/user-form";
	}

	@PostMapping("/user/toggle-status/{id}")
	public String toggleUserStatus(@PathVariable("id") int id, HttpServletRequest request,
			HttpSession session, RedirectAttributes redirectAttributes) {
		User currentAdmin = userServices.getLoggedInUser(request, session);
		Integer adminId = currentAdmin != null ? currentAdmin.getId() : null;

		if (adminId != null && id == adminId) {
			redirectAttributes.addFlashAttribute("errorMessage", "Không thể tự khóa tài khoản của chính mình!");
			return "redirect:/admin/user-form";
		}

		User user = userServices.getUserInfoById(id);
		if (user == null) {
			redirectAttributes.addFlashAttribute("errorMessage", "Người dùng không tồn tại!");
			return "redirect:/admin/user-form";
		}

		if ("admin".equalsIgnoreCase(user.getUsername()) || user.getId() == 1) {
			redirectAttributes.addFlashAttribute("errorMessage",
					"Không thể khóa hoặc xóa tài khoản Super Admin chính của hệ thống!");
			return "redirect:/admin/user-form";
		}

		boolean newStatus = !user.isStatus();
		user.setStatus(newStatus);
		userServices.save(user);

		String actionText = newStatus ? "mở khóa" : "khóa";
		redirectAttributes.addFlashAttribute("successMessage",
				String.format("Đã %s thành công tài khoản '%s'!", actionText, user.getUsername()));
		return "redirect:/admin/user-form";
	}

	@PostMapping("/user/delete/{id}")
	public String userDeletePost(@PathVariable("id") int id, HttpServletRequest request,
			HttpSession session, RedirectAttributes redirectAttributes) {
		return toggleUserStatus(id, request, session, redirectAttributes);
	}

	@GetMapping("/user/delete/{id}")
	public String userDelete(@PathVariable("id") int id, HttpServletRequest request,
			HttpSession session, RedirectAttributes redirectAttributes) {
		return toggleUserStatus(id, request, session, redirectAttributes);
	}
}
