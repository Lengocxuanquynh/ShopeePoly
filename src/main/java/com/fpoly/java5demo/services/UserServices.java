package com.fpoly.java5demo.services;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fpoly.java5demo.beans.RegisterBean;
import com.fpoly.java5demo.entities.User;
import com.fpoly.java5demo.jpas.UserJPA;
import com.fpoly.java5demo.utils.Utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Service
public class UserServices {

	@Autowired
	private UserJPA userJPA;

	@Autowired
	private com.fpoly.java5demo.jpas.OrderJPA orderJPA;

	@Autowired
	private com.fpoly.java5demo.jpas.CartDetailJPA cartDetailJPA;

	public String register(RegisterBean bean) {
		if (userJPA.existsByUsername(bean.getUsername())) {
			return "Tên tài khoản đã tồn tại!";
		}
		if (userJPA.existsByEmail(bean.getEmail())) {
			return "Email đã được sử dụng!";
		}

		User user = new User();
		user.setUsername(bean.getUsername());
		user.setPassword(bean.getPassword());
		user.setName(bean.getName());
		user.setEmail(bean.getEmail());
		user.setRole(0); // 0: User, 1: Admin
		user.setStatus(true);

		userJPA.save(user);
		return null;
	}

	public User login(String username, String password, boolean rememberMe, HttpServletResponse response, HttpSession session) {
		Optional<User> optionalUser = userJPA.findByUsernameAndPassword(username, password);
		if (optionalUser.isPresent()) {
			User user = optionalUser.get();
			if (!user.isStatus()) {
				return null; // Tài khoản bị khóa
			}

			// Lưu vào session
			session.setAttribute("user", user);
			session.setAttribute("role", user.getRole());

			// Nạp giỏ hàng đã lưu trong Database vào Session
			try {
				List<com.fpoly.java5demo.entities.CartDetail> dbCarts = cartDetailJPA.findByUserId(user.getId());
				if (dbCarts != null && !dbCarts.isEmpty()) {
					com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
					java.util.Map<Integer, Integer> cartMap = new java.util.HashMap<>();
					String currentCartJson = (String) session.getAttribute("cart");
					if (currentCartJson != null && !currentCartJson.isEmpty()) {
						try {
							cartMap = mapper.readValue(currentCartJson,
									new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<Integer, Integer>>() {
									});
						} catch (Exception ignored) {
						}
					}
					for (com.fpoly.java5demo.entities.CartDetail cd : dbCarts) {
						if (cd.getProduct() != null && cd.getProduct().isStatus()) {
							int prodId = cd.getProduct().getId();
							cartMap.put(prodId, Math.max(cartMap.getOrDefault(prodId, 0), cd.getQuantity()));
						}
					}
					session.setAttribute("cart", mapper.writeValueAsString(cartMap));
					session.setAttribute("cartCount", cartMap.values().stream().mapToInt(Integer::intValue).sum());
				}
			} catch (Exception e) {
				System.err.println("Lỗi nạp giỏ hàng DB khi đăng nhập: " + e.getMessage());
			}

			// Lưu vào cookie an toàn có chữ ký mật mã nếu rememberMe
			if (rememberMe && response != null) {
				Utils.setCookie(Utils.COOKIE_KEY_USER_ID, String.valueOf(user.getId()), response);
				Utils.setCookie(Utils.COOKIE_KEY_ROLE, String.valueOf(user.getRole()), response);
				String token = Utils.signAuthToken(user.getId(), user.getRole(), user.getPassword());
				Utils.setCookie(Utils.COOKIE_KEY_AUTH_TOKEN, token, response);
			}
			return user;
		}
		return null;
	}

	public void logout(HttpServletRequest request, HttpServletResponse response, HttpSession session) {
		if (session != null) {
			session.removeAttribute("user");
			session.removeAttribute("role");
		}
		if (request != null && response != null) {
			Utils.clearCookie(request, response);
		}
	}

	public User getUserInfoById(int id) {
		return userJPA.findById(id).orElse(null);
	}

	public User getLoggedInUser(HttpServletRequest request, HttpSession session) {
		if (session != null && session.getAttribute("user") != null) {
			User sessionUser = (User) session.getAttribute("user");
			User dbUser = getUserInfoById(sessionUser.getId());
			if (dbUser != null && dbUser.isStatus()) {
				session.setAttribute("user", dbUser);
				session.setAttribute("role", dbUser.getRole());
				return dbUser;
			} else {
				// Tài khoản đã bị Admin khóa hoặc không tồn tại: HỦY SESSION NGAY
				session.removeAttribute("user");
				session.removeAttribute("role");
				return null;
			}
		}
		if (request != null) {
			String userId = Utils.getCookieValue(Utils.COOKIE_KEY_USER_ID, request);
			String cookieRole = Utils.getCookieValue(Utils.COOKIE_KEY_ROLE, request);
			String cookieToken = Utils.getCookieValue(Utils.COOKIE_KEY_AUTH_TOKEN, request);
			if (userId != null && cookieToken != null) {
				try {
					User user = getUserInfoById(Integer.parseInt(userId));
					if (user != null && user.isStatus()) {
						// Kiểm tra chữ ký mật mã chống giả mạo Cookie leo quyền Admin
						String expectedToken = Utils.signAuthToken(user.getId(), user.getRole(), user.getPassword());
						if (!cookieToken.equals(expectedToken)) {
							return null;
						}
						if (cookieRole != null && Integer.parseInt(cookieRole) != user.getRole()) {
							return null;
						}
						if (session != null) {
							session.setAttribute("user", user);
							session.setAttribute("role", user.getRole());
						}
						return user;
					}
				} catch (Exception ignored) {
				}
			}
		}
		return null;
	}

	public List<User> getAllUsers() {
		return userJPA.findAll();
	}

	public User save(User user) {
		return userJPA.save(user);
	}

	public Optional<User> findById(int id) {
		return userJPA.findById(id);
	}

	public String updateUserProfile(int userId, String name, String email, String phone) {
		User user = userJPA.findById(userId).orElse(null);
		if (user == null) {
			return "Không tìm thấy thông tin người dùng!";
		}

		Optional<User> existingEmailUser = userJPA.findByEmail(email);
		if (existingEmailUser.isPresent() && existingEmailUser.get().getId() != userId) {
			return "Email đã được sử dụng bởi tài khoản khác!";
		}

		user.setName(name);
		user.setEmail(email);
		user.setPhone(phone);
		userJPA.save(user);
		return null;
	}

	public String changePassword(int userId, String currentPassword, String newPassword) {
		User user = userJPA.findById(userId).orElse(null);
		if (user == null) {
			return "Không tìm thấy người dùng!";
		}
		if (!user.getPassword().equals(currentPassword)) {
			return "Mật khẩu hiện tại không chính xác!";
		}
		if (newPassword == null || newPassword.length() < 6) {
			return "Mật khẩu mới phải từ 6 ký tự trở lên!";
		}
		user.setPassword(newPassword);
		userJPA.save(user);
		return null;
	}

	public String saveUserFromBean(com.fpoly.java5demo.beans.UserFormBean bean) {
		boolean isUpdate = (bean.getId() != null && bean.getId() > 0);

		if (isUpdate) {
			User user = userJPA.findById(bean.getId()).orElse(null);
			if (user == null) {
				return "Người dùng không tồn tại!";
			}

			Optional<User> byUsername = userJPA.findByUsername(bean.getUsername());
			if (byUsername.isPresent() && byUsername.get().getId() != user.getId()) {
				return "Tên đăng nhập đã tồn tại!";
			}

			Optional<User> byEmail = userJPA.findByEmail(bean.getEmail());
			if (byEmail.isPresent() && byEmail.get().getId() != user.getId()) {
				return "Email đã được sử dụng!";
			}

			user.setUsername(bean.getUsername());
			user.setName(bean.getName());
			user.setEmail(bean.getEmail());
			user.setRole(bean.getRole());
			user.setStatus(bean.isActive());
			// Quy tắc an toàn: Admin không được phép chỉnh sửa mật khẩu người dùng. Giữ nguyên mật khẩu hiện tại.

			userJPA.save(user);
			return null;
		} else {
			if (userJPA.existsByUsername(bean.getUsername())) {
				return "Tên đăng nhập đã tồn tại!";
			}
			if (userJPA.existsByEmail(bean.getEmail())) {
				return "Email đã được sử dụng!";
			}

			User user = new User();
			user.setUsername(bean.getUsername());
			user.setPassword(bean.getPassword());
			user.setName(bean.getName());
			user.setEmail(bean.getEmail());
			user.setRole(bean.getRole());
			user.setStatus(bean.isActive());

			userJPA.save(user);
			return null;
		}
	}

	public String deleteUser(int id, Integer currentAdminId) {
		if (currentAdminId != null && currentAdminId == id) {
			return "Không thể xóa chính tài khoản đang đăng nhập!";
		}

		User user = userJPA.findById(id).orElse(null);
		if (user == null) {
			return "Không tìm thấy người dùng!";
		}

		// Nếu người dùng đã có lịch sử đơn hàng, chuyển trạng thái sang Khóa để bảo toàn dữ liệu
		if (orderJPA.existsByUserId(id)) {
			user.setStatus(false);
			userJPA.save(user);
			return null;
		}

		try {
			cartDetailJPA.deleteByUserId(id);
			userJPA.delete(user);
			return null;
		} catch (Exception e) {
			user.setStatus(false);
			userJPA.save(user);
			return null;
		}
	}
}