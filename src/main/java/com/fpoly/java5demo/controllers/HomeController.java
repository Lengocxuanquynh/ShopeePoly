package com.fpoly.java5demo.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fpoly.java5demo.entities.Category;
import com.fpoly.java5demo.entities.OrderDetail;
import com.fpoly.java5demo.entities.Product;
import com.fpoly.java5demo.entities.SellerRequest;
import com.fpoly.java5demo.entities.User;
import com.fpoly.java5demo.jpas.OrderDetailJPA;
import com.fpoly.java5demo.jpas.SellerRequestJPA;
import com.fpoly.java5demo.services.CartDetailServices;
import com.fpoly.java5demo.services.CategoryServices;
import com.fpoly.java5demo.services.ProductServices;
import com.fpoly.java5demo.services.UserServices;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class HomeController {

	@Autowired
	private ProductServices productServices;

	@Autowired
	private CategoryServices categoryServices;

	@Autowired
	private CartDetailServices cartDetailServices;

	@Autowired
	private UserServices userServices;

	@Autowired
	private OrderDetailJPA orderDetailJPA;

	@Autowired
	private SellerRequestJPA sellerRequestJPA;

	@ModelAttribute("categories")
	public List<Category> getCategories() {
		return categoryServices.getList();
	}

	@ModelAttribute("currentUser")
	public User getCurrentUser(HttpServletRequest request, HttpSession session) {
		return userServices.getLoggedInUser(request, session);
	}

	@ModelAttribute("cartCount")
	public int getCartCount() {
		return cartDetailServices.getTotalQuantity();
	}

	@GetMapping({ "/", "/home" })
	public String home(Model model, @RequestParam(name = "cat", required = false) Integer catId,
			@RequestParam(name = "search", required = false) String search) {
		List<Product> products;

		if (search != null && !search.trim().isEmpty()) {
			products = productServices.search(search);
			model.addAttribute("searchKeyword", search);
		} else if (catId != null && catId > 0) {
			products = productServices.getByCategory(catId);
			model.addAttribute("selectedCategory", catId);
		} else {
			products = productServices.getAvailableProducts();
		}

		model.addAttribute("products", products);
		return "home";
	}

	@GetMapping("/product/{id}")
	public String productDetail(@PathVariable("id") int id,
			@RequestParam(value = "orderDetailId", required = false) Integer orderDetailId,
			Model model, RedirectAttributes redirectAttributes) {
		Product product = productServices.findById(id);
		if (product == null || !product.isStatus()) {
			if (orderDetailId != null) {
				redirectAttributes.addFlashAttribute("errorMessage",
						"Sản phẩm này đã ngừng kinh doanh hoặc bị xóa bởi người bán. Toàn bộ thông tin chi tiết đơn hàng của bạn vẫn được lưu giữ an toàn!");
				return "redirect:/user/orders";
			}
			return "redirect:/";
		}

		List<Product> relatedProducts = productServices.getByCategory(product.getCategory().getId());
		// Loại bỏ sản phẩm hiện tại khỏi danh sách gợi ý liên quan
		relatedProducts.removeIf(p -> p.getId() == id);

		if (orderDetailId != null) {
			OrderDetail detail = orderDetailJPA.findById(orderDetailId).orElse(null);
			if (detail != null) {
				model.addAttribute("fromOrderDetail", detail);
				if (detail.isProductModified()) {
					StringBuilder sb = new StringBuilder();
					if (detail.isPriceChanged()) {
						sb.append(String.format("Giá khi bạn đặt mua: %,d ₫ (Giá hiện tại của shop: %,d ₫). ", detail.getPrice(), product.getPrice()));
					}
					if (detail.isNameChanged()) {
						sb.append(String.format("Tên sản phẩm khi bạn đặt mua: '%s'. ", detail.getDisplayName()));
					}
					model.addAttribute("modifiedNotice", sb.toString().trim());
				}
			}
		}

		// Lấy thông tin shop người bán của sản phẩm
		if (product.getSeller() != null) {
			int sId = product.getSeller().getId();
			SellerRequest shopReq = sellerRequestJPA.findFirstByUserIdAndStatusOrderByIdDesc(sId, 1)
					.orElseGet(() -> sellerRequestJPA.findFirstByUserIdOrderByIdDesc(sId).orElse(null));
			model.addAttribute("shopInfo", shopReq);
			model.addAttribute("shopProductCount", productServices.getBySeller(sId).size());
		}

		model.addAttribute("product", product);
		model.addAttribute("relatedProducts", relatedProducts);
		return "product-detail";
	}

	@GetMapping("/shop/{id}")
	public String shopDetail(
			@PathVariable("id") int sellerId,
			@RequestParam(name = "cat", required = false) Integer catId,
			@RequestParam(name = "search", required = false) String search,
			Model model) {

		User seller = userServices.findById(sellerId).orElse(null);
		if (seller == null) {
			return "redirect:/";
		}

		SellerRequest shopReq = sellerRequestJPA.findFirstByUserIdAndStatusOrderByIdDesc(sellerId, 1)
				.orElseGet(() -> sellerRequestJPA.findFirstByUserIdOrderByIdDesc(sellerId).orElse(null));

		if (shopReq == null) {
			shopReq = new SellerRequest();
			shopReq.setUser(seller);
			shopReq.setShopName(seller.getName() != null ? seller.getName() : "Gian Hàng PolyShop");
			shopReq.setPhone(seller.getPhone() != null ? seller.getPhone() : "");
			shopReq.setAddress("Hà Nội, Việt Nam");
			shopReq.setDescription("Chào mừng bạn đến với gian hàng chính hãng tại PolyShop! Cam kết hàng chính hãng 100%, bảo hành đổi trả uy tín và hỗ trợ tư vấn 24/7.");
		}

		List<Product> allProducts = productServices.getBySeller(sellerId);
		List<Product> availableProducts = allProducts.stream()
				.filter(Product::isStatus)
				.toList();

		List<Product> filteredProducts = availableProducts;
		if (search != null && !search.trim().isEmpty()) {
			String kw = search.trim().toLowerCase();
			filteredProducts = filteredProducts.stream()
					.filter(p -> p.getName() != null && p.getName().toLowerCase().contains(kw))
					.toList();
			model.addAttribute("searchKeyword", search);
		} else if (catId != null && catId > 0) {
			filteredProducts = filteredProducts.stream()
					.filter(p -> p.getCategory() != null && p.getCategory().getId() == catId)
					.toList();
			model.addAttribute("selectedCategory", catId);
		}

		model.addAttribute("seller", seller);
		model.addAttribute("shop", shopReq);
		model.addAttribute("products", filteredProducts);
		model.addAttribute("totalProducts", availableProducts.size());
		return "shop";
	}

	@GetMapping("/category/{id}")
	public String productByCategory(@PathVariable("id") int id, Model model) {
		Category category = categoryServices.getById(id);
		if (category == null) {
			return "redirect:/";
		}
		List<Product> products = productServices.getByCategory(id);
		model.addAttribute("category", category);
		model.addAttribute("products", products);
		model.addAttribute("selectedCategory", id);
		return "home";
	}

	@GetMapping("/search")
	public String searchProducts(@RequestParam("keyword") String keyword, Model model) {
		List<Product> products = productServices.search(keyword);
		model.addAttribute("products", products);
		model.addAttribute("searchKeyword", keyword);
		return "home";
	}
}
