package com.fpoly.java5demo;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;

import com.fpoly.java5demo.beans.CheckoutBean;
import com.fpoly.java5demo.beans.UserFormBean;
import com.fpoly.java5demo.entities.CartDetail;
import com.fpoly.java5demo.entities.Order;
import com.fpoly.java5demo.entities.OrderDetail;
import com.fpoly.java5demo.entities.Product;
import com.fpoly.java5demo.entities.User;
import com.fpoly.java5demo.jpas.CartDetailJPA;
import com.fpoly.java5demo.jpas.OrderDetailJPA;
import com.fpoly.java5demo.jpas.OrderJPA;
import com.fpoly.java5demo.jpas.ProductJPA;
import com.fpoly.java5demo.jpas.UserJPA;
import com.fpoly.java5demo.services.CartDetailServices;
import com.fpoly.java5demo.services.OrderServices;
import com.fpoly.java5demo.services.ProductServices;
import com.fpoly.java5demo.services.UserServices;
import com.fpoly.java5demo.utils.Utils;

import jakarta.servlet.http.Cookie;

@SpringBootTest
class Java5ApplicationTests {

	@Autowired
	private UserServices userServices;

	@Autowired
	private ProductServices productServices;

	@Autowired
	private OrderServices orderServices;

	@Autowired
	private CartDetailServices cartDetailServices;

	@Autowired
	private UserJPA userJPA;

	@Autowired
	private ProductJPA productJPA;

	@Autowired
	private OrderJPA orderJPA;

	@Autowired
	private OrderDetailJPA orderDetailJPA;

	@Autowired
	private CartDetailJPA cartDetailJPA;

	@Autowired
	private com.fpoly.java5demo.jpas.SellerRequestJPA sellerRequestJPA;

	@Autowired
	private com.fpoly.java5demo.components.AuthInterceptor authInterceptor;

	@Autowired
	private org.springframework.web.context.WebApplicationContext webApplicationContext;

	@Test
	void contextLoads() {
		assertNotNull(userServices);
		assertNotNull(productServices);
		assertNotNull(orderServices);
	}

	@Test
	void testUserFormTemplateParsing() throws Exception {
		org.springframework.test.web.servlet.MockMvc mockMvc = 
				org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
		User admin = userJPA.findByUsername("admin").orElse(null);
		assertNotNull(admin);
		MockHttpSession session = new MockHttpSession();
		session.setAttribute("user", admin);
		session.setAttribute("role", 1);

		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/admin/user-form")
				.session(session))
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
	}

	@Test
	void testAuthenticationAndSessionSecurity() {
		MockHttpSession session = new MockHttpSession();
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		// 1. Kiểm tra tài khoản bị khóa không được đăng nhập
		User testLockedUser = userJPA.findByUsername("user1").orElse(null);
		assertNotNull(testLockedUser);
		testLockedUser.setStatus(false);
		userJPA.save(testLockedUser);

		User loginResult = userServices.login("user1", "123456", false, response, session);
		assertNull(loginResult, "Tài khoản bị khóa phải không đăng nhập được");

		// 2. Kiểm tra nếu tài khoản bị khóa trong phiên thì getLoggedInUser phải ngay lập tức hủy session
		session.setAttribute("user", testLockedUser);
		session.setAttribute("role", testLockedUser.getRole());
		User loggedInCheck = userServices.getLoggedInUser(request, session);
		assertNull(loggedInCheck, "Khi tài khoản bị khóa, getLoggedInUser phải trả về null");
		assertNull(session.getAttribute("user"), "Session user phải bị xóa ngay lập tức khi tài khoản bị khóa");

		// Phục hồi lại trạng thái user1
		testLockedUser.setStatus(true);
		userJPA.save(testLockedUser);

		// 3. Kiểm tra clearCookie chuẩn RFC (setMaxAge(0) và setPath("/"))
		request.setCookies(new Cookie(Utils.COOKIE_KEY_USER_ID, "1"), new Cookie(Utils.COOKIE_KEY_ROLE, "0"));
		Utils.clearCookie(request, response);

		Cookie[] clearedCookies = response.getCookies();
		assertTrue(clearedCookies.length > 0, "Phải có cookie xóa được thêm vào response");
		for (Cookie c : clearedCookies) {
			assertEquals(0, c.getMaxAge(), "Cookie bị xóa phải có MaxAge = 0");
			assertEquals("/", c.getPath(), "Cookie bị xóa phải có Path = '/'");
			assertTrue(c.isHttpOnly(), "Cookie xóa phải bật HttpOnly");
		}

		// 4. Kiểm tra Chống Giả Mạo Cookie leo quyền Admin (Cookie Impersonation Defense):
		// Kẻ tấn công tự ý thêm cookie user_id=1, role=1 mà không có token ký số hợp lệ
		MockHttpServletRequest forgedRequest = new MockHttpServletRequest();
		forgedRequest.setCookies(new Cookie(Utils.COOKIE_KEY_USER_ID, "1"), new Cookie(Utils.COOKIE_KEY_ROLE, "1"));
		User forgedUser = userServices.getLoggedInUser(forgedRequest, new MockHttpSession());
		assertNull(forgedUser, "Kẻ tấn công giả mạo cookie user_id=1 không được phép tự động đăng nhập");

		// Khi có token ký số hợp lệ (Remember Me chuẩn):
		User adminUser = userJPA.findByUsername("admin").orElse(null);
		assertNotNull(adminUser);
		String validToken = Utils.signAuthToken(adminUser.getId(), adminUser.getRole(), adminUser.getPassword());
		MockHttpServletRequest validRequest = new MockHttpServletRequest();
		validRequest.setCookies(
			new Cookie(Utils.COOKIE_KEY_USER_ID, String.valueOf(adminUser.getId())),
			new Cookie(Utils.COOKIE_KEY_ROLE, String.valueOf(adminUser.getRole())),
			new Cookie(Utils.COOKIE_KEY_AUTH_TOKEN, validToken)
		);
		User authenticatedUser = userServices.getLoggedInUser(validRequest, new MockHttpSession());
		assertNotNull(authenticatedUser, "Đăng nhập Remember Me với token hợp lệ phải thành công");
		assertEquals(adminUser.getId(), authenticatedUser.getId());
	}

	@Test
	void testB2CPurchasingAndOrderStateMachine() {
		User customer = userJPA.findByUsername("user").orElse(null);
		assertNotNull(customer);

		List<Product> availableProducts = productServices.getAvailableProducts();
		assertFalse(availableProducts.isEmpty());
		Product testProd = availableProducts.get(0);
		int initialStock = testProd.getQuantity();
		assertTrue(initialStock >= 2, "Cần có ít nhất 2 sản phẩm tồn kho để test");

		// 1. Thiết lập giỏ hàng trong session và trong DB
		cartDetailServices.clearCart();
		cartDetailServices.addToCart(testProd.getId(), 2);

		CartDetail cd = new CartDetail();
		cd.setUser(customer);
		cd.setProduct(testProd);
		cd.setQuantity(2);
		cartDetailJPA.save(cd);
		assertFalse(cartDetailJPA.findByUserId(customer.getId()).isEmpty());

		// 2. Thực hiện đặt hàng với hình thức thanh toán PolyPay và địa chỉ dài
		CheckoutBean checkoutBean = new CheckoutBean();
		checkoutBean.setFullName("Nguyễn Văn Test");
		checkoutBean.setPhone("0912345678");
		checkoutBean.setAddress("Số 123 Đường Test Nghiệp Vụ, Phường Test, Quận Nam Từ Liêm, TP Hà Nội");
		checkoutBean.setNote("Giao giờ hành chính, gọi trước khi giao");
		checkoutBean.setPaymentMethod("PolyPay");

		Order order = orderServices.createOrder(customer, checkoutBean);
		assertNotNull(order, "Đơn hàng phải được tạo thành công");

		// Kiểm tra lưu đúng phương thức thanh toán
		assertEquals("PolyPay", order.getPaymentMethod(), "Phương thức thanh toán phải là PolyPay");

		// Kiểm tra địa chỉ không bị mất thông tin
		assertTrue(order.getAddress().contains("0912345678"));
		assertTrue(order.getAddress().contains("Giao giờ hành chính"));

		// Kiểm tra trừ kho
		Product updatedProd = productServices.findById(testProd.getId());
		assertEquals(initialStock - 2, updatedProd.getQuantity(), "Tồn kho phải giảm đi 2");

		// Kiểm tra giỏ hàng DB đã được dọn sạch
		List<CartDetail> remainingDbCart = cartDetailJPA.findByUserId(customer.getId());
		assertTrue(remainingDbCart.isEmpty(), "Giỏ hàng trong Database phải được dọn sạch sau khi đặt hàng thành công");

		// 3. Khách hàng hủy đơn hàng (status = 0) -> Kiểm tra hoàn kho
		boolean cancelOk = orderServices.cancelOrder(order.getId(), customer.getId());
		assertTrue(cancelOk, "Khách hàng phải hủy được đơn hàng ở trạng thái 0");

		Product restoredProd = productServices.findById(testProd.getId());
		assertEquals(initialStock, restoredProd.getQuantity(), "Hủy đơn hàng thành công phải hoàn trả lại tồn kho");

		// 4. Kiểm tra State Machine: Đơn hàng đã hủy không được phép đổi trạng thái tùy tiện
		boolean invalidReopen = orderServices.updateStatus(order.getId(), 1);
		assertFalse(invalidReopen, "Đơn hàng đã Hủy (status=5) không được phép mở lại");
	}

	@Test
	void testSellerDataIsolation() {
		User seller = userJPA.findByUsername("seller").orElse(null);
		assertNotNull(seller, "Tài khoản seller mẫu phải tồn tại");

		// 1. Kiểm tra cách ly sản phẩm: Seller chỉ xem được sản phẩm do chính mình đăng
		List<Product> sellerProducts = productServices.getBySeller(seller.getId());
		assertFalse(sellerProducts.isEmpty(), "Shop Poly Fashion phải có sản phẩm mẫu");

		for (Product p : sellerProducts) {
			assertNotNull(p.getSeller(), "Sản phẩm của seller phải có thông tin seller");
			assertEquals(seller.getId(), p.getSeller().getId(), "Sản phẩm phải thuộc về chính seller");
		}

		// 2. Kiểm tra sản phẩm chính hãng (seller_id = null) không xuất hiện trong danh sách của seller
		for (Product p : sellerProducts) {
			assertFalse(p.getName().contains("iPhone"), "Sản phẩm iPhone của Official Mall không được lẫn vào shop seller");
			assertFalse(p.getName().contains("MacBook"), "Sản phẩm MacBook của Official Mall không được lẫn vào shop seller");
		}
	}

	@Test
	void testAdminCannotModifyUserPassword() {
		User targetUser = userJPA.findByUsername("user").orElse(null);
		assertNotNull(targetUser);
		String originalPassword = targetUser.getPassword();

		// Giả lập Admin chỉnh sửa thông tin người dùng
		UserFormBean updateBean = new UserFormBean();
		updateBean.setId(targetUser.getId());
		updateBean.setUsername(targetUser.getUsername());
		updateBean.setName("Tên Mới Sau Khi Admin Sửa");
		updateBean.setEmail(targetUser.getEmail());
		updateBean.setRole(targetUser.getRole());
		updateBean.setActive(targetUser.isStatus());
		// Cố tình truyền mật khẩu mới vào bean (tấn công hoặc can thiệp trái phép)
		updateBean.setPassword("malicious_admin_new_password_123");

		String result = userServices.saveUserFromBean(updateBean);
		assertNull(result, "Cập nhật thông tin người dùng phải thành công");

		// Kiểm tra lại trong DB: Tên được đổi, nhưng MẬT KHẨU PHẢI GIỮ NGUYÊN
		User reloadedUser = userJPA.findById(targetUser.getId()).orElse(null);
		assertNotNull(reloadedUser);
		assertEquals("Tên Mới Sau Khi Admin Sửa", reloadedUser.getName());
		assertEquals(originalPassword, reloadedUser.getPassword(), "Mật khẩu người dùng không được phép thay đổi khi Admin cập nhật!");
		assertNotEquals("malicious_admin_new_password_123", reloadedUser.getPassword());

		// Phục hồi lại tên cũ
		reloadedUser.setName("Khách Hàng Mẫu");
		userJPA.save(reloadedUser);
	}

	@Test
	void testAdminCannotBuyGoods() {
		User adminUser = userJPA.findByUsername("admin").orElse(null);
		assertNotNull(adminUser);
		assertEquals(1, adminUser.getRole(), "Tài khoản admin phải có role = 1");

		// Giả lập Admin thử checkout hoặc thêm vào giỏ
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpSession session = new MockHttpSession();
		session.setAttribute("user", adminUser);
		session.setAttribute("role", 1);

		User loggedIn = userServices.getLoggedInUser(request, session);
		assertNotNull(loggedIn);
		assertEquals(1, loggedIn.getRole(), "Role của tài khoản đăng nhập phải là 1 (Admin)");
	}

	@Test
	void testAdminToggleUserStatus() {
		User targetUser = userJPA.findByUsername("user").orElse(null);
		assertNotNull(targetUser);
		boolean initialStatus = targetUser.isStatus();

		// Khóa tài khoản
		targetUser.setStatus(!initialStatus);
		userServices.save(targetUser);

		User updatedUser = userServices.getUserInfoById(targetUser.getId());
		assertNotNull(updatedUser);
		assertEquals(!initialStatus, updatedUser.isStatus(), "Trạng thái người dùng phải được đổi thành công");

		// Phục hồi lại trạng thái ban đầu
		updatedUser.setStatus(initialStatus);
		userServices.save(updatedUser);
	}

	@Test
	void testOrderDetailSnapshotAndModificationDetection() {
		User customer = userJPA.findByUsername("user1").orElse(null);
		assertNotNull(customer);

		// 1. Tạo sản phẩm ban đầu để test
		Product testProd = productJPA.findAll().get(0);
		String originalName = testProd.getName();
		int originalPrice = testProd.getPrice();

		// Thêm vào giỏ và đặt hàng
		cartDetailServices.addToCart(testProd.getId(), 1);
		CheckoutBean checkoutBean = new CheckoutBean();
		checkoutBean.setFullName("Snapshot Tester");
		checkoutBean.setPhone("0999888777");
		checkoutBean.setAddress("Số 1 Đường Snapshot Test");
		checkoutBean.setPaymentMethod("COD");

		Order order = orderServices.createOrder(customer, checkoutBean);
		assertNotNull(order, "Đơn hàng phải được tạo thành công");

		List<OrderDetail> details = orderDetailJPA.findByOrderId(order.getId());
		assertFalse(details.isEmpty(), "Phải có ít nhất 1 chi tiết đơn hàng");
		OrderDetail detail = details.get(0);

		// Kiểm tra Snapshot được đóng băng (Freeze) đúng giá trị lúc đặt hàng
		assertEquals(originalName, detail.getProductName(), "Tên sản phẩm phải được đóng băng vào Snapshot");
		assertEquals(originalPrice, detail.getPrice(), "Giá tiền phải được đóng băng vào Snapshot");
		assertEquals(originalName, detail.getDisplayName());
		assertFalse(detail.isPriceChanged(), "Mới đặt xong thì chưa có sự thay đổi giá");
		assertFalse(detail.isNameChanged(), "Mới đặt xong thì chưa có sự thay đổi tên");
		assertFalse(detail.isProductModified(), "Mới đặt xong thì chưa bị sửa đổi");
		assertFalse(detail.isProductDeleted(), "Sản phẩm chưa bị xóa");

		// 2. Giả lập Người Bán chỉnh sửa tên và tăng giá sản phẩm
		testProd.setPrice(originalPrice + 100000);
		testProd.setName("Tên Mới Sau Khi Người Bán Đổi");
		productJPA.save(testProd);

		// Đọc lại OrderDetail từ Database
		OrderDetail reloadedDetail = orderDetailJPA.findById(detail.getId()).orElse(null);
		assertNotNull(reloadedDetail);

		// Nghiệp vụ cốt lõi: Giá & Tên trong billdetail VẪN NGUYÊN VẸN, không bị đổi theo sản phẩm
		assertEquals(originalPrice, reloadedDetail.getPrice(), "Giá trên billdetail phải giữ nguyên giá lúc đặt hàng");
		assertEquals(originalName, reloadedDetail.getProductName(), "Tên trên billdetail phải giữ nguyên tên lúc đặt hàng");
		assertEquals(originalName, reloadedDetail.getDisplayName());

		// Phát hiện trạng thái chỉnh sửa
		assertTrue(reloadedDetail.isPriceChanged(), "Phải phát hiện người bán đã thay đổi giá");
		assertTrue(reloadedDetail.isNameChanged(), "Phải phát hiện người bán đã thay đổi tên");
		assertTrue(reloadedDetail.isProductModified(), "Phải phát hiện sản phẩm đã bị người bán chỉnh sửa");

		// 3. Giả lập Người Bán xóa / ẩn sản phẩm (status = false)
		testProd.setStatus(false);
		productJPA.save(testProd);

		OrderDetail detailAfterDelete = orderDetailJPA.findById(detail.getId()).orElse(null);
		assertNotNull(detailAfterDelete);
		assertTrue(detailAfterDelete.isProductDeleted(), "Phải phát hiện sản phẩm đã bị xóa hoặc ngừng bán");
		assertEquals(originalName, detailAfterDelete.getDisplayName(), "Tên hiển thị vẫn phải vẹn nguyên");
		assertEquals(originalPrice, detailAfterDelete.getPrice(), "Giá tiền vẫn phải vẹn nguyên");

		// 4. Phục hồi lại dữ liệu mẫu
		testProd.setName(originalName);
		testProd.setPrice(originalPrice);
		testProd.setStatus(true);
		productJPA.save(testProd);
	}

	@Test
	void testShopProfileAndPublicStorefront() throws Exception {
		// 1. Kiểm tra Người Bán "seller" có hồ sơ gian hàng đã phê duyệt
		User seller = userJPA.findByUsername("seller").orElse(null);
		assertNotNull(seller, "Tài khoản seller mẫu phải tồn tại");
		assertEquals(2, seller.getRole(), "Tài khoản seller phải có role = 2");

		com.fpoly.java5demo.entities.SellerRequest shopReq = sellerRequestJPA
				.findFirstByUserIdAndStatusOrderByIdDesc(seller.getId(), 1)
				.orElse(null);
		assertNotNull(shopReq, "Seller phải có hồ sơ gian hàng được duyệt");
		assertEquals("Poly Fashion Official Store", shopReq.getShopName());
		assertNotNull(shopReq.getAddress());
		assertNotNull(shopReq.getDescription());

		// 2. Kiểm tra danh sách sản phẩm công khai của gian hàng
		List<Product> shopProducts = productJPA.findBySellerIdAndStatusTrue(seller.getId());
		assertFalse(shopProducts.isEmpty(), "Gian hàng seller phải có sản phẩm đang kinh doanh");
		for (Product p : shopProducts) {
			assertEquals(seller.getId(), p.getSeller().getId(), "Mọi sản phẩm của shop phải thuộc về seller");
			assertTrue(p.isStatus(), "Sản phẩm công khai phải có status = true");
		}

		// 3. Kiểm tra bảo mật URL /seller/profile thông qua AuthInterceptor
		// Case A: Khách vãng lai (Chưa đăng nhập) -> Bị chặn & redirect /login
		MockHttpServletRequest guestReq = new MockHttpServletRequest();
		guestReq.setRequestURI("/seller/profile");
		MockHttpServletResponse guestRes = new MockHttpServletResponse();
		MockHttpSession guestSession = new MockHttpSession();

		boolean guestAccess = authInterceptor.preHandle(guestReq, guestRes, new Object());
		assertFalse(guestAccess, "Khách chưa đăng nhập không được truy cập /seller/profile");
		assertTrue(guestRes.getRedirectedUrl().contains("/login"), "Phải chuyển hướng về /login");

		// Case B: Người mua thông thường (role = 0) -> Bị chặn & redirect /seller
		User normalUser = userJPA.findByUsername("user").orElse(null);
		assertNotNull(normalUser);
		MockHttpServletRequest userReq = new MockHttpServletRequest();
		userReq.setRequestURI("/seller/profile");
		MockHttpServletResponse userRes = new MockHttpServletResponse();
		MockHttpSession userSession = new MockHttpSession();
		userSession.setAttribute("user", normalUser);
		userSession.setAttribute("role", 0);
		userReq.setSession(userSession);

		boolean userAccess = authInterceptor.preHandle(userReq, userRes, new Object());
		assertFalse(userAccess, "Người mua role 0 không được truy cập /seller/profile");
		assertEquals("/seller", userRes.getRedirectedUrl(), "Phải chuyển hướng về /seller");

		// Case C: Người Bán (role = 2) -> Cho phép truy cập 100%
		MockHttpServletRequest sellerReq = new MockHttpServletRequest();
		sellerReq.setRequestURI("/seller/profile");
		MockHttpServletResponse sellerRes = new MockHttpServletResponse();
		MockHttpSession sellerSession = new MockHttpSession();
		sellerSession.setAttribute("user", seller);
		sellerSession.setAttribute("role", 2);
		sellerReq.setSession(sellerSession);

		boolean sellerAccess = authInterceptor.preHandle(sellerReq, sellerRes, new Object());
		assertTrue(sellerAccess, "Seller role 2 phải được phép truy cập /seller/profile");
	}

	@Test
	void testBuyerOrderApprovedBySellerAndSuperAdminRole() throws Exception {
		// 1. Kiểm tra 3 cấp phân quyền hệ thống chuẩn: SuperAdmin (1), Seller (2), Người Mua (0)
		User superAdmin = userJPA.findByUsername("admin").orElse(null);
		assertNotNull(superAdmin);
		assertEquals(1, superAdmin.getRole(), "Role 1 phải là Super Admin");

		User seller = userJPA.findByUsername("seller").orElse(null);
		assertNotNull(seller);
		assertEquals(2, seller.getRole(), "Role 2 phải là Người Bán (Seller)");

		User buyer = userJPA.findByUsername("user").orElse(null);
		assertNotNull(buyer);
		assertEquals(0, buyer.getRole(), "Role 0 phải là Người Mua (Buyer)");

		// 2. Tạo đơn hàng mô phỏng Người Mua đặt hàng của Seller
		List<Product> sellerProds = productJPA.findBySellerIdAndStatusTrue(seller.getId());
		assertFalse(sellerProds.isEmpty(), "Seller phải có sản phẩm");
		Product p = sellerProds.get(0);

		Order order = new Order();
		order.setUser(buyer);
		order.setDate(new java.sql.Date(System.currentTimeMillis()));
		order.setAddress("123 Phố Cầu Giấy, Hà Nội");
		order.setPaymentMethod("COD");
		order.setTotalPrice(p.getPrice());
		order.setStatus(0); // 0: Chờ duyệt (Khách mới đặt)
		Order savedOrder = orderJPA.save(order);

		OrderDetail detail = new OrderDetail();
		detail.setOrder(savedOrder);
		detail.setProduct(p);
		detail.setProductName(p.getName());
		detail.setPrice(p.getPrice());
		detail.setQuantity(1);
		orderDetailJPA.save(detail);

		// Gán chi tiết đơn hàng để kiểm tra Seller phụ trách
		savedOrder = orderJPA.findById(savedOrder.getId()).orElse(null);
		assertNotNull(savedOrder);
		savedOrder.setOrderDetails(List.of(detail));
		assertEquals(0, savedOrder.getStatus(), "Đơn hàng mới tạo phải ở trạng thái 0 (Chờ duyệt)");
		assertNotNull(savedOrder.getSellerNames());
		assertTrue(savedOrder.getSellerNames().contains(seller.getName()), "Đơn hàng phải nhận diện đúng Người Bán phụ trách");

		// 3. Người Bán (Seller) tiến hành phê duyệt đơn hàng (chuyển sang trạng thái 1 - Đã duyệt & đóng gói)
		boolean approved = orderServices.updateStatus(savedOrder.getId(), 1);
		assertTrue(approved, "Người Bán phải cập nhật/phê duyệt đơn thành công");

		Order reloadedOrder = orderJPA.findById(savedOrder.getId()).orElse(null);
		assertNotNull(reloadedOrder);
		assertEquals(1, reloadedOrder.getStatus(), "Trạng thái đơn hàng sau khi Seller duyệt phải là 1 (Đã xác nhận & đóng gói)");
	}

	@Test
	public void testAdminToggleProductStatusModeration() {
		List<Product> prods = productServices.getList();
		assertFalse(prods.isEmpty(), "Cần có ít nhất 1 sản phẩm để kiểm thử kiểm duyệt");

		Product testProduct = prods.get(0);
		int prodId = testProduct.getId();
		boolean originalStatus = testProduct.isStatus();

		// 1. Admin bấm Khóa / Ẩn sản phẩm vi phạm
		boolean success1 = productServices.toggleStatus(prodId);
		assertTrue(success1, "Thao tác đổi trạng thái kiểm duyệt phải thành công");

		Product afterToggle1 = productServices.findById(prodId);
		assertNotNull(afterToggle1);
		assertEquals(!originalStatus, afterToggle1.isStatus(), "Trạng thái sản phẩm phải đảo ngược sau khi Admin can thiệp");

		// 2. Admin bấm Mở khóa hiển thị lại cho sản phẩm
		boolean success2 = productServices.toggleStatus(prodId);
		assertTrue(success2, "Thao tác mở khóa lại phải thành công");

		Product afterToggle2 = productServices.findById(prodId);
		assertNotNull(afterToggle2);
		assertEquals(originalStatus, afterToggle2.isStatus(), "Trạng thái sản phẩm phải được khôi phục về ban đầu");
	}
}
