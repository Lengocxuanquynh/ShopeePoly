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

	@Autowired
	private com.fpoly.java5demo.services.NotificationServices notificationServices;

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

		java.util.Set<Integer> notifiedSellerIds = new java.util.HashSet<>();

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

			if (item.getSeller() != null && !notifiedSellerIds.contains(item.getSeller().getId())) {
				notifiedSellerIds.add(item.getSeller().getId());
				notificationServices.createNotification(
						item.getSeller(),
						"Đơn hàng mới chờ duyệt!",
						String.format("Bạn có đơn hàng mới #%d từ khách hàng %s đang chờ duyệt.", savedOrder.getId(), user.getName()),
						"/seller/order/" + savedOrder.getId(),
						"ORDER"
				);
			}
		}

		// Thông báo cho Người Mua
		notificationServices.createNotification(
				user,
				"Đặt hàng thành công!",
				String.format("Đơn hàng #%d trị giá %s ₫ đã được tạo và đang chờ Shop duyệt đơn.", savedOrder.getId(), String.format("%,d", savedOrder.getTotalPrice())),
				"/user/order/" + savedOrder.getId(),
				"ORDER"
		);

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

		int currentStatus = order.getStatus();
		if (currentStatus == status) {
			return true;
		}

		// Kiểm tra logic trạng thái đơn hàng (Order State Machine) & Chống nhảy cóc (No skipping steps):
		// 0 (Chờ duyệt) -> chỉ được sang 1 (Đã duyệt & Đóng gói). Không được nhảy cóc sang 2, 3, 4, 5.
		// 1 (Đã duyệt) -> chỉ được sang 2 (Đang giao hàng) hoặc 5 (Hủy do hết hàng). Không nhảy sang 3, 4.
		// 2 (Đang giao hàng) -> chỉ được sang 3 (Đã nhận hàng) hoặc 5 (Giao thất bại). Không nhảy sang 4.
		// 3 (Đã nhận hàng) -> chỉ được sang 4 (Hoàn thành). Không nhảy sang 5.
		// 4 (Hoàn thành) hoặc 5 (Đã hủy) -> Trạng thái kết thúc, không được phép thay đổi.
		boolean isValidTransition = false;
		if (currentStatus == 0 && status == 1) {
			isValidTransition = true;
		} else if (currentStatus == 1 && (status == 2 || status == 5)) {
			isValidTransition = true;
		} else if (currentStatus == 2 && (status == 3 || status == 5)) {
			isValidTransition = true;
		} else if (currentStatus == 3 && status == 4) {
			isValidTransition = true;
		}

		if (!isValidTransition) {
			return false;
		}

		// Nếu chuyển sang Đã hủy (status = 5) từ trạng thái chưa hủy (1, 2) thì hoàn kho
		if (currentStatus != 5 && status == 5) {
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

		// Tạo thông báo cho Người Mua theo từng bước tiến trình
		if (order.getUser() != null) {
			String title;
			String msg;
			switch (status) {
				case 1:
					title = "Shop đã duyệt đơn hàng #" + order.getId();
					msg = "Kiện hàng #" + order.getId() + " của bạn đã được Shop duyệt và đang đóng gói.";
					break;
				case 2:
					title = "Đơn hàng #" + order.getId() + " đang được giao";
					msg = "Kiện hàng #" + order.getId() + " đã bàn giao cho shipper và đang trên đường tới bạn.";
					break;
				case 3:
					title = "Đơn hàng #" + order.getId() + " đã giao thành công";
					msg = "Đơn hàng #" + order.getId() + " đã được giao đến bạn. Hãy kiểm tra sản phẩm nhé!";
					break;
				case 4:
					title = "Đơn hàng #" + order.getId() + " hoàn thành";
					msg = "Đơn hàng #" + order.getId() + " đã hoàn tất. Cảm ơn bạn đã mua sắm tại PolyShop!";
					break;
				case 5:
					title = "Đơn hàng #" + order.getId() + " đã bị hủy";
					msg = "Đơn hàng #" + order.getId() + " của bạn đã bị hủy.";
					break;
				default:
					title = "Cập nhật đơn hàng #" + order.getId();
					msg = "Trạng thái đơn hàng #" + order.getId() + " vừa được cập nhật.";
			}

			notificationServices.createNotification(
					order.getUser(),
					title,
					msg,
					"/user/order/" + order.getId(),
					"ORDER"
			);
		}

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
