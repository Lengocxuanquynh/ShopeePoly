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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fpoly.java5demo.beans.ProductFormBean;
import com.fpoly.java5demo.entities.Category;
import com.fpoly.java5demo.entities.Product;
import com.fpoly.java5demo.jpas.SellerRequestJPA;
import com.fpoly.java5demo.services.CategoryServices;
import com.fpoly.java5demo.services.ProductServices;
import com.fpoly.java5demo.services.UserServices;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin")
public class ProductController {

	@Autowired
	private ProductServices productServices;

	@Autowired
	private CategoryServices categoryServices;

	@Autowired
	private UserServices userServices;

	@Autowired
	private SellerRequestJPA sellerRequestJPA;

	@GetMapping({ "", "/dashboard" })
	public String dashboard(Model model) {
		model.addAttribute("productCount", productServices.getList().size());
		model.addAttribute("categoryCount", categoryServices.getList().size());
		model.addAttribute("userCount", userServices.getAllUsers().size());
		model.addAttribute("pendingSellerCount", sellerRequestJPA.countByStatus(0));
		model.addAttribute("products", productServices.getList());
		return "admin/dashboard";
	}

	@GetMapping({ "/products", "/product-form" })
	public String listProducts(Model model) {
		List<Product> products = productServices.getList();
		long activeCount = products.stream().filter(Product::isStatus).count();
		long inactiveCount = products.size() - activeCount;

		model.addAttribute("products", products);
		model.addAttribute("activeCount", activeCount);
		model.addAttribute("inactiveCount", inactiveCount);
		return "admin/products";
	}

	@GetMapping("/product-form/{id}")
	public String productEditNotice(@PathVariable("id") int id, RedirectAttributes redirectAttributes) {
		redirectAttributes.addFlashAttribute("errorMessage",
				"Sản phẩm do Người bán (Shop) tự quản lý thông tin và giá bán. SuperAdmin chỉ có quyền kiểm duyệt hoặc khóa/mở khóa hiển thị!");
		return "redirect:/admin/products";
	}

	@PostMapping("/product-form")
	public String productSaveNotice(RedirectAttributes redirectAttributes) {
		redirectAttributes.addFlashAttribute("errorMessage",
				"SuperAdmin không trực tiếp thêm hoặc sửa sản phẩm của Shop. Người bán sẽ đăng bán sản phẩm qua Kênh Người Bán (/seller)!");
		return "redirect:/admin/products";
	}

	@PostMapping("/product/toggle-status/{id}")
	public String toggleProductStatus(@PathVariable("id") int id, RedirectAttributes redirectAttributes) {
		Product product = productServices.findById(id);
		if (product == null) {
			redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy sản phẩm!");
			return "redirect:/admin/products";
		}

		boolean willLock = product.isStatus();
		boolean success = productServices.toggleStatus(id);
		if (success) {
			if (willLock) {
				redirectAttributes.addFlashAttribute("successMessage",
						"Đã khóa/ẩn sản phẩm vi phạm: '" + product.getName() + "'!");
			} else {
				redirectAttributes.addFlashAttribute("successMessage",
						"Đã mở khóa hiển thị cho sản phẩm: '" + product.getName() + "'!");
			}
		} else {
			redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi cập nhật trạng thái kiểm duyệt sản phẩm!");
		}
		return "redirect:/admin/products";
	}

	@PostMapping("/product/delete/{id}")
	public String productDelete(@PathVariable("id") int id, RedirectAttributes redirectAttributes) {
		Product product = productServices.findById(id);
		String prodName = (product != null) ? product.getName() : ("#" + id);
		String error = productServices.remove(id);
		if (error == null) {
			redirectAttributes.addFlashAttribute("successMessage", "Đã xóa vĩnh viễn sản phẩm vi phạm: '" + prodName + "'!");
		} else {
			redirectAttributes.addFlashAttribute("errorMessage", error);
		}
		return "redirect:/admin/products";
	}

	@ModelAttribute("categories")
	public List<Category> getCategories() {
		return categoryServices.getList();
	}
}
