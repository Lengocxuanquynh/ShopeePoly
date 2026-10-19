package com.fpoly.java5demo.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fpoly.java5demo.entities.Order;
import com.fpoly.java5demo.entities.OrderDetail;
import com.fpoly.java5demo.services.OrderServices;

@Controller
@RequestMapping("/admin")
public class AdminOrderController {

	@Autowired
	private OrderServices orderServices;

	@GetMapping("/orders")
	public String listOrders(Model model) {
		List<Order> orders = orderServices.getAllOrders();
		model.addAttribute("orders", orders);
		return "admin/order-list";
	}

	@GetMapping("/order/{id}")
	public String viewOrder(@PathVariable("id") int id, Model model) {
		Order order = orderServices.getOrderById(id);
		if (order == null) {
			return "redirect:/admin/orders";
		}
		List<OrderDetail> details = orderServices.getOrderDetails(id);
		model.addAttribute("order", order);
		model.addAttribute("orderDetails", details);
		return "admin/order-detail";
	}

	@PostMapping("/order/status")
	public String updateStatus(@RequestParam("orderId") int orderId,
			@RequestParam("status") int status, RedirectAttributes redirectAttributes) {
		boolean ok = orderServices.updateStatus(orderId, status);
		if (ok) {
			redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái đơn hàng thành công!");
		} else {
			redirectAttributes.addFlashAttribute("errorMessage", "Không thể cập nhật trạng thái đơn hàng!");
		}
		return String.format("redirect:/admin/order/%d", orderId);
	}
}
