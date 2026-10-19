package com.fpoly.java5demo.controllers;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fpoly.java5demo.beans.ProductFormBean;
import com.fpoly.java5demo.beans.SellerRegisterBean;
import com.fpoly.java5demo.beans.ShopProfileBean;
import com.fpoly.java5demo.entities.Category;
import com.fpoly.java5demo.entities.Order;
import com.fpoly.java5demo.entities.OrderDetail;
import com.fpoly.java5demo.entities.Product;
import com.fpoly.java5demo.entities.SellerRequest;
import com.fpoly.java5demo.entities.User;
import com.fpoly.java5demo.jpas.SellerRequestJPA;
import com.fpoly.java5demo.services.CategoryServices;
import com.fpoly.java5demo.services.ImageServices;
import com.fpoly.java5demo.services.OrderServices;
import com.fpoly.java5demo.services.ProductServices;
import com.fpoly.java5demo.services.UserServices;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/seller")
public class SellerController {

	@Autowired
	private UserServices userServices;

	@Autowired
	private CategoryServices categoryServices;

	@Autowired
	private ProductServices productServices;

	@Autowired
	private OrderServices orderServices;

	@Autowired
	private SellerRequestJPA sellerRequestJPA;

	@Autowired
	private ImageServices imageServices;

	// 1. Trang giới thiệu & Đăng ký bán hàng
	@GetMapping("")
	public String sellerIndex(HttpServletRequest request, HttpSession session, Model model) {
		User currentUser = userServices.getLoggedInUser(request, session);
		if (currentUser != null && (currentUser.getRole() == 2 || currentUser.getRole() == 1)) {
			return "redirect:/seller/dashboard";
		}
		model.addAttribute("currentUser", currentUser);
		model.addAttribute("categories", categoryServices.getList());
		model.addAttribute("alreadySeller", false);

		if (!model.containsAttribute("sellerRegisterBean")) {
			SellerRegisterBean bean = new SellerRegisterBean();
			if (currentUser != null && currentUser.getPhone() != null) {
				bean.setPhone(currentUser.getPhone().trim());
			}
			model.addAttribute("sellerRegisterBean", bean);
		}

		if (currentUser != null) {
			java.util.Optional<SellerRequest> reqOpt =
					sellerRequestJPA.findFirstByUserIdOrderByIdDesc(currentUser.getId());
			if (reqOpt.isPresent()) {
				SellerRequest req = reqOpt.get();
				if (req.getStatus() == 0) {
					model.addAttribute("pendingRequest", req);
				} else if (req.getStatus() == 2) {
					model.addAttribute("rejectedRequest", req);
				}
			}
		}

		return "seller-register";
	}

	// 2. Xử lý gửi hồ sơ đăng ký mở gian hàng (Chờ Admin phê duyệt)
	@PostMapping("/register")
	public String processRegisterSeller(
			@Valid @ModelAttribute("sellerRegisterBean") SellerRegisterBean bean,
			Errors errors,
			HttpServletRequest request,
			HttpSession session,
			RedirectAttributes redirectAttributes,
			Model model) {

		User currentUser = userServices.getLoggedInUser(request, session);
		if (currentUser == null) {
			return "redirect:/login?redirect=/seller";
		}

		if (errors.hasErrors()) {
			model.addAttribute("currentUser", currentUser);
			model.addAttribute("categories", categoryServices.getList());
			model.addAttribute("alreadySeller", false);
			return "seller-register";
		}

		// Tạo yêu cầu mở gian hàng với trạng thái 0 (Chờ Admin duyệt)
		SellerRequest req = new SellerRequest();
		req.setUser(currentUser);
		req.setShopName(bean.getShopName().trim());
		req.setPhone(bean.getPhone().trim());
		req.setAddress(bean.getAddress().trim());
		if (bean.getCategoryId() != null && bean.getCategoryId() > 0) {
			Category cat = categoryServices.getById(bean.getCategoryId());
			if (cat != null) {
				req.setCategoryName(cat.getName());
			}
		}
		req.setCreatedAt(new java.sql.Date(System.currentTimeMillis()));
		req.setStatus(0); // 0 = Chờ duyệt
		sellerRequestJPA.save(req);

		// Cập nhật số điện thoại người dùng
		currentUser.setPhone(bean.getPhone().trim());
		userServices.save(currentUser);

		redirectAttributes.addFlashAttribute("successMessage",
				String.format("Hồ sơ đăng ký mở gian hàng '%s' đã được gửi thành công! Ban Quản Trị PolyShop sẽ xem xét và phê duyệt trong thời gian sớm nhất.",
						bean.getShopName().trim()));

		return "redirect:/seller";
	}

	// 3. Dashboard Kênh Người Bán
	@GetMapping("/dashboard")
	public String sellerDashboard(HttpServletRequest request, HttpSession session, Model model) {
		User currentUser = userServices.getLoggedInUser(request, session);
		if (currentUser == null) {
			return "redirect:/login?redirect=/seller/dashboard";
		}

		List<Product> products = productServices.getBySeller(currentUser.getId());
		List<Order> orders = getOrdersForSeller(currentUser.getId());

		model.addAttribute("currentUser", currentUser);
		model.addAttribute("productCount", products.size());
		model.addAttribute("orderCount", orders.size());
		model.addAttribute("products", products);
		return "seller/dashboard";
	}

	// 4. Quản lý sản phẩm của Người Bán
	@GetMapping({ "/products", "/product-form" })
	public String productForm(Model model, HttpServletRequest request, HttpSession session) {
		User currentUser = userServices.getLoggedInUser(request, session);
		if (currentUser == null) {
			return "redirect:/login?redirect=/seller/product-form";
		}

		model.addAttribute("currentUser", currentUser);
		model.addAttribute("bean", new ProductFormBean());
		model.addAttribute("products", productServices.getBySeller(currentUser.getId()));
		return "seller/product-form";
	}

	@GetMapping("/product-form/{id}")
	public String productEdit(@PathVariable("id") int id, Model model, HttpServletRequest request, HttpSession session,
			RedirectAttributes redirectAttributes) {
		User currentUser = userServices.getLoggedInUser(request, session);
		if (currentUser == null) {
			return "redirect:/login?redirect=/seller/product-form";
		}

		Product product = productServices.findById(id);
		if (product == null) {
			return "redirect:/seller/product-form";
		}

		// Ràng buộc bảo mật: Seller chỉ được chỉnh sửa sản phẩm do chính mình đăng
		if (product.getSeller() == null || (product.getSeller().getId() != currentUser.getId() && currentUser.getRole() != 1)) {
			redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền chỉnh sửa sản phẩm này!");
			return "redirect:/seller/product-form";
		}

		ProductFormBean bean = new ProductFormBean();
		bean.setId(product.getId());
		bean.setName(product.getName());
		bean.setPrice(product.getPrice());
		bean.setQuantity(product.getQuantity());
		bean.setStatus(product.isStatus() ? 1 : 2);
		if (product.getCategory() != null) {
			bean.setCategory(product.getCategory().getId());
		}

		model.addAttribute("currentUser", currentUser);
		model.addAttribute("bean", bean);
		model.addAttribute("product", product);
		model.addAttribute("products", productServices.getBySeller(currentUser.getId()));
		return "seller/product-form";
	}

	@PostMapping("/product-form")
	public String productSave(@Valid @ModelAttribute("bean") ProductFormBean bean, Errors errors,
			Model model, RedirectAttributes redirectAttributes, HttpServletRequest request, HttpSession session) {
		User currentUser = userServices.getLoggedInUser(request, session);
		if (currentUser == null) {
			return "redirect:/login?redirect=/seller/product-form";
		}

		if (errors.hasErrors()) {
			model.addAttribute("currentUser", currentUser);
			model.addAttribute("products", productServices.getBySeller(currentUser.getId()));
			return "seller/product-form";
		}

		Category category = categoryServices.getById(bean.getCategory());
		if (category == null) {
			model.addAttribute("errorMessage", "Vui lòng chọn danh mục hợp lệ!");
			model.addAttribute("currentUser", currentUser);
			model.addAttribute("products", productServices.getBySeller(currentUser.getId()));
			return "seller/product-form";
		}

		Product product;
		boolean isUpdate = (bean.getId() != null && bean.getId() > 0);

		if (isUpdate) {
			product = productServices.findById(bean.getId());
			if (product == null) {
				return "redirect:/seller/product-form";
			}
			// Ràng buộc bảo mật: Không cho sửa sản phẩm của shop khác
			if (product.getSeller() == null || (product.getSeller().getId() != currentUser.getId() && currentUser.getRole() != 1)) {
				redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền chỉnh sửa sản phẩm của shop khác!");
				return "redirect:/seller/product-form";
			}
		} else {
			product = new Product();
			product.setSeller(currentUser); // Gán quyền sở hữu sản phẩm cho Seller hiện tại
		}

		product.setName(bean.getName());
		product.setPrice(bean.getPrice());
		product.setQuantity(bean.getQuantity());
		product.setStatus(bean.getStatus() == 1);
		product.setCategory(category);

		String error;
		if (isUpdate) {
			error = productServices.updateProduct(product, bean.getImages());
		} else {
			error = productServices.addProduct(product, bean.getImages());
		}

		if (error != null) {
			model.addAttribute("errorMessage", error);
			model.addAttribute("currentUser", currentUser);
			model.addAttribute("products", productServices.getBySeller(currentUser.getId()));
			return "seller/product-form";
		}

		redirectAttributes.addFlashAttribute("successMessage",
				isUpdate ? "Cập nhật sản phẩm thành công!" : "Đăng bán sản phẩm mới thành công!");
		return "redirect:/seller/product-form";
	}

	@PostMapping("/product/delete/{id}")
	public String productDelete(@PathVariable("id") int id, HttpServletRequest request, HttpSession session,
			RedirectAttributes redirectAttributes) {
		User currentUser = userServices.getLoggedInUser(request, session);
		if (currentUser == null) {
			return "redirect:/login?redirect=/seller/product-form";
		}

		Product product = productServices.findById(id);
		if (product == null) {
			redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy sản phẩm!");
			return "redirect:/seller/product-form";
		}

		// Ràng buộc bảo mật: Chặn xóa sản phẩm của người khác
		if (product.getSeller() == null || (product.getSeller().getId() != currentUser.getId() && currentUser.getRole() != 1)) {
			redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền xóa sản phẩm của shop khác!");
			return "redirect:/seller/product-form";
		}

		String error = productServices.remove(id);
		if (error == null) {
			redirectAttributes.addFlashAttribute("successMessage", "Xóa/ẩn sản phẩm thành công!");
		} else {
			redirectAttributes.addFlashAttribute("errorMessage", error);
		}
		return "redirect:/seller/product-form";
	}

	// 5. Quản lý đơn hàng của Người Bán
	@GetMapping("/orders")
	public String listOrders(Model model, HttpServletRequest request, HttpSession session) {
		User currentUser = userServices.getLoggedInUser(request, session);
		if (currentUser == null) {
			return "redirect:/login?redirect=/seller/orders";
		}

		model.addAttribute("currentUser", currentUser);
		model.addAttribute("orders", getOrdersForSeller(currentUser.getId()));
		return "seller/order-list";
	}

	@GetMapping("/order/{id}")
	public String viewOrder(@PathVariable("id") int id, Model model, HttpServletRequest request, HttpSession session) {
		User currentUser = userServices.getLoggedInUser(request, session);
		if (currentUser == null) {
			return "redirect:/login?redirect=/seller/orders";
		}

		Order order = orderServices.getOrderById(id);
		if (order == null) {
			return "redirect:/seller/orders";
		}

		// Lọc chỉ lấy các món hàng thuộc shop của người bán này
		List<OrderDetail> allDetails = orderServices.getOrderDetails(id);
		List<OrderDetail> sellerDetails = new ArrayList<>();
		for (OrderDetail od : allDetails) {
			if (od.getProduct() != null && od.getProduct().getSeller() != null
					&& od.getProduct().getSeller().getId() == currentUser.getId()) {
				sellerDetails.add(od);
			}
		}

		// Nếu đơn không có món nào thuộc shop của seller này (và không phải Admin) thì chặn xem
		if (sellerDetails.isEmpty() && currentUser.getRole() != 1) {
			return "redirect:/seller/orders";
		}

		model.addAttribute("currentUser", currentUser);
		model.addAttribute("order", order);
		model.addAttribute("orderDetails", (currentUser.getRole() == 1) ? allDetails : sellerDetails);
		return "seller/order-detail";
	}

	@PostMapping("/order/status")
	public String updateStatus(@RequestParam("orderId") int orderId,
			@RequestParam("status") int status, HttpServletRequest request, HttpSession session,
			RedirectAttributes redirectAttributes) {
		User currentUser = userServices.getLoggedInUser(request, session);
		if (currentUser == null) {
			return "redirect:/login?redirect=/seller/orders";
		}

		// Kiểm tra đơn hàng có chứa sản phẩm của shop này không
		boolean belongsToSeller = false;
		for (OrderDetail od : orderServices.getOrderDetails(orderId)) {
			if (od.getProduct() != null && od.getProduct().getSeller() != null
					&& od.getProduct().getSeller().getId() == currentUser.getId()) {
				belongsToSeller = true;
				break;
			}
		}

		if (!belongsToSeller && currentUser.getRole() != 1) {
			redirectAttributes.addFlashAttribute("errorMessage", "Bạn không có quyền cập nhật đơn hàng này!");
			return "redirect:/seller/orders";
		}

		boolean ok = orderServices.updateStatus(orderId, status);
		if (ok) {
			redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái đơn hàng thành công!");
		} else {
			redirectAttributes.addFlashAttribute("errorMessage", "Không thể cập nhật trạng thái đơn hàng!");
		}
		return String.format("redirect:/seller/order/%d", orderId);
	}

	// 6. Quản lý Hồ sơ Gian hàng (Seller Profile)
	@GetMapping("/profile")
	public String sellerProfile(Model model, HttpServletRequest request, HttpSession session) {
		User currentUser = userServices.getLoggedInUser(request, session);
		if (currentUser == null) {
			return "redirect:/login?redirect=/seller/profile";
		}
		if (currentUser.getRole() != 2 && currentUser.getRole() != 1) {
			return "redirect:/seller";
		}

		SellerRequest req = sellerRequestJPA.findFirstByUserIdAndStatusOrderByIdDesc(currentUser.getId(), 1)
				.orElseGet(() -> sellerRequestJPA.findFirstByUserIdOrderByIdDesc(currentUser.getId())
						.orElse(null));

		if (req == null) {
			req = new SellerRequest();
			req.setUser(currentUser);
			req.setShopName(currentUser.getName() != null ? currentUser.getName() : "Gian hàng của tôi");
			req.setPhone(currentUser.getPhone() != null ? currentUser.getPhone() : "");
			req.setAddress("Hà Nội, Việt Nam");
			req.setCategoryName("Thời Trang Nam");
			req.setCreatedAt(new java.sql.Date(System.currentTimeMillis()));
			req.setStatus(1);
			req = sellerRequestJPA.save(req);
		}

		ShopProfileBean bean = new ShopProfileBean();
		bean.setShopName(req.getShopName());
		bean.setPhone(req.getPhone());
		bean.setAddress(req.getAddress());
		bean.setDescription(req.getDescription());
		bean.setCategoryName(req.getCategoryName());
		bean.setCurrentAvatar(req.getAvatar());

		List<Product> products = productServices.getBySeller(currentUser.getId());
		List<Order> orders = getOrdersForSeller(currentUser.getId());

		model.addAttribute("currentUser", currentUser);
		model.addAttribute("shopProfileBean", bean);
		model.addAttribute("sellerRequest", req);
		model.addAttribute("productCount", products.size());
		model.addAttribute("orderCount", orders.size());
		return "seller/profile";
	}

	@PostMapping("/profile")
	public String updateSellerProfile(
			@Valid @ModelAttribute("shopProfileBean") ShopProfileBean bean,
			Errors errors,
			Model model,
			HttpServletRequest request,
			HttpSession session,
			RedirectAttributes redirectAttributes) {

		User currentUser = userServices.getLoggedInUser(request, session);
		if (currentUser == null) {
			return "redirect:/login?redirect=/seller/profile";
		}
		if (currentUser.getRole() != 2 && currentUser.getRole() != 1) {
			return "redirect:/seller";
		}

		SellerRequest req = sellerRequestJPA.findFirstByUserIdAndStatusOrderByIdDesc(currentUser.getId(), 1)
				.orElseGet(() -> sellerRequestJPA.findFirstByUserIdOrderByIdDesc(currentUser.getId())
						.orElse(null));

		if (req == null) {
			req = new SellerRequest();
			req.setUser(currentUser);
			req.setCreatedAt(new java.sql.Date(System.currentTimeMillis()));
			req.setStatus(1);
		}

		if (errors.hasErrors()) {
			List<Product> products = productServices.getBySeller(currentUser.getId());
			List<Order> orders = getOrdersForSeller(currentUser.getId());
			bean.setCurrentAvatar(req.getAvatar());
			model.addAttribute("currentUser", currentUser);
			model.addAttribute("sellerRequest", req);
			model.addAttribute("productCount", products.size());
			model.addAttribute("orderCount", orders.size());
			return "seller/profile";
		}

		req.setShopName(bean.getShopName().trim());
		req.setPhone(bean.getPhone().trim());
		req.setAddress(bean.getAddress().trim());
		req.setDescription(bean.getDescription() != null ? bean.getDescription().trim() : null);

		if (bean.getAvatarFile() != null && !bean.getAvatarFile().isEmpty()) {
			try {
				String avatarName = imageServices.save(bean.getAvatarFile());
				req.setAvatar(avatarName);
			} catch (Exception e) {
				model.addAttribute("errorMessage", "Không thể tải lên ảnh đại diện: " + e.getMessage());
				List<Product> products = productServices.getBySeller(currentUser.getId());
				List<Order> orders = getOrdersForSeller(currentUser.getId());
				bean.setCurrentAvatar(req.getAvatar());
				model.addAttribute("currentUser", currentUser);
				model.addAttribute("sellerRequest", req);
				model.addAttribute("productCount", products.size());
				model.addAttribute("orderCount", orders.size());
				return "seller/profile";
			}
		}

		sellerRequestJPA.save(req);

		// Cập nhật SĐT người dùng
		currentUser.setPhone(bean.getPhone().trim());
		userServices.save(currentUser);

		redirectAttributes.addFlashAttribute("successMessage", "Cập nhật hồ sơ gian hàng thành công!");
		return "redirect:/seller/profile";
	}

	private List<Order> getOrdersForSeller(int sellerId) {
		return orderServices.getOrdersBySeller(sellerId);
	}

	@ModelAttribute("categories")
	public List<Category> getCategories() {
		return categoryServices.getList();
	}
}
