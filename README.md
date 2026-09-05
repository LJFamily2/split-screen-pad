# 📱 Split Screen Pad — Chia Đôi Màn Hình & Cửa Sổ Nổi Cho Xiaomi Redmi Pad SE 8.7

![Android 14](https://img.shields.io/badge/Android-14%20%2F%20HyperOS-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Platform](https://img.shields.io/badge/Target-Xiaomi%20Redmi%20Pad%20SE%208.7-FF6900?style=for-the-badge&logo=xiaomi&logoColor=white)
![Language](https://img.shields.io/badge/Kotlin%20%2F%20HTML5-007ACC?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)

> 🚀 **Bộ công cụ đa nhiệm (multitasking) cho máy tính bảng Android — tối ưu cho Xiaomi Redmi Pad SE 8.7 (Android 14 / HyperOS).**
> Gom những tính năng chia đôi màn hình tốt nhất của One UI (Samsung), HyperOS (Xiaomi) và iPadOS vào một app duy nhất.

---

## 🔍 Tổng Quan (SEO Summary)

Nếu bạn đang tìm:

- **Cách chia đôi màn hình Xiaomi Redmi Pad SE 8.7** khi app hệ thống không hỗ trợ.
- **Sửa lỗi Split Screen bị ẩn hoặc giới hạn trên HyperOS / MIUI 14**.
- **Chạy 2 web/app song song trên Android tablet** mà không bị ngắt nhạc hay dừng video.
- **Cửa sổ nổi (Floating Window Overlay)** đè lên mọi ứng dụng khác.
- **Quản lý cặp ứng dụng (App Pairs)** để mở 2 app cùng lúc chỉ với 1 chạm.

**Split Screen Pad** gồm hai phần: **ứng dụng Android (Kotlin)** và **trình giả lập web (Web Simulator)** để xem trước giao diện ngay trên trình duyệt.

👉 **Dùng thử simulator:** mở `simulator/index.html` hoặc chạy `npm run serve` rồi vào `http://localhost:8080`.

---

## ⚡ Tính Năng

### Chia đôi màn hình (Split view)

| Thao tác | Mô tả |
| :--- | :--- |
| **Kéo thanh chia** | Đổi tỷ lệ tự do, hiện % thời gian thực (VD `65 : 35`) |
| **Kéo ra sát mép** | Đưa một khung thành **toàn màn hình** (như One UI) |
| **Chạm 2 lần vào thanh chia** | **Hoán đổi** hai khung ngay lập tức |
| **Chạm 1 lần vào thanh chia** | Mở **menu Split options** (swap · 50:50 · toàn màn hình · xoay · thả sang cửa sổ nổi · lưu cặp) |
| **Giữ lâu thanh chia** | Về lại tỷ lệ đều 50:50 |
| **Nút 1/3 · 1/2 · 2/3** | Tỷ lệ nhanh (snap ratio) |
| **Nút Rotate** | Đổi giữa **trên/dưới** (portrait) và **trái/phải** (landscape) |

### Từng khung (per-pane)

- Thanh địa chỉ riêng, gõ **tên miền → mở web**, gõ **từ khoá → tự tìm Google**.
- **Back / Forward / Reload** riêng cho mỗi khung, có thanh tiến trình.
- **Desktop / Mobile** riêng từng khung — một bên xem giao diện máy tính, một bên giao diện điện thoại.
- Menu `⋮`: toàn màn hình · mở ra cửa sổ nổi · gửi sang khung kia · copy link · chia sẻ · mở bằng trình duyệt hệ thống.
- **Video toàn màn hình** (YouTube fullscreen) hoạt động trong khung.
- **Nhạc / video không bị dừng** khi bạn thao tác ở khung còn lại.

### Cửa sổ nổi (Floating window / Pop-up view)

- Kéo thanh tiêu đề để di chuyển, **tự hít vào mép màn hình** (snap to edge).
- **Kéo góc dưới để đổi kích thước**.
- **Thu nhỏ thành bong bóng** (bubble), chạm để mở lại.
- **Đổi độ trong suốt** (4 mức) để nhìn xuyên qua app bên dưới.
- Vị trí, kích thước, độ trong suốt và địa chỉ **được nhớ lại sau khi khởi động lại**.

### Cặp ứng dụng (App Pairs)

- Lưu cặp đang mở với tên tuỳ ý, đổi tên, xoá.
- Danh sách cặp hiện ngay trên **màn hình chính**, chạm 1 lần là mở lại cả hai.

### Chọn 2 app (2-step picker)

- Bước 1 chọn app cho khung 1, bước 2 chọn app cho khung 2, có **ô tìm kiếm tức thì**.
- Liệt kê **app đã cài trên máy** (kèm icon thật) và các **web app** phổ biến (Zalo, YouTube, ChatGPT, Docs…).
- Thêm được **URL / PWA tuỳ ý**.

### Ghi nhớ & giao diện

- **Nhớ phiên làm việc**: hai địa chỉ, tỷ lệ chia, hướng chia, chế độ Desktop/Mobile.
- **Theo theme của máy**: sáng/tối tự động, và trên Android 12+ dùng **Material You** lấy màu nhấn từ hình nền.
- Thiết kế **Glassmorphism**: bề mặt kính mờ, viền sáng, nền mesh gradient — tương phản chữ đủ cao để đọc thoải mái ở cả hai chế độ.
- **Phím tắt** (khi gắn bàn phím): `Ctrl+1/2` chọn khung · `Ctrl+E` hoán đổi · `Ctrl+R` tải lại · `Ctrl+D` xoay · `Ctrl+M` toàn màn hình.

---

## 🛠️ Biên Dịch APK (`/android/`)

Yêu cầu: **JDK 17** + **Android SDK 34** (hoặc Android Studio Hedgehog trở lên).

```bash
cd android
chmod +x gradlew          # Linux / macOS
./gradlew assembleDebug   # Windows: .\gradlew.bat assembleDebug
```

File APK: `android/app/build/outputs/apk/debug/app-debug.apk` — copy sang máy tính bảng và cài đặt.

Chạy kiểm thử:

```bash
cd android && ./gradlew testDebugUnitTest    # unit test
python3 tools/verify_android.py              # kiểm tra tài nguyên & ID layout
```

> GitHub Actions (`.github/workflows/ci.yml`) tự build APK, chạy unit test và lint trên mỗi lần push.

---

## ⚙️ Cấp Quyền Cửa Sổ Nổi (HyperOS / MIUI)

1. Mở app, chọn **Floating window** (hoặc nút **⧉ Float** trên thanh công cụ).
2. Chạm **Open settings** → Android mở thẳng mục **Hiển thị trên các ứng dụng khác**.
3. Tìm **Split Screen Pad** và **BẬT**.

---

## 🌐 Trình Giả Lập Web (`/simulator/`)

```bash
npm run serve      # rồi mở http://localhost:8080
```

Simulator mô phỏng đầy đủ thao tác của app: kéo thanh chia (hỗ trợ **cả chuột và cảm ứng**), tỷ lệ nhanh, hoán đổi, xoay, toàn màn hình, cửa sổ nổi, cặp ứng dụng, dock cạnh màn hình và chuyển theme sáng/tối.

Chạy kiểm thử trình duyệt (Playwright + Chromium):

```bash
npm install
npm test
```

> ⚠️ **Lưu ý về iframe:** trình duyệt chặn nhúng các trang như Google, YouTube, Facebook (`X-Frame-Options`). Simulator sẽ hiện thông báo giải thích thay vì khung trắng. Trong app Android, các trang này chạy trong **WebView thật** nên không bị giới hạn đó. Các **trang demo** kèm sẵn (`simulator/pages/`) luôn hiển thị được để bạn thử toàn bộ thao tác chia màn hình.

---

## 📁 Cấu Trúc Dự Án

```
split-screen-pad/
├── android/                        # Ứng dụng Android (Kotlin)
│   └── app/src/main/
│       ├── java/com/splitview/pad/
│       │   ├── MainActivity.kt            # Màn hình chính + workspace chia đôi
│       │   ├── SplitLayoutController.kt   # Tỷ lệ, hướng chia, cử chỉ thanh chia
│       │   ├── WebPane.kt                 # Một khung web: điều hướng, UA, fullscreen
│       │   ├── FloatingOverlayService.kt  # Cửa sổ nổi (kéo/resize/bubble/opacity)
│       │   ├── AppPickerDialog.kt         # Bộ chọn 2 bước
│       │   ├── AppCatalog.kt              # Danh sách app + web tương đương
│       │   ├── NativeSplitLauncher.kt     # Mở 2 app hệ thống cạnh nhau
│       │   ├── AppPair.kt / Prefs.kt      # Cặp ứng dụng & ghi nhớ phiên
│       │   └── UrlUtils.kt                # Xử lý địa chỉ / tìm kiếm
│       └── res/                    # Layout, drawable kính mờ, values + values-night
├── simulator/                      # Trình giả lập web
│   ├── index.html · style.css · simulator.js
│   └── pages/                      # Trang demo nhúng được
├── tests/simulator.test.js         # Kiểm thử trình duyệt (Playwright)
├── tools/verify_android.py         # Kiểm tra tài nguyên Android tĩnh
└── .github/workflows/ci.yml        # Build APK + test tự động
```

---

## ⚠️ Giới Hạn Thật Sự Của Android (nói rõ để không hiểu nhầm)

Android **chỉ cho phép** một app mở app khác cạnh mình (`FLAG_ACTIVITY_LAUNCH_ADJACENT`) khi app đó **đã ở trong chế độ chia đôi màn hình**. Không có API công khai nào ép hệ thống chuyển từ toàn màn hình sang split.

Vì vậy khi bạn chọn **2 app đã cài**:

- Nếu Split Pad đang ở chế độ chia đôi → mở thẳng app thứ hai sang nửa còn lại.
- Nếu không → app sẽ hỏi bạn chọn: **mở lần lượt cả hai app** (rồi kéo từ Recents sang nửa còn lại), hoặc **dùng bản web** của hai app đó, hiển thị song song ngay trong Split Pad.

Hai khung web bên trong Split Pad thì **luôn** chia đôi được, không phụ thuộc hệ thống.

---

## 🏷️ Keywords

`Xiaomi Redmi Pad SE 8.7` `Split Screen Android 14` `HyperOS Multitasking` `MIUI Split Screen Fix` `Chia đôi màn hình Xiaomi` `Cửa sổ nổi HyperOS` `Floating Browser Overlay` `Dual WebView Android App` `App Pair Manager Tablet` `Chạy 2 ứng dụng cùng lúc Redmi Pad` `Continuous Background Media` `Desktop View User-Agent Switcher` `Material You Glassmorphism`

---

## 🤝 Đóng Góp & Giấy Phép

- **License**: MIT
- **Developer / Maintainer**: SwangLee / LJFamily2
