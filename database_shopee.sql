-- ==========================================================
-- SCRIPT KHỞI TẠO CƠ SỞ DỮ LIỆU SHOPEE CLONE (JAVA 5)
-- HỆ QUẢN TRỊ CSDL: MICROSOFT SQL SERVER
-- ==========================================================

-- 1. TẠO DATABASE (NẾU CHƯA CÓ)
IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = 'java5')
BEGIN
    CREATE DATABASE java5;
END
GO

USE java5;
GO

-- 2. XÓA CÁC BẢNG CŨ NẾU ĐÃ TỒN TẠI ĐỂ RESET DỮ LIỆU
IF OBJECT_ID('seller_requests', 'U') IS NOT NULL DROP TABLE seller_requests;
IF OBJECT_ID('order_details', 'U') IS NOT NULL DROP TABLE order_details;
IF OBJECT_ID('cart_details', 'U') IS NOT NULL DROP TABLE cart_details;
IF OBJECT_ID('images', 'U') IS NOT NULL DROP TABLE images;
IF OBJECT_ID('orders', 'U') IS NOT NULL DROP TABLE orders;
IF OBJECT_ID('products', 'U') IS NOT NULL DROP TABLE products;
IF OBJECT_ID('categories', 'U') IS NOT NULL DROP TABLE categories;
IF OBJECT_ID('users', 'U') IS NOT NULL DROP TABLE users;
GO

-- 3. TẠO BẢNG USERS (NGƯỜI DÙNG)
CREATE TABLE users (
    id INT IDENTITY(1,1) PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    name NVARCHAR(200) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NULL,
    status BIT NOT NULL DEFAULT 1,
    role INT NOT NULL DEFAULT 0 -- 0: User / Khách hàng, 1: Admin / Quản trị viên, 2: Seller
);
GO

-- 4. TẠO BẢNG CATEGORIES (DANH MỤC SẢN PHẨM)
CREATE TABLE categories (
    id INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(100) NOT NULL
);
GO

-- 5. TẠO BẢNG PRODUCTS (SẢN PHẨM)
CREATE TABLE products (
    id INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(200) NOT NULL,
    price INT NOT NULL,
    quantity INT NOT NULL DEFAULT 0,
    status BIT NOT NULL DEFAULT 1,
    cat_id INT NOT NULL,
    seller_id INT NULL,
    CONSTRAINT FK_Products_Categories FOREIGN KEY (cat_id) REFERENCES categories(id) ON DELETE CASCADE,
    CONSTRAINT FK_Products_Users FOREIGN KEY (seller_id) REFERENCES users(id)
);
GO

-- 6. TẠO BẢNG IMAGES (HÌNH ẢNH SẢN PHẨM)
CREATE TABLE images (
    id INT IDENTITY(1,1) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    prod_id INT NOT NULL,
    CONSTRAINT FK_Images_Products FOREIGN KEY (prod_id) REFERENCES products(id) ON DELETE CASCADE
);
GO

-- 7. TẠO BẢNG CART_DETAILS (CHI TIẾT GIỎ HÀNG)
CREATE TABLE cart_details (
    id INT IDENTITY(1,1) PRIMARY KEY,
    quantity INT NOT NULL DEFAULT 1,
    user_id INT NULL,
    prod_id INT NULL,
    CONSTRAINT FK_Cart_Users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT FK_Cart_Products FOREIGN KEY (prod_id) REFERENCES products(id) ON DELETE CASCADE
);
GO

-- 8. TẠO BẢNG ORDERS (ĐƠN HÀNG)
CREATE TABLE orders (
    id INT IDENTITY(1,1) PRIMARY KEY,
    address NVARCHAR(500) NOT NULL,
    payment_method NVARCHAR(50) NOT NULL DEFAULT 'COD',
    total_price INT NOT NULL DEFAULT 0,
    date DATE NOT NULL,
    status INT NOT NULL DEFAULT 0, -- 0: Chờ xác nhận, 1: Đã xác nhận, 2: Đang giao, 3: Đã giao, 4: Hoàn thành, 5: Hủy
    user_id INT NOT NULL,
    CONSTRAINT FK_Orders_Users FOREIGN KEY (user_id) REFERENCES users(id)
);
GO

-- 9. TẠO BẢNG ORDER_DETAILS (CHI TIẾT ĐƠN HÀNG - LƯU SNAPSHOT GIÁ & THÔNG TIN SẢN PHẨM)
CREATE TABLE order_details (
    id INT IDENTITY(1,1) PRIMARY KEY,
    price INT NOT NULL,
    quantity INT NOT NULL,
    product_name NVARCHAR(200) NULL,
    product_image VARCHAR(255) NULL,
    order_id INT NOT NULL,
    prod_id INT NULL,
    CONSTRAINT FK_OrderDetails_Orders FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT FK_OrderDetails_Products FOREIGN KEY (prod_id) REFERENCES products(id) ON DELETE SET NULL
);
GO

-- 10. TẠO BẢNG SELLER_REQUESTS (HỒ SƠ ĐĂNG KÝ BÁN HÀNG)
CREATE TABLE seller_requests (
    id INT IDENTITY(1,1) PRIMARY KEY,
    shop_name NVARCHAR(200) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    address NVARCHAR(500) NOT NULL,
    category_name NVARCHAR(100) NULL,
    created_at DATE NOT NULL,
    status INT NOT NULL DEFAULT 0, -- 0: Chờ duyệt, 1: Đã duyệt, 2: Từ chối
    note NVARCHAR(500) NULL,
    description NVARCHAR(1000) NULL,
    avatar VARCHAR(255) NULL,
    user_id INT NOT NULL,
    CONSTRAINT FK_SellerRequests_Users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
GO

-- ==========================================================
-- CHÈN DỮ LIỆU MẪU (SEED DATA)
-- ==========================================================

-- A. Users
INSERT INTO users (username, password, name, email, phone, status, role) VALUES 
('admin', '123456', N'Quản Trị Viên Shopee', 'admin@shopee.vn', '0901234567', 1, 1),
('seller', '123456', N'Chủ Shop Poly Fashion', 'seller@shopee.vn', '0987654321', 1, 2),
('user1', '123456', N'Nguyễn Văn A', 'nguyenvana@gmail.com', '0912345678', 1, 0),
('user2', '123456', N'Trần Thị Bích', 'bichthitran@gmail.com', '0934567890', 1, 0);
GO

-- B. Categories
INSERT INTO categories (name) VALUES 
(N'Điện Thoại & Phụ Kiện'),
(N'Máy Tính & Laptop'),
(N'Thời Trang Nam'),
(N'Thời Trang Nữ'),
(N'Thiết Bị Điện Tử');
GO

-- C. Products
INSERT INTO products (name, price, quantity, status, cat_id, seller_id) VALUES 
(N'Điện thoại Apple iPhone 15 Pro Max 256GB Titan Tự Nhiên', 29490000, 30, 1, 1, NULL),
(N'Tai nghe không dây Bluetooth True Wireless chống ồn chủ động', 350000, 150, 1, 1, NULL),
(N'Củ sạc nhanh 20W Type-C chuẩn PD cho iPhone / iPad', 180000, 200, 1, 1, NULL),
(N'Laptop Apple MacBook Air M2 13.6 inch 8GB RAM 256GB SSD', 23990000, 15, 1, 2, NULL),
(N'Chuột máy tính không dây công thái học Silent Click', 220000, 80, 1, 2, NULL),
(N'Bàn phím cơ không dây Bluetooth RGB Led 87 phím Hot-swap', 750000, 50, 1, 2, NULL),
(N'Áo thun nam ngắn tay cổ tròn cotton co giãn 4 chiều thoáng mát', 99000, 300, 1, 3, 2),
(N'Quần short đùi nam thể thao vải dù gió chống thấm nước', 85000, 180, 1, 3, 2),
(N'Áo khoác dù bomber 2 lớp phong cách trẻ trung Ulzzang', 219000, 95, 1, 3, 2),
(N'Váy hoa nhí dáng xòe tiểu thư vintage mùa hè cổ vuông', 165000, 110, 1, 4, 2),
(N'Set áo sơ mi croptop tay bồng kèm chân váy chữ A xếp ly', 240000, 75, 1, 4, 2),
(N'Loa Bluetooth không dây mini âm bass siêu trầm chống nước', 290000, 120, 1, 5, NULL),
(N'Đồng hồ thông minh Smartwatch theo dõi nhịp tim chống nước IP68', 680000, 60, 1, 5, NULL);
GO

-- D. Đơn hàng mẫu
INSERT INTO orders (address, payment_method, total_price, date, status, user_id) VALUES 
(N'Tòa FPT Polytechnic, Phố Trịnh Văn Bô, Nam Từ Liêm, Hà Nội (Người nhận: Nguyễn Văn A - SĐT: 0987654321)', 'COD', 530000, '2026-09-25', 1, 3),
(N'123 Đường Nguyễn Huệ, Quận 1, TP. Hồ Chí Minh (Người nhận: Trần Thị Bích - SĐT: 0912345678)', 'PolyPay', 350000, '2026-09-26', 0, 4);
GO

-- E. Chi tiết đơn hàng mẫu (Lưu snapshot tên sản phẩm)
INSERT INTO order_details (price, quantity, product_name, product_image, order_id, prod_id) VALUES 
(350000, 1, N'Tai nghe không dây Bluetooth True Wireless chống ồn chủ động', NULL, 1, 2),
(180000, 1, N'Củ sạc nhanh 20W Type-C chuẩn PD cho iPhone / iPad', NULL, 1, 3),
(350000, 1, N'Tai nghe không dây Bluetooth True Wireless chống ồn chủ động', NULL, 2, 2);
GO

-- F. Hồ sơ gian hàng mẫu (Đã duyệt)
INSERT INTO seller_requests (shop_name, phone, address, category_name, created_at, status, description, avatar, user_id) VALUES
(N'Poly Fashion Official Store', '0987654321', N'Tòa FPT Polytechnic, Phố Trịnh Văn Bô, Nam Từ Liêm, Hà Nội', N'Thời Trang Nam', '2026-09-01', 1, N'Poly Fashion - Chuyên cung cấp thời trang nam nữ phong cách trẻ trung, chất lượng cao, chuẩn Shopee Mall. Cam kết 100% đúng hình ảnh, bảo hành đổi trả trong 7 ngày!', NULL, 2);
GO

PRINT N'Khởi tạo cơ sở dữ liệu Shopee Clone (Java 5) thành công!';