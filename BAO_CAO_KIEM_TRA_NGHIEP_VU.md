# BÁO CÁO ĐÁNH GIÁ & KIỂM TRA NGHIỆP VỤ DỰ ÁN SHOPEE CLONE (JAVA 5)

**Dự án**: Website Thương mại Điện tử Shopee Clone  
**Nền tảng công nghệ**: Spring Boot 3.x, Spring Data JPA, Thymeleaf, H2 / MS SQL Server, Bootstrap 5  
**Phạm vi môn học**: Java 5 - Lập trình Web với Spring Boot (FPT Polytechnic)  
**Thời gian rà soát**: Tháng 10/2026  

---

## MỤC LỤC
1. [Tổng Quan Đánh Giá](#1-tổng-quan-đánh-giá)
2. [Bảng Ma Trận Đánh Giá Nghiệp Vụ](#2-bảng-ma-trận-đánh-giá-nghiệp-vụ)
3. [Đánh Giá Chuyên Sâu: Nghiệp Vụ Mua Hàng Cơ Bản (B2C) - 8.5/10](#3-đánh-giá-chuyên-sâu-nghiệp-vụ-mua-hàng-cơ-bản-b2c---8510)
   - [3.1. Vì sao luồng Mua hàng đạt điểm cao (8.5/10)?](#31-vì-sao-luồng-mua-hàng-đạt-điểm-cao-8510)
   - [3.2. Chi tiết 4 hạt sạn bị trừ 1.5 điểm](#32-chi-tiết-4-hạt-sạn-bị-trừ-15-điểm)
   - [3.3. Hướng dẫn nâng cấp từ 8.5/10 lên 10/10 tuyệt đối](#33-hướng-dẫn-nâng-cấp-từ-8510-lên-1010-tuyệt-đối)
4. [Phân Tích Chi Tiết Các Phân Hệ Khác](#4-phân-tích-chi-tiết-các-phân-hệ-khác)
   - [4.1. Phân hệ Xác thực & Phân quyền (Auth & Security)](#41-phân-hệ-xác-thực--phân-quyền-auth--security)
   - [4.2. Phân hệ Danh mục (Category Management)](#42-phân-hệ-danh-mục-category-management)
   - [4.3. Phân hệ Sản phẩm & Quản lý Tồn kho (Products & Stock)](#43-phân-hệ-sản-phẩm--quản-lý-tồn-kho-products--stock)
   - [4.4. Phân hệ Giỏ hàng & Đồng bộ Phiên (Cart & Session Sync)](#44-phân-hệ-giỏ-hàng--đồng-bộ-phiên-cart--session-sync)
   - [4.5. Phân hệ Vòng đời Đơn hàng & Quản lý Đơn (Order Lifecycle)](#45-phân-hệ-vòng-đời-đơn-hàng--quản-lý-đơn-order-lifecycle)
   - [4.6. Phân hệ Quản lý Người dùng & Hồ sơ cá nhân (User Profile & Admin)](#46-phân-hệ-quản-lý-người-dùng--hồ-sơ-cá-nhân-user-profile--admin)
   - [4.7. Phân hệ Kênh Người Bán (Seller & Multi-vendor Architecture)](#47-phân-hệ-kênh-người-bán-seller--multi-vendor-architecture)
5. [Tổng Hợp Các Lỗi Nghiệp Vụ & Mã Nguồn Cần Sửa](#5-tổng-hợp-các-lỗi-nghiệp-vụ--mã-nguồn-cần-sửa)
6. [Khuyến Nghị Cho Buổi Báo Cáo / Bảo Vệ Môn Học](#6-khuyến-nghị-cho-buổi-báo-cáo--bảo-vệ-môn-học)

---

## 1. TỔNG QUAN ĐÁNH GIÁ

Dự án được xây dựng theo kiến trúc MVC phân tầng rõ ràng (`beans`, `entities`, `jpas`, `services`, `controllers`, `components`, `config`). Toàn bộ mã nguồn biên dịch thành công (`BUILD SUCCESS`), mã nguồn sạch sẽ, sử dụng Spring Data JPA và Thymeleaf theo đúng chuẩn chương trình đào tạo Java 5.

### Bảng Điểm Tổng Hợp:
| Hạng mục | Điểm số | Nhận xét |
| :--- | :---: | :--- |
| **Cấu trúc chuẩn Java 5 (FPT Poly)** | **10/10** | Chuẩn MVC, Spring Boot, Spring Data JPA, Thymeleaf, Interceptor, Validation. |
| **Nghiệp vụ Mua hàng cơ bản (B2C)** | **8.5/10** | Luồng xem sản phẩm, thêm giỏ, đặt hàng trừ kho, hủy đơn hoàn kho chạy trơn tru, logic chặt chẽ. |
| **Nghiệp vụ Phân quyền & Quản lý Phiên** | **6.0/10** | Có Interceptor nhưng Cookie Remember Me và hàm Logout đang bị lỗi logic. |
| **Nghiệp vụ Kênh Người Bán (Seller)** | **4.0/10** | Bảng `products` thiếu trường `seller_id`, dẫn đến Seller can thiệp chéo sản phẩm và đơn của nhau. |

---

## 2. BẢNG MA TRẬN ĐÁNH GIÁ NGHIỆP VỤ

| Phân hệ | Chức năng nghiệp vụ | Hiện trạng | Kết luận |
| :--- | :--- | :---: | :---: |
| **Xác thực** | Đăng ký tài khoản (kiểm tra trùng username, email) | Hoạt động tốt | ✅ Đạt |
| **Xác thực** | Đăng nhập hệ thống & phân quyền Role | Hoạt động tốt | ✅ Đạt |
| **Xác thực** | Đăng xuất hệ thống (xóa Session & Cookie) | Lỗi `setMaxAge(-1)` | ❌ Lỗi nặng |
| **Xác thực** | Xử lý tài khoản bị khóa trong phiên đang hoạt động | Vẫn cho phép thao tác | ⚠️ Cần sửa |
| **Danh mục** | CRUD danh mục, hiển thị cây danh mục | Hoạt động tốt | ✅ Đạt |
| **Danh mục** | Chặn xóa danh mục khi còn chứa sản phẩm | Hoạt động tốt | ✅ Đạt |
| **Danh mục** | Kiểm tra trùng tên danh mục trong SQL | Query case-sensitivity | ⚠️ Cần tối ưu |
| **Sản phẩm** | Xem danh sách, tìm kiếm từ khóa, lọc danh mục | Hoạt động tốt | ✅ Đạt |
| **Sản phẩm** | Thêm mới, chỉnh sửa, upload nhiều ảnh | Hoạt động tốt | ✅ Đạt |
| **Sản phẩm** | Xóa sản phẩm (Fallback ẩn khi đã có đơn hàng) | Hoạt động tốt | ✅ Đạt |
| **Giỏ hàng** | Thêm giỏ, cập nhật số lượng, xóa món, tính tiền | Hoạt động tốt | ✅ Đạt |
| **Giỏ hàng** | Khôi phục giỏ hàng từ DB khi đăng nhập | Chưa gọi nạp giỏ DB | ❌ Lỗi logic |
| **Giỏ hàng** | Xóa giỏ hàng DB sau khi đặt hàng thành công | Chỉ xóa session | ⚠️ Cần bổ sung |
| **Đặt hàng** | Form checkout, validate SĐT & địa chỉ | Hoạt động tốt | ✅ Đạt |
| **Đặt hàng** | Kiểm tra số lượng tồn kho trước khi tạo đơn | Rất chuẩn xác | ✅ Đạt |
| **Đặt hàng** | Trừ số lượng tồn kho trong kho tự động | Rất chuẩn xác | ✅ Đạt |
| **Đơn hàng** | Khách xem lịch sử đơn & chi tiết đơn | Chống IDOR tốt | ✅ Đạt |
| **Đơn hàng** | Khách hủy đơn Chờ xác nhận, hoàn trả kho | Rất chuẩn xác | ✅ Đạt |
| **Đơn hàng** | Admin đổi trạng thái đơn hàng | Chưa chặn chuyển trạng thái phi lý | ⚠️ Cần bổ sung |
| **Tài khoản** | Đổi mật khẩu, cập nhật thông tin cá nhân | Hoạt động tốt | ✅ Đạt |
| **Tài khoản** | Admin khóa / xóa tài khoản (bảo toàn đơn hàng) | Rất chuẩn xác | ✅ Đạt |
| **Kênh Người Bán** | Đăng ký mở shop, đăng bán, quản lý đơn riêng | Thiếu `seller_id` trong CSDL | ❌ Sai kiến trúc |

---

## 3. ĐÁNH GIÁ CHUYÊN SÂU: NGHIỆP VỤ MUA HÀNG CƠ BẢN (B2C) - 8.5/10

Chu trình mua sắm người dùng từ A-Z trong hệ thống:
```text
Trang chủ / Danh mục ──> Chi tiết Sản phẩm ──> Thêm Giỏ / Mua ngay ──> Giỏ hàng
       │
       └──> Thanh toán (Checkout) ──> Đặt hàng thành công ──> Quản lý đơn & Hủy đơn
```

### 3.1. Vì sao luồng Mua hàng đạt điểm cao (8.5/10)?

Hệ thống xử lý xuất sắc các nghiệp vụ then chốt của một website thương mại điện tử:

1. **Kiểm tra tồn kho 2 lần độc lập**:
   - **Lần 1 (Tại giỏ hàng - `CartDetailServices.java:L71`)**: Chặn ngay khi thêm giỏ, không cho số lượng mua vượt quá số tồn kho hiện có (`if (product.getQuantity() < newQty) newQty = product.getQuantity()`).
   - **Lần 2 (Tại thời điểm bấm Đặt hàng - `OrderServices.java:L53-L56`)**: Kiểm tra lại toàn bộ giỏ hàng với CSDL một lần nữa trước khi ghi nhận đơn. Nếu phát hiện hết hàng hoặc không đủ tồn kho, hệ thống từ chối tạo đơn ngay lập tức.
2. **Tính toàn vẹn dữ liệu bằng `@Transactional` (`OrderServices.java:L41`)**:
   - Việc tạo đơn hàng (`orderJPA.save`), lưu chi tiết đơn hàng (`orderDetailJPA.save`) và trừ số lượng kho (`productJPA.save`) nằm trọn trong một Transaction. Nếu một bước bị lỗi, toàn bộ thao tác được rollback để không bị trừ kho ảo.
3. **Bảo mật giá tiền**:
   - Hệ thống không tin giá tiền gửi từ trình duyệt mà luôn truy vấn lại giá gốc từ bảng `products` trong CSDL để tính tổng tiền thanh toán (`item.getPrice()`).
4. **Hủy đơn hàng hoàn kho & Chống IDOR (`OrderServices.java:L140-L163`)**:
   - Khách chỉ hủy được đơn của chính mình (`order.getUser().getId() == userId`).
   - Chỉ cho hủy khi đơn ở trạng thái `0` (*Chờ xác nhận*).
   - Tự động hoàn trả số lượng sản phẩm vào kho khi hủy thành công.

---

### 3.2. Chi tiết 4 hạt sạn bị trừ 1.5 điểm

Mặc dù luồng chạy thông suốt, nhưng khi soi kỹ logic nghiệp vụ thực tế, có **4 điểm chưa hoàn hảo**:

#### 🔴 Hạt sạn 1: Phương thức thanh toán (Payment Method) bị bỏ quên hoàn toàn (-0.5 điểm)
- **Thực tế trên giao diện**: Trang `checkout.html:L125-L145` cho khách hàng chọn 3 hình thức:
  - Thanh toán khi nhận hàng (COD)
  - Ví điện tử PolyPay
  - Chuyển khoản ngân hàng (Bank Transfer)
- **Thực tế trong CSDL & Mã nguồn**:
  - Entity `Order.java` và bảng `orders` **hoàn toàn không có cột lưu phương thức thanh toán**.
  - Trong `OrderServices.java:L60-L79`, giá trị `checkoutBean.getPaymentMethod()` **bị vứt bỏ**, không hề được lưu lại. Dù khách chọn phương thức nào thì hệ thống cũng không ghi nhận vào đơn hàng.

#### 🟡 Hạt sạn 2: Rủi ro mất Số điện thoại/Địa chỉ do cắt chuỗi `substring(200)` (-0.3 điểm)
- **Vị trí**: `OrderServices.java:L62-L68`
- Bảng `orders` chỉ có 1 cột duy nhất là `address NVARCHAR(200)`, nên lập trình viên đã gộp chung tất cả thông tin vào một chuỗi:
  ```java
  String fullAddress = checkoutBean.getAddress() 
      + " (Người nhận: " + checkoutBean.getFullName() 
      + " - SĐT: " + checkoutBean.getPhone() + ")";
  if (checkoutBean.getNote() != null) {
      fullAddress += " [Ghi chú: " + checkoutBean.getNote().trim() + "]";
  }
  if (fullAddress.length() > 200) {
      fullAddress = fullAddress.substring(0, 200); // <-- Cắt ngang
  }
  ```
- **Hệ quả**: Nếu người dùng nhập địa chỉ chi tiết dài (thôn xóm, ngõ ngách, số nhà...), đoạn chuỗi phía sau chứa **Số điện thoại** hoặc **Ghi chú giao hàng** sẽ bị cắt đứt mất.

#### 🟡 Hạt sạn 3: Giỏ hàng trong CSDL chưa được dọn dẹp sau khi Đặt hàng (-0.4 điểm)
- **Vị trí**: `OrderServices.java:L97`
- Sau khi đặt hàng thành công, hệ thống chỉ gọi `cartDetailServices.clearCart()` (xóa giỏ hàng trong `HttpSession`).
- Các bản ghi tương ứng trong bảng `cart_details` của Database của user đó **vẫn còn nguyên**. Khi người dùng đăng nhập ở một phiên khác hoặc khi session listener đồng bộ lại, các món hàng cũ đã mua có thể xuất hiện lại trong giỏ.

#### 🟡 Hạt sạn 4: Trải nghiệm người dùng khi sản phẩm hết hàng (`quantity = 0`) (-0.3 điểm)
- **Vị trí**: `product-detail.html:L108-L125`
- Khi sản phẩm có số lượng tồn kho bằng `0`:
  - Nút **"Thêm Vào Giỏ Hàng"** và **"Mua Ngay"** không bị khóa (`disabled`).
  - Người dùng bấm "Thêm vào giỏ" thì hệ thống tự động loại bỏ món hàng nhưng **không có thông báo lỗi** gì cho khách.
  - Người dùng bấm "Mua ngay" thì bị chuyển hướng sang giỏ hàng trống mà không rõ nguyên nhân.

---

### 3.3. Hướng dẫn nâng cấp từ 8.5/10 lên 10/10 tuyệt đối

Để hoàn thiện luồng mua hàng đạt điểm tuyệt đối:

1. **Dọn sạch giỏ hàng trong CSDL sau khi đặt hàng** (`OrderServices.java` dòng 97):
   ```java
   cartDetailServices.clearCart();
   try {
       cartDetailJPA.deleteByUserId(user.getId());
   } catch (Exception ignored) {}
   ```

2. **Khóa nút Mua khi sản phẩm hết hàng** (`product-detail.html` dòng 118-124):
   ```html
   <button type="submit" name="buyNow" value="false" 
           th:disabled="${product.quantity <= 0}" 
           class="btn btn-polyshop-outline px-4 py-2">
     <span th:text="${product.quantity > 0 ? 'Thêm Vào Giỏ Hàng' : 'Hết Hàng'}"></span>
   </button>
   <button type="submit" name="buyNow" value="true" 
           th:disabled="${product.quantity <= 0}" 
           class="btn btn-polyshop-orange px-5 py-2 fw-bold">
     Mua Ngay
   </button>
   ```

---

## 4. PHÂN TÍCH CHI TIẾT CÁC PHÂN HỆ KHÁC

### 4.1. Phân hệ Xác thực & Phân quyền (Auth & Security)
- **Lỗi không đăng xuất được khi có Cookie Remember Me (`Utils.java:L74-L84`)**:
  - `cookie.setMaxAge(-1)` chỉ giữ cookie thành session cookie chứ không xóa.
  - Khi người dùng bấm `/logout`, cookie `user_id` vẫn còn trên trình duyệt. Khi người dùng quay lại trang chủ, `UserServices.java:L98-L111` đọc thấy cookie và tự động đăng nhập lại.
- **Lỗ hổng mạo danh Admin qua Cookie**: Cookie lưu trực tiếp `user_id=1` dạng plaintext. Người dùng có thể sửa cookie F12 để chiếm quyền Admin.
- **Bỏ qua tài khoản bị khóa trong phiên (`UserServices.java:L86-L96`)**: Nếu tài khoản bị Admin khóa (`status = false`), hàm `getLoggedInUser()` fallback trả về `sessionUser` (vẫn khác `null`), khiến khách hàng bị khóa vẫn tiếp tục thao tác được.

### 4.2. Phân hệ Danh mục (Category Management)
- Kiểm tra toàn vẹn dữ liệu tốt: Chặn xóa danh mục khi còn sản phẩm liên kết (`CategoryServices.java:L74-L89`).
- Câu truy vấn `WHERE LOWER(name)=?1` trong `CategoryJPA.java` chưa lowercase tham số `?1`.

### 4.3. Phân hệ Sản phẩm & Quản lý Tồn kho (Products & Stock)
- Đầy đủ chức năng CRUD: Thêm mới, chỉnh sửa, upload nhiều ảnh.
- Có kiểm tra giá tối thiểu (`price >= 1000`) và số lượng không âm (`quantity >= 0`).
- Khi xóa sản phẩm đã có đơn hàng cũ, tự động chuyển về ẩn sản phẩm (`status = false`) để bảo toàn dữ liệu.

### 4.4. Phân hệ Giỏ hàng & Đồng bộ Phiên (Cart & Session Sync)
- Sử dụng Session JSON Map cho phép khách vãng lai mua hàng.
- **Lỗi logic**: Chưa nạp lại giỏ hàng từ Database vào Session khi Đăng nhập (`UserServices.java:login`). Khách đăng nhập trên thiết bị mới sẽ thấy giỏ hàng trống trơn dù CSDL có lưu.

### 4.5. Phân hệ Vòng đời Đơn hàng & Quản lý Đơn (Order Lifecycle)
- Admin có thể đổi trạng thái đơn hàng tự do, không theo luồng State Machine (ví dụ: đơn Đã giao thành công vẫn cho chuyển về Hủy -> gây cộng kho ảo lần 2).
- Nếu đơn đã hủy (5) được đổi ngược về trạng thái hoạt động (0), kho hàng không được trừ lại.

### 4.6. Phân hệ Quản lý Người dùng & Hồ sơ cá nhân (User Profile & Admin)
- Đổi mật khẩu có kiểm tra mật khẩu cũ, khớp mật khẩu mới, tối thiểu 6 ký tự.
- Cập nhật thông tin cá nhân có kiểm tra tránh trùng email.
- Admin không thể tự xóa tài khoản của chính mình.
- Xóa tài khoản có đơn hàng tự động chuyển thành Khóa tài khoản (`status = false`).

### 4.7. Phân hệ Kênh Người Bán (Seller & Multi-vendor Architecture)
- Bảng `products` và `orders` không có cột liên kết đến `User/Seller`.
- Seller khi đăng nhập nhìn thấy toàn bộ sản phẩm và đơn hàng của cả sàn, có thể sửa/xóa sản phẩm của Admin và của Seller khác.

---

## 5. TỔNG HỢP CÁC LỖI NGHIỆP VỤ & MÃ NGUỒN CẦN SỬA

### 1. Sửa hàm xóa Cookie trong `Utils.java`
**Vị trí**: `src/main/java/com/fpoly/java5demo/utils/Utils.java`

```java
// CODE CŨ (LỖI: không xóa được cookie):
public static void clearCookie(HttpServletRequest request, HttpServletResponse response) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) return;
    for (Cookie cookie : cookies) {
        cookie.setMaxAge(-1);
        response.addCookie(cookie);
    }
}

// CODE MỚI (CHUẨN NGHIỆP VỤ):
public static void clearCookie(HttpServletRequest request, HttpServletResponse response) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) return;
    for (Cookie cookie : cookies) {
        Cookie deleteCookie = new Cookie(cookie.getName(), "");
        deleteCookie.setMaxAge(0); // 0 giây = xóa ngay lập tức
        deleteCookie.setPath("/");  // Bắt buộc cùng path lúc set
        response.addCookie(deleteCookie);
    }
}
```

---

### 2. Sửa kiểm tra tài khoản bị khóa trong `UserServices.java`
**Vị trí**: `src/main/java/com/fpoly/java5demo/services/UserServices.java`

```java
// CODE MỚI (CHUẨN NGHIỆP VỤ):
public User getLoggedInUser(HttpServletRequest request, HttpSession session) {
    if (session != null && session.getAttribute("user") != null) {
        User sessionUser = (User) session.getAttribute("user");
        User dbUser = getUserInfoById(sessionUser.getId());
        if (dbUser != null && dbUser.isStatus()) {
            session.setAttribute("user", dbUser);
            session.setAttribute("role", dbUser.getRole());
            return dbUser;
        } else {
            // Tài khoản đã bị khóa hoặc không tồn tại: hủy session ngay
            session.removeAttribute("user");
            session.removeAttribute("role");
            return null;
        }
    }
    ...
}
```

---

### 3. Nạp giỏ hàng từ CSDL vào Session khi Đăng nhập
**Vị trí**: `src/main/java/com/fpoly/java5demo/services/UserServices.java` (trong hàm `login`)

```java
// BỔ SUNG VÀO CUỐI HÀM login() TRƯỚC KHI RETURN user:
List<com.fpoly.java5demo.entities.CartDetail> dbCarts = cartDetailJPA.findByUserId(user.getId());
if (dbCarts != null && !dbCarts.isEmpty()) {
    Map<Integer, Integer> cartMap = cartDetailServices.getCartMapSession();
    for (com.fpoly.java5demo.entities.CartDetail item : dbCarts) {
        if (item.getProduct() != null && item.getProduct().isStatus()) {
            cartMap.put(item.getProduct().getId(),
                    cartMap.getOrDefault(item.getProduct().getId(), 0) + item.getQuantity());
        }
    }
    try {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        session.setAttribute("cart", mapper.writeValueAsString(cartMap));
        session.setAttribute("cartCount", cartMap.values().stream().mapToInt(Integer::intValue).sum());
    } catch (Exception ignored) {}
}
```

---

### 4. Dọn sạch giỏ hàng trong CSDL khi tạo đơn hàng thành công
**Vị trí**: `src/main/java/com/fpoly/java5demo/services/OrderServices.java` (trong hàm `createOrder`)

```java
// Sau dòng: cartDetailServices.clearCart();
// Thêm:
try {
    cartDetailJPA.deleteByUserId(user.getId());
} catch (Exception ignored) {}
```

---

## 6. KHUYẾN NGHỊ CHO BUỔI BÁO CÁO / BẢO VỆ MÔN HỌC

1. **Về Điểm Cộng Nghiệp Vụ Cần Nhấn Mạnh Khi Báo Cáo**:
   - Nhấn mạnh việc xử lý **Transaction (`@Transactional`)** trong việc kiểm tra tồn kho và trừ kho.
   - Nhấn mạnh tính năng **Bảo toàn dữ liệu**: Không xóa cứng tài khoản đã có hóa đơn mà tự động chuyển sang Khóa tài khoản.
   - Nhấn mạnh tính năng **Chống xem/hủy trộm đơn hàng (IDOR)** và **Tự động hoàn kho khi hủy đơn**.
2. **Về Kênh Người Bán**:
   - Dự án tập trung chuẩn hóa mô hình bán lẻ B2C (Cửa hàng - Khách hàng). Phân hệ Seller là phần mở rộng nâng cao, nếu giảng viên không yêu cầu thì nên tập trung demo quyền Admin và Khách hàng để đạt kết quả an toàn và trọn vẹn nhất.
3. **Môi trường Database**:
   - Dự án đã cấu hình sẵn **H2 Database in-memory** trong `application.properties`, có thể khởi chạy và demo tức thì trên bất kỳ máy tính nào mà không phụ thuộc vào SQL Server.
