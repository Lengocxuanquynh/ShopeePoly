package com.fpoly.java5demo.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fpoly.java5demo.entities.SellerRequest;
import com.fpoly.java5demo.entities.User;
import com.fpoly.java5demo.jpas.SellerRequestJPA;
import com.fpoly.java5demo.services.UserServices;

@Controller
@RequestMapping("/admin")
public class AdminSellerController {

	@Autowired
	private SellerRequestJPA sellerRequestJPA;

	@Autowired
	private UserServices userServices;

	@Autowired
	private com.fpoly.java5demo.services.NotificationServices notificationServices;

	@GetMapping("/seller-requests")
	public String listSellerRequests(Model model) {
		List<SellerRequest> requests = sellerRequestJPA.findAllByOrderByCreatedAtDesc();
		long pendingCount = sellerRequestJPA.countByStatus(0);

		model.addAttribute("requests", requests);
		model.addAttribute("pendingCount", pendingCount);
		return "admin/seller-requests";
	}

	@PostMapping("/seller-request/approve/{id}")
	public String approveRequest(@PathVariable("id") int id, RedirectAttributes redirectAttributes) {
		SellerRequest req = sellerRequestJPA.findById(id).orElse(null);
		if (req == null) {
			redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy yêu cầu mở gian hàng!");
			return "redirect:/admin/seller-requests";
		}

		req.setStatus(1); // 1 = Đã duyệt
		sellerRequestJPA.save(req);

		// Nâng cấp quyền người dùng thành Người Bán (Role = 2)
		User user = req.getUser();
		if (user != null) {
			user.setRole(2);
			userServices.save(user);

			notificationServices.createNotification(
					user,
					"Đăng ký Người Bán thành công!",
					String.format("Chúc mừng! Gian hàng '%s' của bạn đã được Super Admin phê duyệt. Bạn có thể bắt đầu đăng bán sản phẩm ngay.", req.getShopName()),
					"/seller/dashboard",
					"SELLER_REQUEST"
			);
		}

		redirectAttributes.addFlashAttribute("successMessage",
				String.format("Đã phê duyệt thành công gian hàng '%s'. Tài khoản '%s' hiện đã có quyền Người Bán!",
						req.getShopName(), (user != null ? user.getUsername() : "")));
		return "redirect:/admin/seller-requests";
	}

	@PostMapping("/seller-request/reject/{id}")
	public String rejectRequest(@PathVariable("id") int id, RedirectAttributes redirectAttributes) {
		SellerRequest req = sellerRequestJPA.findById(id).orElse(null);
		if (req == null) {
			redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy yêu cầu mở gian hàng!");
			return "redirect:/admin/seller-requests";
		}

		req.setStatus(2); // 2 = Từ chối
		sellerRequestJPA.save(req);

		if (req.getUser() != null) {
			notificationServices.createNotification(
					req.getUser(),
					"Yêu cầu mở Shop bị từ chối",
					String.format("Yêu cầu đăng ký mở gian hàng '%s' của bạn đã bị Super Admin từ chối. Vui lòng liên hệ hỗ trợ hoặc cập nhật lại thông tin hồ sơ.", req.getShopName()),
					"/seller-register",
					"SELLER_REQUEST"
			);
		}

		redirectAttributes.addFlashAttribute("errorMessage",
				String.format("Đã từ chối hồ sơ đăng ký mở gian hàng '%s'.", req.getShopName()));
		return "redirect:/admin/seller-requests";
	}
}
