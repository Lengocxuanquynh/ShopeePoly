package com.fpoly.java5demo.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.fpoly.java5demo.entities.Category;
import com.fpoly.java5demo.entities.Product;
import com.fpoly.java5demo.entities.SellerRequest;
import com.fpoly.java5demo.entities.User;
import com.fpoly.java5demo.jpas.CategoryJPA;
import com.fpoly.java5demo.jpas.ProductJPA;
import com.fpoly.java5demo.jpas.SellerRequestJPA;
import com.fpoly.java5demo.jpas.UserJPA;

@Component
public class DatabaseDataInitializer implements CommandLineRunner {

	@Autowired
	private UserJPA userJPA;

	@Autowired
	private CategoryJPA categoryJPA;

	@Autowired
	private ProductJPA productJPA;

	@Autowired
	private SellerRequestJPA sellerRequestJPA;

	@Override
	public void run(String... args) throws Exception {
		// 1. Khởi tạo tài khoản mẫu
		if (userJPA.count() == 0) {
			// Super Admin
			User admin = new User();
			admin.setUsername("admin");
			admin.setPassword("123456");
			admin.setName("Super Admin PolyShop");
			admin.setEmail("admin@polyshop.vn");
			admin.setRole(1); // 1: Super Admin (Quản trị tối cao)
			admin.setStatus(true);
			userJPA.save(admin);

			// Seller 1: Poly Fashion
			User seller1 = new User();
			seller1.setUsername("seller");
			seller1.setPassword("123456");
			seller1.setName("Chủ Shop Poly Fashion");
			seller1.setEmail("seller@polyshop.vn");
			seller1.setPhone("0987654321");
			seller1.setRole(2); // 2: Người Bán Hàng (Seller)
			seller1.setStatus(true);
			seller1 = userJPA.save(seller1);

			SellerRequest sr1 = new SellerRequest();
			sr1.setUser(seller1);
			sr1.setShopName("Poly Fashion Official Store");
			sr1.setPhone("0987654321");
			sr1.setAddress("Tòa FPT Polytechnic, Phố Trịnh Văn Bô, Nam Từ Liêm, Hà Nội");
			sr1.setCategoryName("Thời Trang Nam & Nữ");
			sr1.setCreatedAt(new java.sql.Date(System.currentTimeMillis()));
			sr1.setStatus(1); // 1: Đã duyệt
			sr1.setDescription("Poly Fashion - Chuyên cung cấp thời trang nam nữ phong cách trẻ trung, chất lượng cao, chuẩn Shopee Mall. Cam kết 100% đúng hình ảnh, bảo hành đổi trả trong 7 ngày!");
			sellerRequestJPA.save(sr1);

			// Seller 2: PolyWatch Luxury (Đồng hồ)
			User seller2 = new User();
			seller2.setUsername("shopwatch");
			seller2.setPassword("123456");
			seller2.setName("Chủ Shop PolyWatch Luxury");
			seller2.setEmail("polywatch@polyshop.vn");
			seller2.setPhone("0912345678");
			seller2.setRole(2);
			seller2.setStatus(true);
			seller2 = userJPA.save(seller2);

			SellerRequest sr2 = new SellerRequest();
			sr2.setUser(seller2);
			sr2.setShopName("PolyWatch Luxury - Thế Giới Đồng Hồ");
			sr2.setPhone("0912345678");
			sr2.setAddress("Số 68 Đường Cầu Giấy, Q. Cầu Giấy, Hà Nội");
			sr2.setCategoryName("Đồng Hồ & Phụ Kiện");
			sr2.setCreatedAt(new java.sql.Date(System.currentTimeMillis()));
			sr2.setStatus(1);
			sr2.setDescription("PolyWatch Luxury cam kết phân phối đồng hồ chính hãng từ các thương hiệu hàng đầu: Casio, Apple Watch, Seiko, Citizen, Tissot. Bảo hành 5 năm, thay pin trọn đời!");
			sellerRequestJPA.save(sr2);

			// Seller 3: TechZone (Thiết bị công nghệ)
			User seller3 = new User();
			seller3.setUsername("shoptech");
			seller3.setPassword("123456");
			seller3.setName("Chủ Shop TechZone");
			seller3.setEmail("techzone@polyshop.vn");
			seller3.setPhone("0934567890");
			seller3.setRole(2);
			seller3.setStatus(true);
			seller3 = userJPA.save(seller3);

			SellerRequest sr3 = new SellerRequest();
			sr3.setUser(seller3);
			sr3.setShopName("TechZone Official Store");
			sr3.setPhone("0934567890");
			sr3.setAddress("Tầng 3 Vincom Center, Trần Duy Hưng, Hà Nội");
			sr3.setCategoryName("Thiết Bị Điện Tử & Laptop");
			sr3.setCreatedAt(new java.sql.Date(System.currentTimeMillis()));
			sr3.setStatus(1);
			sr3.setDescription("TechZone Official - Chuyên cung cấp điện thoại, máy tính laptop, phụ kiện chính hãng 100%. Bảo hành 12 tháng, đổi mới 30 ngày đầu!");
			sellerRequestJPA.save(sr3);

			// Buyers
			User user = new User();
			user.setUsername("user");
			user.setPassword("123456");
			user.setName("Nguyễn Văn A (Người Mua)");
			user.setEmail("user@polyshop.vn");
			user.setRole(0); // 0: Người Mua (Khách hàng)
			user.setStatus(true);
			userJPA.save(user);

			User user1 = new User();
			user1.setUsername("user1");
			user1.setPassword("123456");
			user1.setName("Trần Văn B");
			user1.setEmail("user1@polyshop.vn");
			user1.setRole(0);
			user1.setStatus(true);
			userJPA.save(user1);

			System.out.println(">>> Đã khởi tạo các tài khoản mẫu: admin, seller, shopwatch, shoptech, user, user1");
		}

		// 2. Khởi tạo danh mục sản phẩm
		if (categoryJPA.count() == 0) {
			Category cat1 = new Category();
			cat1.setName("Điện Thoại & Phụ Kiện");

			Category cat2 = new Category();
			cat2.setName("Máy Tính & Laptop");

			Category cat3 = new Category();
			cat3.setName("Thời Trang Nam");

			Category cat4 = new Category();
			cat4.setName("Thời Trang Nữ");

			Category cat5 = new Category();
			cat5.setName("Đồng Hồ & Thiết Bị Điện Tử");

			List<Category> savedCategories = categoryJPA.saveAll(Arrays.asList(cat1, cat2, cat3, cat4, cat5));
			System.out.println(">>> Đã khởi tạo 5 danh mục sản phẩm Shopee");

			// 3. Khởi tạo sản phẩm mẫu
			if (productJPA.count() == 0) {
				Category c1 = savedCategories.get(0); // Điện thoại
				Category c2 = savedCategories.get(1); // Laptop
				Category c3 = savedCategories.get(2); // Nam
				Category c4 = savedCategories.get(3); // Nữ
				Category c5 = savedCategories.get(4); // Đồng hồ & Điện tử

				User sellerFashion = userJPA.findByUsername("seller").orElse(null);
				User sellerWatch = userJPA.findByUsername("shopwatch").orElse(null);
				User sellerTech = userJPA.findByUsername("shoptech").orElse(null);

				List<Product> sampleProducts = new ArrayList<>();

				// Shop Tech Products
				sampleProducts.add(initProduct("Điện thoại Apple iPhone 15 Pro Max 256GB Titan Tự Nhiên", 29490000, 35, true, c1, sellerTech));
				sampleProducts.add(initProduct("Tai nghe không dây Bluetooth True Wireless chống ồn chủ động ANC", 350000, 150, true, c1, sellerTech));
				sampleProducts.add(initProduct("Củ sạc nhanh 20W Type-C chuẩn PD cho iPhone / iPad", 180000, 200, true, c1, sellerTech));
				sampleProducts.add(initProduct("Laptop Apple MacBook Air M2 13.6 inch 8GB RAM 256GB SSD", 23990000, 15, true, c2, sellerTech));
				sampleProducts.add(initProduct("Chuột máy tính không dây công thái học Silent Click", 220000, 80, true, c2, sellerTech));
				sampleProducts.add(initProduct("Bàn phím cơ không dây Bluetooth RGB Led 87 phím Hot-swap", 750000, 50, true, c2, sellerTech));
				sampleProducts.add(initProduct("Loa Bluetooth không dây mini âm bass siêu trầm chống nước IPX7", 290000, 120, true, c5, sellerTech));

				// Shop Watch Products (Clone variations of watches as requested)
				sampleProducts.add(initProduct("Đồng hồ thông minh Apple Watch Series 9 GPS 45mm viền nhôm Sport", 8990000, 40, true, c5, sellerWatch));
				sampleProducts.add(initProduct("Đồng hồ nam Casio Vintage điện tử dây kim loại mạ vàng cổ điển", 650000, 95, true, c5, sellerWatch));
				sampleProducts.add(initProduct("Đồng hồ nam cơ tự động Seiko 5 Automatic lộ máy kính Sapphire", 3850000, 30, true, c5, sellerWatch));
				sampleProducts.add(initProduct("Đồng hồ thông minh Smartwatch Pro theo dõi nhịp tim SpO2 chống nước", 790000, 60, true, c5, sellerWatch));
				sampleProducts.add(initProduct("Đồng hồ thể thao nam G-Shock chống va đập chịu nước 200M", 1950000, 45, true, c5, sellerWatch));
				sampleProducts.add(initProduct("Đồng hồ nữ Tissot Classic mặt số xà cừ đính đá sang trọng", 4200000, 20, true, c5, sellerWatch));
				sampleProducts.add(initProduct("Đồng hồ nam dây da thời trang Citizen Eco-Drive năng lượng ánh sáng", 2890000, 25, true, c5, sellerWatch));

				// Shop Fashion Products
				sampleProducts.add(initProduct("Áo thun nam ngắn tay cổ tròn cotton co giãn 4 chiều thoáng mát", 99000, 300, true, c3, sellerFashion));
				sampleProducts.add(initProduct("Quần short đùi nam thể thao vải dù gió chống thấm nước", 85000, 180, true, c3, sellerFashion));
				sampleProducts.add(initProduct("Áo khoác dù bomber 2 lớp phong cách trẻ trung Ulzzang", 219000, 95, true, c3, sellerFashion));
				sampleProducts.add(initProduct("Áo sơ mi nam công sở dài tay Oxford chống nhăn form Regular", 199000, 120, true, c3, sellerFashion));
				sampleProducts.add(initProduct("Quần jean nam ống suông dáng baggy rách gối cá tính", 259000, 85, true, c3, sellerFashion));
				sampleProducts.add(initProduct("Váy hoa nhí dáng xòe tiểu thư vintage mùa hè cổ vuông", 165000, 110, true, c4, sellerFashion));
				sampleProducts.add(initProduct("Set áo sơ mi croptop tay bồng kèm chân váy chữ A xếp ly", 240000, 75, true, c4, sellerFashion));
				sampleProducts.add(initProduct("Đầm dạ hội nữ thiết kế body cổ yếm tôn dáng quyến rũ", 320000, 50, true, c4, sellerFashion));

				productJPA.saveAll(sampleProducts);
				System.out.println(">>> Đã khởi tạo 22 sản phẩm mẫu phong phú phong cách Shopee cho 3 gian hàng");
			}
		}
	}

	private Product initProduct(String name, int price, int quantity, boolean status, Category category, User seller) {
		Product p = new Product();
		p.setName(name);
		p.setPrice(price);
		p.setQuantity(quantity);
		p.setStatus(status);
		p.setCategory(category);
		p.setSeller(seller);
		return p;
	}
}
