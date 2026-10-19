package com.fpoly.java5demo.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fpoly.java5demo.beans.CartItemBean;
import com.fpoly.java5demo.entities.CartDetail;
import com.fpoly.java5demo.entities.Product;
import com.fpoly.java5demo.jpas.CartDetailJPA;

import jakarta.servlet.http.HttpSession;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class CartDetailServices {

	@Autowired
	private CartDetailJPA cartDetailJPA;

	@Autowired
	private HttpSession session;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ProductServices productServices;

	public CartDetailServices() {
	}

	public List<CartDetail> getCartsByUserId(int id) {
		List<CartDetail> cartDetails = new ArrayList<>();
		try {
			cartDetails = cartDetailJPA.findByUserId(id);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return cartDetails;
	}

	public boolean syncSessionCartToDatabase(List<CartDetail> cartDetails) {
		try {
			if (cartDetails != null && !cartDetails.isEmpty() && cartDetails.get(0).getUser() != null) {
				cartDetailJPA.deleteByUserId(cartDetails.get(0).getUser().getId());
				cartDetailJPA.saveAll(cartDetails);
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean addToCart(int prodId, int count) {
		Map<Integer, Integer> cartMap = this.getCartMapSession();
		Product product = productServices.findById(prodId);
		if (product == null || !product.isStatus()) {
			return false;
		}

		int currentQty = cartMap.getOrDefault(prodId, 0);
		int newQty = currentQty + count;

		if (product.getQuantity() < newQty) {
			newQty = product.getQuantity();
		}

		if (newQty <= 0) {
			cartMap.remove(prodId);
		} else {
			cartMap.put(prodId, newQty);
		}

		saveCartToSession(cartMap);
		return true;
	}

	public boolean updateQuantity(int prodId, int quantity) {
		Map<Integer, Integer> cartMap = this.getCartMapSession();
		Product product = productServices.findById(prodId);
		if (product == null) {
			return false;
		}

		if (quantity <= 0) {
			cartMap.remove(prodId);
		} else {
			if (quantity > product.getQuantity()) {
				quantity = product.getQuantity();
			}
			cartMap.put(prodId, quantity);
		}

		saveCartToSession(cartMap);
		return true;
	}

	public boolean removeCart(int prodId) {
		Map<Integer, Integer> cartMap = this.getCartMapSession();
		if (cartMap.containsKey(prodId)) {
			cartMap.remove(prodId);
			saveCartToSession(cartMap);
			return true;
		}
		return false;
	}

	public void clearCart() {
		session.removeAttribute("cart");
		session.setAttribute("cartCount", 0);
	}

	public List<CartItemBean> getCartItems() {
		List<CartItemBean> items = new ArrayList<>();
		Map<Integer, Integer> cartMap = getCartMapSession();

		for (Map.Entry<Integer, Integer> entry : cartMap.entrySet()) {
			Product product = productServices.findById(entry.getKey());
			if (product != null && product.isStatus()) {
				items.add(new CartItemBean(product, entry.getValue()));
			}
		}

		return items;
	}

	public int getTotalAmount() {
		int total = 0;
		for (CartItemBean item : getCartItems()) {
			total += item.getSubTotal();
		}
		return total;
	}

	public int getTotalQuantity() {
		int count = 0;
		for (Integer qty : getCartMapSession().values()) {
			count += qty;
		}
		return count;
	}

	public Map<Integer, Integer> getCartMapSession() {
		Map<Integer, Integer> cartMap = new HashMap<>();
		String cartJson = (String) session.getAttribute("cart");
		if (cartJson != null && !cartJson.isEmpty()) {
			try {
				cartMap = objectMapper.readValue(cartJson, new TypeReference<Map<Integer, Integer>>() {
				});
			} catch (Exception e) {
				System.err.println("Lỗi parse JSON giỏ hàng: " + e.getMessage());
			}
		}
		return cartMap;
	}

	private void saveCartToSession(Map<Integer, Integer> cartMap) {
		try {
			String newCartJson = objectMapper.writeValueAsString(cartMap);
			session.setAttribute("cart", newCartJson);
			session.setAttribute("cartCount", getTotalQuantity());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
