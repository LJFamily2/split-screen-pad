# 📱 Split Screen Pad — Chia 2 & 3 Màn Hình Cho Xiaomi Redmi Pad SE 8.7

![Android 14](https://img.shields.io/badge/Android-14%20%2F%20HyperOS-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Platform](https://img.shields.io/badge/Target-Xiaomi%20Redmi%20Pad%20SE%208.7-FF6900?style=for-the-badge&logo=xiaomi&logoColor=white)
![Language](https://img.shields.io/badge/Kotlin%20%2F%20HTML5-007ACC?style=for-the-badge)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)

> 🚀 **Bộ công cụ đa nhiệm (multitasking) cho máy tính bảng Android — tối ưu cho Xiaomi Redmi Pad SE 8.7 (Android 14 / HyperOS).**
> Gom những tính năng chia màn hình tốt nhất của One UI (Samsung), HyperOS (Xiaomi) và iPadOS vào một app duy nhất.
> Hỗ trợ **2 khung** hoặc **3 khung** (1 app chiếm trọn một nửa + 2 app xếp trên/dưới ở nửa còn lại).

---

## 🔍 Tổng Quan (SEO Summary)

Nếu bạn đang tìm:

- **Cách chia đôi màn hình Xiaomi Redmi Pad SE 8.7** khi app hệ thống không hỗ trợ.
- **Sửa lỗi Split Screen bị ẩn hoặc giới hạn trên HyperOS / MIUI 14**.
- **Chạy 2–3 web/app song song trên Android tablet** mà không bị ngắt nhạc hay dừng video.
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

- Danh sách app cài trên máy được **quét sẵn ở luồng nền** lúc khởi động; bộ chọn app mở ra tức thì với danh sách web app rồi tự điền thêm app native khi quét xong (trước đây phải chờ quét hết mọi package trên main thread).
- **Icon app tải lười (lazy)** theo từng dòng và có cache — trước đây tải toàn bộ icon trước khi hiện dialog.
- Chạm thanh chia mở menu **ngay khi nhấc tay**, không phải chờ hết khoảng thời gian nhận diện double-tap (~300ms).
- Trang web **chỉ tải khi vào workspace**, không tải lúc còn ở màn hình chính; khung thứ ba chỉ tải khi bật bố cục 3 khung.

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

> ⚠️ **Lưu ý về iframe:** trình duyệt chặn nhúng các trang như Google, YouTube, Facebook (`X-Frame-Options`). Simulator sẽ hiện thông báo giải thích thay vì khung trắng. Trong app Android, các trang này chạy trong **WebView thật** nên không bị giới hạn đó. Các **trang demo** kèm sẵn (`simulator/pages/`) luôn hiển thị được để bạn thử toàn bộ thao tác chia màn hình.

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

Các khung web bên trong Split Pad thì **luôn** chia được (2 hoặc 3 khung), không phụ thuộc hệ thống.

---

## 🏷️ Keywords

`Xiaomi Redmi Pad SE 8.7` `Split Screen Android 14` `HyperOS Multitasking` `MIUI Split Screen Fix` `Chia đôi màn hình Xiaomi` `Chia 3 màn hình Android` `Three app split screen` `Dual WebView Android App` `App Pair Manager Tablet` `Chạy 2 ứng dụng cùng lúc Redmi Pad` `Continuous Background Media` `1:2 split layout` `Desktop View User-Agent Switcher` `Material You Glassmorphism`

---

## 🤝 Đóng Góp & Giấy Phép

- **License**: MIT
- **Developer / Maintainer**: SwangLee / LJFamily2
