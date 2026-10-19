package com.fpoly.java5demo.config;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.fpoly.java5demo.entities.Notification;
import com.fpoly.java5demo.entities.User;
import com.fpoly.java5demo.services.NotificationServices;
import com.fpoly.java5demo.services.UserServices;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@ControllerAdvice
public class GlobalModelAttributes {

	@Autowired
	private UserServices userServices;

	@Autowired
	private NotificationServices notificationServices;

	@ModelAttribute("currentUser")
	public User getCurrentUser(HttpServletRequest request, HttpSession session) {
		return userServices.getLoggedInUser(request, session);
	}

	@ModelAttribute("userNotifications")
	public List<Notification> getUserNotifications(HttpServletRequest request, HttpSession session) {
		User user = userServices.getLoggedInUser(request, session);
		if (user != null) {
			return notificationServices.getNotificationsForUser(user.getId());
		}
		return Collections.emptyList();
	}

	@ModelAttribute("unreadNotificationCount")
	public long getUnreadNotificationCount(HttpServletRequest request, HttpSession session) {
		User user = userServices.getLoggedInUser(request, session);
		if (user != null) {
			return notificationServices.getUnreadCount(user.getId());
		}
		return 0;
	}
}
