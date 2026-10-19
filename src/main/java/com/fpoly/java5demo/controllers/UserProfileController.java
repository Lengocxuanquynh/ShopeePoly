package com.fpoly.java5demo.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fpoly.java5demo.beans.ChangePasswordBean;
import com.fpoly.java5demo.beans.UserProfileBean;
import com.fpoly.java5demo.entities.User;
import com.fpoly.java5demo.services.CartDetailServices;
import com.fpoly.java5demo.services.UserServices;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/user")
public class UserProfileController {

	@Autowired
	private UserServices userServices;

	@Autowired
	private CartDetailServices cartDetailServices;

	@ModelAttribute("currentUser")
	public User getCurrentUser(HttpServletRequest request, HttpSession session) {
		return userServices.getLoggedInUser(request, session);
	}

	@ModelAttribute("cartCount")
	public int getCartCount() {
		return cartDetailServices.getTotalQuantity();
	}

	@GetMapping("/profile")
	public String profileForm(Model model, HttpServletRequest request, HttpSession session) {
		User currentUser = userServices.getLoggedInUser(request, session);
		if (currentUser == null) {
			return "redirect:/login?redirect=/user/profile";
		}

		UserProfileBean profileBean = new UserProfileBean();
		profileBean.setId(currentUser.getId());
		profileBean.setUsername(currentUser.getUsername());
		profileBean.setName(currentUser.getName());
		profileBean.setEmail(currentUser.getEmail());
		profileBean.setPhone(currentUser.getPhone() != null ? currentUser.getPhone() : "");

		model.addAttribute("profileBean", profileBean);
		model.addAttribute("passwordBean", new ChangePasswordBean());
		return "user/profile";
	}

	@PostMapping("/profile")
	public String updateProfile(@Valid @ModelAttribute("profileBean") UserProfileBean profileBean, Errors errors,
			Model model, HttpServletRequest request, HttpSession session, RedirectAttributes redirectAttributes) {
		User currentUser = userServices.getLoggedInUser(request, session);
		if (currentUser == null) {
			return "redirect:/login?redirect=/user/profile";
		}

		if (errors.hasErrors()) {
			model.addAttribute("passwordBean", new ChangePasswordBean());
			return "user/profile";
		}

		String error = userServices.updateUserProfile(currentUser.getId(), profileBean.getName(), profileBean.getEmail(), profileBean.getPhone());
		if (error != null) {
			model.addAttribute("passwordBean", new ChangePasswordBean());
			model.addAttribute("errorMessage", error);
			return "user/profile";
		}

		// Cập nhật lại user trong session
		currentUser.setName(profileBean.getName());
		currentUser.setEmail(profileBean.getEmail());
		currentUser.setPhone(profileBean.getPhone());
		session.setAttribute("user", currentUser);

		redirectAttributes.addFlashAttribute("successProfileMessage", "Cập nhật thông tin cá nhân thành công!");
		return "redirect:/user/profile";
	}

	@PostMapping("/change-password")
	public String changePassword(@Valid @ModelAttribute("passwordBean") ChangePasswordBean passwordBean, Errors errors,
			Model model, HttpServletRequest request, HttpSession session, RedirectAttributes redirectAttributes) {
		User currentUser = userServices.getLoggedInUser(request, session);
		if (currentUser == null) {
			return "redirect:/login?redirect=/user/profile";
		}

		if (!passwordBean.getNewPassword().equals(passwordBean.getConfirmPassword())) {
			errors.rejectValue("confirmPassword", "mismatch", "Xác nhận mật khẩu mới không khớp!");
		}

		if (errors.hasErrors()) {
			UserProfileBean profileBean = new UserProfileBean();
			profileBean.setId(currentUser.getId());
			profileBean.setUsername(currentUser.getUsername());
			profileBean.setName(currentUser.getName());
			profileBean.setEmail(currentUser.getEmail());
			profileBean.setPhone(currentUser.getPhone() != null ? currentUser.getPhone() : "");

			model.addAttribute("profileBean", profileBean);
			model.addAttribute("activeTab", "password");
			return "user/profile";
		}

		String error = userServices.changePassword(currentUser.getId(), passwordBean.getCurrentPassword(), passwordBean.getNewPassword());
		if (error != null) {
			UserProfileBean profileBean = new UserProfileBean();
			profileBean.setId(currentUser.getId());
			profileBean.setUsername(currentUser.getUsername());
			profileBean.setName(currentUser.getName());
			profileBean.setEmail(currentUser.getEmail());
			profileBean.setPhone(currentUser.getPhone() != null ? currentUser.getPhone() : "");

			model.addAttribute("profileBean", profileBean);
			model.addAttribute("activeTab", "password");
			errors.rejectValue("currentPassword", "invalid", error);
			return "user/profile";
		}

		redirectAttributes.addFlashAttribute("successPasswordMessage", "Đổi mật khẩu thành công!");
		return "redirect:/user/profile";
	}
}
