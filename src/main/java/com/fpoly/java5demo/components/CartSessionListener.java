package com.fpoly.java5demo.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fpoly.java5demo.entities.CartDetail;
import com.fpoly.java5demo.entities.Product;
import com.fpoly.java5demo.entities.User;
import com.fpoly.java5demo.jpas.UserJPA;
import com.fpoly.java5demo.services.CartDetailServices;
import com.fpoly.java5demo.services.ProductServices;
import com.fpoly.java5demo.utils.Utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class CartSessionListener implements HttpSessionListener {

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private CartDetailServices cartDetailServices;

	@Autowired(required = false)
	private HttpServletRequest request;

	@Autowired
	private UserJPA userJPA;

	@Autowired
	private ProductServices productServices;

	@Override
	public void sessionCreated(HttpSessionEvent se) {
		HttpSession session = se.getSession();
		Map<Integer, Integer> cartMap = new HashMap<>();

		try {
			if (request != null) {
				String userId = Utils.getCookieValue(Utils.COOKIE_KEY_USER_ID, request);
				if (userId != null) {
					List<CartDetail> cartDetails = cartDetailServices.getCartsByUserId(Integer.parseInt(userId));
					for (CartDetail cartDetail : cartDetails) {
						if (cartDetail.getProduct() != null) {
							cartMap.put(cartDetail.getProduct().getId(), cartDetail.getQuantity());
						}
					}
				}
			}
		} catch (Exception e) {
			// Request might not be available
		}

		try {
			String newCartJson = objectMapper.writeValueAsString(cartMap);
			session.setAttribute("cart", newCartJson);
			session.setAttribute("cartCount", cartMap.values().stream().mapToInt(Integer::intValue).sum());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent se) {
		HttpSession session = se.getSession();

		try {
			String userId = null;
			if (request != null) {
				userId = Utils.getCookieValue(Utils.COOKIE_KEY_USER_ID, request);
			}
			if (userId == null && session.getAttribute("user") != null) {
				User sessionUser = (User) session.getAttribute("user");
				userId = String.valueOf(sessionUser.getId());
			}

			if (userId == null) {
				return;
			}
			User user = userJPA.findById(Integer.parseInt(userId)).orElse(null);
			if (user == null) {
				return;
			}
			String cartJson = (String) session.getAttribute("cart");

			if (cartJson != null && !cartJson.isEmpty()) {
				Map<Integer, Integer> cartMap = objectMapper.readValue(cartJson,
						new TypeReference<Map<Integer, Integer>>() {
						});

				List<CartDetail> cartDetails = new ArrayList<>();
				for (Map.Entry<Integer, Integer> entry : cartMap.entrySet()) {
					int prodId = entry.getKey();
					int quantity = entry.getValue();

					Product product = productServices.findById(prodId);
					if (product == null) {
						continue;
					}

					CartDetail cartDetail = new CartDetail();
					cartDetail.setProduct(product);
					cartDetail.setUser(user);
					cartDetail.setQuantity(quantity);
					cartDetails.add(cartDetail);
				}

				cartDetailServices.syncSessionCartToDatabase(cartDetails);
			}
		} catch (Exception e) {
			System.err.println("Lỗi đồng bộ giỏ hàng khi hủy session: " + e.getMessage());
		}
	}
}
