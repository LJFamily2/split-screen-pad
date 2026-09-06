# 📱 Split Screen Pad — Chia 2 & 3 Màn Hình Cho Android Tablet & Foldables

![Android 7.0+](https://img.shields.io/badge/Android-7.0%2B%20%2F%2014%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Platform](https://img.shields.io/badge/Target-Android%20Tablets%20%26%20Foldables-FF6900?style=for-the-badge&logo=android&logoColor=white)
![Language](https://img.shields.io/badge/Kotlin%20%2F%20HTML5-007ACC?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)

> 🚀 **Bộ công cụ đa nhiệm (multitasking) chuyên nghiệp cho máy tính bảng và màn hình gập Android.**
> Gom những tính năng chia màn hình tốt nhất của One UI (Samsung), HyperOS (Xiaomi), iPadOS và stock Android vào một ứng dụng duy nhất.
> Hỗ trợ **2 khung** hoặc **3 khung** (1 app chiếm trọn một nửa + 2 app xếp trên/dưới ở nửa còn lại).

---

## 🔍 Tổng Quan (SEO Summary)

Nếu bạn đang tìm:

- **Cách chia đôi màn hình trên máy tính bảng & điện thoại Android** khi app hệ thống giới hạn.
- **Sửa lỗi Split Screen bị hạn chế trên Android tablet / màn hình gập**.
- **Chạy 2–3 web/app song song trên Android** mà không bị ngắt nhạc hay dừng video.
- **Bố cục 1:2** — một app chiếm trọn nửa màn hình, nửa còn lại chia đôi trên/dưới.
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
| **Chạm vào thanh chia** | Mở **menu Split options** ngay lập tức (swap · 50:50 · toàn màn hình · xoay · đổi 2↔3 khung · lưu cặp) |
| **Giữ lâu thanh chia** | Về lại tỷ lệ đều 50:50 |
| **Nút 1/3 · 1/2 · 2/3** | Tỷ lệ nhanh (snap ratio) |
| **Nút Rotate** | Đổi giữa **trên/dưới** (portrait) và **trái/phải** (landscape) |
| **Nút 2 panes / 3 panes** | Đổi giữa 2 khung và **bố cục 1:2** |

### Bố cục 3 khung (1:2)

Một app chiếm **trọn một nửa** màn hình, nửa còn lại chia thành **một khung trên và một khung dưới** — giống chế độ 3 app của One UI.

- Có **thanh chia thứ hai** riêng cho cặp trên/dưới, kéo độc lập với thanh chia chính.
- Menu **3 chấm (⋮)** ở đầu mỗi khung cho phép **đổi vị trí** nhanh: *Move to Left half · Move to Top right · Move to Bottom right*. Tên vị trí tự đổi theo hướng chia hiện tại.
- **Add a third pane** / **Close this pane** ngay trong menu 3 chấm. Khi đóng một khung, hai khung còn lại tự dồn lên đúng vị trí.
- Số khung, hai tỷ lệ chia và hướng chia đều **được nhớ lại**.

### Từng khung (per-pane)

- Thanh địa chỉ riêng, gõ **tên miền → mở web**, gõ **từ khoá → tự tìm Google**.
- **Back / Forward / Reload** riêng cho mỗi khung, có thanh tiến trình.
- **Desktop / Mobile** riêng từng khung — một bên xem giao diện máy tính, một bên giao diện điện thoại.
- Menu `⋮` ở đầu khung: toàn màn hình · đổi vị trí (Move to…) · thêm/đóng khung · Desktop/Mobile · copy link · chia sẻ · mở bằng trình duyệt hệ thống.
- **Video toàn màn hình** (YouTube fullscreen) hoạt động trong khung.
- **Nhạc / video không bị dừng** khi bạn thao tác ở khung còn lại.

### Cặp ứng dụng (App Pairs)

- Lưu cặp đang mở với tên tuỳ ý, đổi tên, xoá.
- Danh sách cặp hiện ngay trên **màn hình chính**, chạm 1 lần là mở lại cả hai.

### Chọn 2 app (2-step picker)

- Bước 1 chọn app cho khung 1, bước 2 chọn app cho khung 2, có **ô tìm kiếm tức thì**.
- Liệt kê **app đã cài trên máy** (kèm icon thật) và các **web app** phổ biến (Zalo, YouTube, ChatGPT, Docs…).
- Thêm được **URL / PWA tuỳ ý**.

### Ghi nhớ & giao diện

- **Nhớ phiên làm việc**: địa chỉ từng khung, số khung, hai tỷ lệ chia, hướng chia, chế độ Desktop/Mobile.
- **Theo theme của máy**: sáng/tối tự động, và trên Android 12+ dùng **Material You** lấy màu nhấn từ hình nền.
- Thiết kế **Glassmorphism**: bề mặt kính mờ, viền sáng, nền mesh gradient — tương phản chữ đủ cao để đọc thoải mái ở cả hai chế độ.
- **Phím tắt** (khi gắn bàn phím): `Ctrl+1/2/3` chọn khung · `Ctrl+E` hoán đổi · `Ctrl+R` tải lại · `Ctrl+D` xoay · `Ctrl+M` toàn màn hình.

### ⚡ Tối ưu tốc độ

Menu và pop-up mở **ngay lập tức**, không còn độ trễ:

- Danh sách app cài trên máy được **quét sẵn ở luồng nền** lúc khởi động; bộ chọn app mở ra tức thì với danh sách web app rồi tự điền thêm app native khi quét xong.
- **Icon app tải lười (lazy)** theo từng dòng và có cache.
- Chạm thanh chia mở menu **ngay khi nhấc tay**.
- Trang web **chỉ tải khi vào workspace**, không tải lúc còn ở màn hình chính.

---

## 🛠️ Biên Dịch APK (`/android/`)

Yêu cầu: **JDK 17** + **Android SDK 34** (hoặc Android Studio).

```bash
cd android
chmod +x gradlew          # Linux / macOS
./gradlew assembleDebug   # Windows: .\gradlew.bat assembleDebug
```

File APK: `android/app/build/outputs/apk/debug/app-debug.apk` — copy sang máy tính bảng / điện thoại Android và cài đặt.

Chạy kiểm thử:

```bash
cd android && ./gradlew testDebugUnitTest    # unit test
python3 tools/verify_android.py              # kiểm tra tài nguyên & ID layout
```

---

## 🌐 Trình Giả Lập Web (`/simulator/`)

```bash
npm run serve      # rồi mở http://localhost:8080
```

Simulator mô phỏng đầy đủ thao tác của app: kéo thanh chia (hỗ trợ **cả chuột và cảm ứng**), tỷ lệ nhanh, hoán đổi, xoay, toàn màn hình, **bố cục 2 và 3 khung**, menu 3 chấm đổi vị trí, cặp ứng dụng, dock cạnh màn hình và chuyển theme sáng/tối.

Chạy kiểm thử trình duyệt (Playwright + Chromium):

```bash
npm install
npm test
```

---

## 📁 Cấu Trúc Dự Án

```
split-screen-pad/
├── android/                        # Ứng dụng Android (Kotlin)
│   └── app/src/main/
│       ├── java/com/splitview/pad/
│       │   ├── MainActivity.kt            # Màn hình chính + workspace chia khung
│       │   ├── SplitLayoutController.kt   # 2/3 khung, tỷ lệ, hướng chia, cử chỉ
│       │   ├── WebPane.kt                 # Một khung web: điều hướng, UA, fullscreen
│       │   ├── AppPickerDialog.kt         # Bộ chọn 2 bước (mở tức thì)
│       │   ├── AppCatalog.kt              # Quét app nền + cache icon
│       │   ├── NativeSplitLauncher.kt     # Mở 2 app hệ thống song song (FLAG_ACTIVITY_LAUNCH_ADJACENT)
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

## 📱 Khả Năng Chia Màn Hình Native & Web View

- **Native App Split Launch**: Hỗ trợ mở 2 app native đã cài đặt side-by-side thông qua cờ hệ thống `FLAG_ACTIVITY_LAUNCH_ADJACENT | FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_MULTIPLE_TASK`.
- **Dual Web Workspace**: Tích hợp trình duyệt đa khung (2 hoặc 3 pane) cho phép chạy mượt mà bất kỳ trang web / PWA nào song song mà không phụ thuộc vào giới hạn hệ điều hành.

---

## 🏷️ Keywords

`Android Split Screen` `Android Tablet Multitasking` `Foldable Multitasking` `Chia đôi màn hình Android` `Chia 3 màn hình Android` `Three app split screen` `Dual WebView Android App` `App Pair Manager Tablet` `Continuous Background Media` `1:2 split layout` `Desktop View User-Agent Switcher` `Material You Glassmorphism`

---

## 🤝 Đóng Góp & Giấy Phép

- **License**: MIT
- **Developer / Maintainer**: SwangLee / LJFamily2
