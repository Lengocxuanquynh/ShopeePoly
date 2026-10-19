package com.fpoly.java5demo.services;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fpoly.java5demo.beans.CheckoutBean;
import com.fpoly.java5demo.entities.Order;
import com.fpoly.java5demo.entities.OrderDetail;
import com.fpoly.java5demo.entities.Product;
import com.fpoly.java5demo.entities.User;
import com.fpoly.java5demo.jpas.OrderDetailJPA;
import com.fpoly.java5demo.jpas.OrderJPA;
import com.fpoly.java5demo.jpas.ProductJPA;
import com.fpoly.java5demo.jpas.UserJPA;

@Service
public class OrderServices {

	@Autowired
	private OrderJPA orderJPA;

	@Autowired
	private OrderDetailJPA orderDetailJPA;

	@Autowired
	private UserJPA userJPA;

	@Autowired
	private ProductJPA productJPA;

	@Autowired
	private CartDetailServices cartDetailServices;

	@Autowired
	private com.fpoly.java5demo.jpas.CartDetailJPA cartDetailJPA;

	@Transactional
	public Order createOrder(User user, CheckoutBean checkoutBean) {
		Map<Integer, Integer> cartItems = cartDetailServices.getCartMapSession();
		if (cartItems.isEmpty()) {
			return null;
		}

		List<Integer> ids = new ArrayList<>(cartItems.keySet());
		List<Product> products = productJPA.findAllById(ids);

		int totalPrice = 0;
		for (Product item : products) {
			int buyQty = cartItems.getOrDefault(item.getId(), 0);
			if (item.getQuantity() < buyQty || buyQty <= 0) {
				return null; // Không đủ số lượng
			}
			totalPrice += buyQty * item.getPrice();
		}

		// Tạo đơn hàng
		Order order = new Order();
		String fullAddress = String.format("%s (Người nhận: %s - SĐT: %s)",
				checkoutBean.getAddress(), checkoutBean.getFullName(), checkoutBean.getPhone());
		if (checkoutBean.getNote() != null && !checkoutBean.getNote().trim().isEmpty()) {
			fullAddress += String.format(" [Ghi chú: %s]", checkoutBean.getNote().trim());
		}
		if (fullAddress.length() > 500) {
			fullAddress = fullAddress.substring(0, 500);
		}
		order.setAddress(fullAddress);
		order.setPaymentMethod(checkoutBean.getPaymentMethod() != null ? checkoutBean.getPaymentMethod() : "COD");
		order.setUser(user);

		Calendar calendar = Calendar.getInstance();
		String dateStr = String.format("%04d-%02d-%02d", calendar.get(Calendar.YEAR),
				calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH));
		order.setDate(Date.valueOf(dateStr));
		order.setStatus(0); // 0: Chờ xác nhận
		order.setTotalPrice(totalPrice);

		Order savedOrder = orderJPA.save(order);

		// Lưu chi tiết đơn hàng và trừ kho nguyên tử (Atomic Update chống Race Condition)
		for (Product item : products) {
			int buyQty = cartItems.get(item.getId());
			int affected = productJPA.deductStock(item.getId(), buyQty);
			if (affected == 0) {
				org.springframework.transaction.interceptor.TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
				return null;
			}

			OrderDetail detail = new OrderDetail();
			detail.setOrder(savedOrder);
			detail.setProduct(item);
			detail.setProductName(item.getName());
			if (item.getImages() != null && !item.getImages().isEmpty() && item.getImages().get(0) != null) {
				detail.setProductImage(item.getImages().get(0).getName());
			}
			detail.setPrice(item.getPrice());
			detail.setQuantity(buyQty);
			orderDetailJPA.save(detail);
		}

		// Xóa giỏ hàng trong session và dọn sạch giỏ hàng trong Database
		cartDetailServices.clearCart();
		try {
			cartDetailJPA.deleteByUserId(user.getId());
		} catch (Exception e) {
			System.err.println("Lỗi dọn giỏ hàng Database: " + e.getMessage());
		}

		return savedOrder;
	}

	public List<Order> getOrdersByUser(int userId) {
		return orderJPA.findByUserIdOrderByDateDesc(userId);
	}

	public List<Order> getOrdersBySeller(int sellerId) {
		return orderJPA.findBySellerId(sellerId);
	}

	public List<Order> getAllOrders() {
		return orderJPA.findAllByOrderByDateDesc();
	}

	public Order getOrderById(int orderId) {
		return orderJPA.findById(orderId).orElse(null);
	}

	public List<OrderDetail> getOrderDetails(int orderId) {
		return orderDetailJPA.findByOrderId(orderId);
	}

	@Transactional
	public boolean updateStatus(int orderId, int status) {
		Order order = getOrderById(orderId);
		if (order == null) {
			return false;
		}

		// Kiểm tra logic trạng thái đơn hàng (Order State Machine):
		// 1. Đơn hàng đã Hủy (5) không được mở lại tùy tiện
		if (order.getStatus() == 5 && status != 5) {
			return false;
		}

		// 2. Đơn hàng đã Hoàn thành (4) không được đổi ngược lại
		if (order.getStatus() == 4 && status != 4) {
			return false;
		}

		// 3. Đơn hàng đã Giao (3) hoặc Hoàn thành (4) không được tự ý hủy
		if ((order.getStatus() == 3 || order.getStatus() == 4) && status == 5) {
			return false;
		}

		// 4. Nếu chuyển sang Đã hủy (status = 5) từ trạng thái chưa hủy (0, 1, 2) thì hoàn kho
		if (order.getStatus() != 5 && status == 5) {
			List<OrderDetail> details = orderDetailJPA.findByOrderId(orderId);
			for (OrderDetail detail : details) {
				Product p = detail.getProduct();
				if (p != null) {
					p.setQuantity(p.getQuantity() + detail.getQuantity());
					productJPA.save(p);
				}
			}
		}

		order.setStatus(status);
		orderJPA.save(order);
		return true;
	}

	@Transactional
	public boolean cancelOrder(int orderId, int userId) {
		Order order = getOrderById(orderId);
		if (order == null || order.getUser().getId() != userId) {
			return false;
		}
		// Chỉ cho hủy khi đơn hàng ở trạng thái 0 (Chờ xác nhận)
		if (order.getStatus() == 0) {
			order.setStatus(5); // 5: Đã hủy
			orderJPA.save(order);

			// Hoàn trả số lượng sản phẩm vào kho
			List<OrderDetail> details = orderDetailJPA.findByOrderId(orderId);
			for (OrderDetail detail : details) {
				Product p = detail.getProduct();
				if (p != null) {
					p.setQuantity(p.getQuantity() + detail.getQuantity());
					productJPA.save(p);
				}
			}
			return true;
		}
		return false;
	}
}
