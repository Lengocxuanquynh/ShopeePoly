package com.fpoly.java5demo.services;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fpoly.java5demo.entities.Notification;
import com.fpoly.java5demo.entities.User;
import com.fpoly.java5demo.jpas.NotificationJPA;

@Service
public class NotificationServices {

	@Autowired
	private NotificationJPA notificationJPA;

	public Notification createNotification(User user, String title, String content, String link, String type) {
		if (user == null) {
			return null;
		}
		Notification noti = new Notification();
		noti.setUser(user);
		noti.setTitle(title);
		noti.setContent(content);
		noti.setLink(link);
		noti.setType(type);
		noti.setRead(false);
		noti.setCreatedAt(new Date());
		return notificationJPA.save(noti);
	}

	public List<Notification> getNotificationsForUser(int userId) {
		return notificationJPA.findByUserIdOrderByCreatedAtDesc(userId);
	}

	public long getUnreadCount(int userId) {
		return notificationJPA.countByUserIdAndReadFalse(userId);
	}

	public void markAsRead(int id, int userId) {
		notificationJPA.findById(id).ifPresent(n -> {
			if (n.getUser() != null && n.getUser().getId() == userId) {
				n.setRead(true);
				notificationJPA.save(n);
			}
		});
	}

	public void markAllAsRead(int userId) {
		List<Notification> notis = notificationJPA.findByUserIdOrderByCreatedAtDesc(userId);
		for (Notification n : notis) {
			if (!n.isRead()) {
				n.setRead(true);
			}
		}
		notificationJPA.saveAll(notis);
	}
}
