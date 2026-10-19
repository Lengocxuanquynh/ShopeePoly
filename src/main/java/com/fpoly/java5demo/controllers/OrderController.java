package com.fpoly.java5demo.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fpoly.java5demo.beans.CartItemBean;
import com.fpoly.java5demo.beans.CheckoutBean;
import com.fpoly.java5demo.entities.Order;
import com.fpoly.java5demo.entities.OrderDetail;
import com.fpoly.java5demo.entities.User;
import com.fpoly.java5demo.services.CartDetailServices;
import com.fpoly.java5demo.services.OrderServices;
import com.fpoly.java5demo.services.UserServices;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class OrderController {

	@Autowired
	private OrderServices orderServices;

	@Autowired
	private CartDetailServices cartDetailServices;

	@Autowired
	private UserServices userServices;

	@GetMapping("/checkout")
	public String checkoutPage(Model model, HttpServletRequest request, HttpSession session,
			RedirectAttributes redirectAttributes) {
		User currentUser = userServices.getLoggedInUser(request, session);
		if (currentUser == null) {
			return "redirect:/login?redirect=/checkout";
		}

		if (currentUser.getRole() == 1) {
			redirectAttributes.addFlashAttribute("errorMessage",
					"Tài khoản Quản trị viên (Admin) không được phép thực hiện chức năng mua hàng!");
			return "redirect:/cart";
		}

		List<CartItemBean> cartItems = cartDetailServices.getCartItems();
		if (cartItems.isEmpty()) {
			return "redirect:/cart";
		}

		CheckoutBean bean = new CheckoutBean();
		bean.setFullName(currentUser.getName());
		if (currentUser.getPhone() != null && !currentUser.getPhone().trim().isEmpty()) {
			bean.setPhone(currentUser.getPhone().trim());
		}

		model.addAttribute("currentUser", currentUser);
		model.addAttribute("checkoutBean", bean);
		model.addAttribute("cartItems", cartItems);
		model.addAttribute("totalAmount", cartDetailServices.getTotalAmount());
		model.addAttribute("cartCount", cartDetailServices.getTotalQuantity());

		return "checkout";
	}

	@PostMapping("/checkout")
	public String placeOrder(@Valid @ModelAttribute("checkoutBean") CheckoutBean bean, Errors errors,
			Model model, HttpServletRequest request, HttpSession session) {
		User currentUser = userServices.getLoggedInUser(request, session);
		if (currentUser == null) {
			return "redirect:/login?redirect=/checkout";
		}

		if (currentUser.getRole() == 1) {
			model.addAttribute("errorMessage",
					"Tài khoản Quản trị viên (Admin) không được phép thực hiện chức năng mua hàng!");
			model.addAttribute("currentUser", currentUser);
			model.addAttribute("cartItems", cartDetailServices.getCartItems());
			model.addAttribute("totalAmount", cartDetailServices.getTotalAmount());
			model.addAttribute("cartCount", cartDetailServices.getTotalQuantity());
			return "checkout";
		}

		List<CartItemBean> cartItems = cartDetailServices.getCartItems();
		if (cartItems.isEmpty()) {
			return "redirect:/cart";
		}

		if (errors.hasErrors()) {
			model.addAttribute("currentUser", currentUser);
			model.addAttribute("cartItems", cartItems);
			model.addAttribute("totalAmount", cartDetailServices.getTotalAmount());
			model.addAttribute("cartCount", cartDetailServices.getTotalQuantity());
			return "checkout";
		}

		Order order = orderServices.createOrder(currentUser, bean);
		if (order == null) {
			model.addAttribute("errorMessage", "Không thể tạo đơn hàng! Vui lòng kiểm tra lại số lượng tồn kho của sản phẩm.");
			model.addAttribute("currentUser", currentUser);
			model.addAttribute("cartItems", cartItems);
			model.addAttribute("totalAmount", cartDetailServices.getTotalAmount());
			return "checkout";
		}

		return String.format("redirect:/order/success/%d", order.getId());
	}

	@GetMapping("/order/success/{id}")
	public String orderSuccess(@PathVariable("id") int id, Model model, HttpServletRequest request, HttpSession session) {
		User currentUser = userServices.getLoggedInUser(request, session);
		Order order = orderServices.getOrderById(id);

		if (order == null || (currentUser != null && order.getUser().getId() != currentUser.getId() && currentUser.getRole() != 1)) {
			return "redirect:/";
		}

		List<OrderDetail> details = orderServices.getOrderDetails(id);
		model.addAttribute("order", order);
		model.addAttribute("orderDetails", details);
		model.addAttribute("currentUser", currentUser);
		return "order-success";
	}

	@GetMapping("/user/orders")
	public String myOrders(Model model, HttpServletRequest request, HttpSession session) {
		User currentUser = userServices.getLoggedInUser(request, session);
		if (currentUser == null) {
			return "redirect:/login?redirect=/user/orders";
		}

		List<Order> orders = orderServices.getOrdersByUser(currentUser.getId());
		model.addAttribute("orders", orders);
		model.addAttribute("currentUser", currentUser);
		model.addAttribute("cartCount", cartDetailServices.getTotalQuantity());
		return "user/orders";
	}

	@GetMapping("/user/order/{id}")
	public String myOrderDetail(@PathVariable("id") int id, Model model, HttpServletRequest request, HttpSession session) {
		User currentUser = userServices.getLoggedInUser(request, session);
		if (currentUser == null) {
			return "redirect:/login?redirect=/user/orders";
		}

		Order order = orderServices.getOrderById(id);
		if (order == null || order.getUser().getId() != currentUser.getId()) {
			return "redirect:/user/orders";
		}

		List<OrderDetail> details = orderServices.getOrderDetails(id);
		model.addAttribute("order", order);
		model.addAttribute("orderDetails", details);
		model.addAttribute("currentUser", currentUser);
		return "user/order-detail";
	}

	@PostMapping("/user/order/cancel/{id}")
	public String cancelMyOrder(@PathVariable("id") int id, HttpServletRequest request, HttpSession session) {
		User currentUser = userServices.getLoggedInUser(request, session);
		if (currentUser != null) {
			orderServices.cancelOrder(id, currentUser.getId());
		}
		return "redirect:/user/orders";
	}
}
