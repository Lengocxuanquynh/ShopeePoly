package com.fpoly.java5demo.components;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.fpoly.java5demo.entities.User;
import com.fpoly.java5demo.services.UserServices;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class AuthInterceptor implements HandlerInterceptor {

	@Autowired
	private UserServices userServices;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		HttpSession session = request.getSession();
		User user = userServices.getLoggedInUser(request, session);
		String uri = request.getRequestURI();

		// 1. Phân quyền Admin: Chỉ duy nhất Admin hệ thống (role == 1) mới được vào /admin/**
		if (uri.startsWith("/admin")) {
			if (user == null) {
				response.sendRedirect(String.format("/login?redirect=%s", uri));
				return false;
			}
			if (user.getRole() != 1) { // 1: Super Admin
				if (user.getRole() == 2) { // Nếu là người bán, chuyển hướng về Kênh Người Bán
					response.sendRedirect("/seller/dashboard");
				} else {
					response.sendRedirect("/?error=forbidden");
				}
				return false;
			}
		}

		// 2. Phân quyền Người Bán: Người Bán (role == 2) hoặc Admin (role == 1) mới vào được Seller Center
		if (uri.startsWith("/seller/dashboard") || uri.startsWith("/seller/product") || uri.startsWith("/seller/order") || uri.startsWith("/seller/profile")) {
			if (user == null) {
				response.sendRedirect(String.format("/login?redirect=%s", uri));
				return false;
			}
			if (user.getRole() != 2 && user.getRole() != 1) {
				response.sendRedirect("/seller");
				return false;
			}
		}

		// Nếu truy cập các trang yêu cầu đăng nhập: user profile, order history, checkout
		if (uri.startsWith("/user") || uri.startsWith("/checkout")) {
			if (user == null) {
				response.sendRedirect(String.format("/login?redirect=%s", uri));
				return false;
			}
		}

		return true;
	}
}
