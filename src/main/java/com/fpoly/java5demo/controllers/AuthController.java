package com.fpoly.java5demo.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fpoly.java5demo.beans.LoginBean;
import com.fpoly.java5demo.beans.RegisterBean;
import com.fpoly.java5demo.entities.User;
import com.fpoly.java5demo.services.UserServices;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class AuthController {

	@Autowired
	private UserServices userServices;

	@GetMapping("/login")
	public String loginForm(Model model, @RequestParam(name = "redirect", required = false) String redirect) {
		model.addAttribute("loginBean", new LoginBean());
		model.addAttribute("redirect", redirect);
		return "login";
	}

	@PostMapping("/login")
	public String login(@Valid @ModelAttribute("loginBean") LoginBean bean, Errors errors,
			@RequestParam(name = "redirect", required = false) String redirect,
			HttpServletResponse response, HttpSession session, Model model) {
		if (errors.hasErrors()) {
			model.addAttribute("redirect", redirect);
			return "login";
		}

		User user = userServices.login(bean.getUsername(), bean.getPassword(), bean.isRememberMe(), response, session);
		if (user == null) {
			model.addAttribute("errorMessage", "Tên đăng nhập hoặc mật khẩu không chính xác, hoặc tài khoản đã bị khóa!");
			model.addAttribute("redirect", redirect);
			return "login";
		}

		if (redirect != null && !redirect.trim().isEmpty() && !redirect.contains("/login")) {
			return String.format("redirect:%s", redirect);
		}

		if (user.getRole() == 1) {
			return "redirect:/admin";
		}
		if (user.getRole() == 2) {
			return "redirect:/seller/dashboard";
		}

		return "redirect:/";
	}

	@GetMapping("/register")
	public String registerForm(Model model) {
		model.addAttribute("bean", new RegisterBean());
		return "register";
	}

	@PostMapping("/register")
	public String register(@Valid @ModelAttribute("bean") RegisterBean bean, Errors errors, Model model) {
		if (errors.hasErrors()) {
			return "register";
		}

		String error = userServices.register(bean);
		if (error != null) {
			model.addAttribute("errorMessage", error);
			return "register";
		}

		model.addAttribute("successMessage", "Đăng ký tài khoản thành công! Vui lòng đăng nhập.");
		model.addAttribute("loginBean", new LoginBean());
		return "login";
	}

	@GetMapping("/logout")
	public String logout(HttpServletRequest request, HttpServletResponse response, HttpSession session) {
		userServices.logout(request, response, session);
		return "redirect:/";
	}
}
