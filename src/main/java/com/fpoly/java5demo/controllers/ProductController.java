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

	@GetMapping("/products")
	public String listProducts() {
		return "redirect:/admin/product-form";
	}

	@GetMapping("/product-form")
	public String productForm(Model model) {
		model.addAttribute("bean", new ProductFormBean());
		model.addAttribute("products", productServices.getList());
		return "admin/product-form";
	}

	@GetMapping("/product-form/{id}")
	public String productEdit(@PathVariable("id") int id, Model model) {
		Product product = productServices.findById(id);
		if (product == null) {
			return "redirect:/admin/product-form";
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

		model.addAttribute("bean", bean);
		model.addAttribute("product", product);
		model.addAttribute("products", productServices.getList());
		return "admin/product-form";
	}

	@PostMapping("/product-form")
	public String productSave(@Valid @ModelAttribute("bean") ProductFormBean bean, Errors errors,
			Model model, RedirectAttributes redirectAttributes) {
		if (errors.hasErrors()) {
			model.addAttribute("products", productServices.getList());
			return "admin/product-form";
		}

		Category category = categoryServices.getById(bean.getCategory());
		if (category == null) {
			model.addAttribute("errorMessage", "Vui lòng chọn danh mục hợp lệ!");
			model.addAttribute("products", productServices.getList());
			return "admin/product-form";
		}

		Product product;
		boolean isUpdate = (bean.getId() != null && bean.getId() > 0);

		if (isUpdate) {
			product = productServices.findById(bean.getId());
			if (product == null) {
				return "redirect:/admin/product-form";
			}
		} else {
			product = new Product();
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
			model.addAttribute("products", productServices.getList());
			return "admin/product-form";
		}

		redirectAttributes.addFlashAttribute("successMessage", isUpdate ? "Cập nhật sản phẩm thành công!" : "Thêm mới sản phẩm thành công!");
		return "redirect:/admin/product-form";
	}

	@PostMapping("/product/delete/{id}")
	public String productDelete(@PathVariable("id") int id, RedirectAttributes redirectAttributes) {
		String error = productServices.remove(id);
		if (error == null) {
			redirectAttributes.addFlashAttribute("successMessage", "Xóa sản phẩm thành công!");
		} else {
			redirectAttributes.addFlashAttribute("errorMessage", error);
		}
		return "redirect:/admin/product-form";
	}

	@ModelAttribute("categories")
	public List<Category> getCategories() {
		return categoryServices.getList();
	}
}
