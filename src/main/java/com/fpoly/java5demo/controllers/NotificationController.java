package com.fpoly.java5demo.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fpoly.java5demo.entities.Notification;
import com.fpoly.java5demo.entities.User;
import com.fpoly.java5demo.jpas.NotificationJPA;
import com.fpoly.java5demo.services.NotificationServices;
import com.fpoly.java5demo.services.UserServices;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class NotificationController {

	@Autowired
	private NotificationServices notificationServices;

	@Autowired
	private NotificationJPA notificationJPA;

	@Autowired
	private UserServices userServices;

	@GetMapping("/notification/click/{id}")
	public String clickNotification(@PathVariable("id") int id, HttpServletRequest request, HttpSession session) {
		User currentUser = userServices.getLoggedInUser(request, session);
		if (currentUser == null) {
			return "redirect:/login";
		}

		Notification noti = notificationJPA.findById(id).orElse(null);
		if (noti != null && noti.getUser() != null && noti.getUser().getId() == currentUser.getId()) {
			notificationServices.markAsRead(id, currentUser.getId());
			if (noti.getLink() != null && !noti.getLink().isEmpty()) {
				return "redirect:" + noti.getLink();
			}
		}
		return "redirect:/";
	}

	@PostMapping("/notification/read-all")
	@ResponseBody
	public String markAllRead(HttpServletRequest request, HttpSession session) {
		User currentUser = userServices.getLoggedInUser(request, session);
		if (currentUser != null) {
			notificationServices.markAllAsRead(currentUser.getId());
			return "OK";
		}
		return "UNAUTHORIZED";
	}
}
