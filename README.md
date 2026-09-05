# 📱 Split Screen Pad - Ứng dụng Chia Đôi Màn Hình & Trình Giả Lập cho Xiaomi Redmi Pad SE 8.7

![Android 14](https://img.shields.io/badge/Android-14%20%2F%20HyperOS-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Platform](https://img.shields.io/badge/Target-Xiaomi%20Redmi%20Pad%20SE%208.7-FF6900?style=for-the-badge&logo=xiaomi&logoColor=white)
![Language](https://img.shields.io/badge/Language-Kotlin%20%2F%20HTML5-007ACC?style=for-the-badge)

Ứng dụng đa nhiệm chia đôi màn hình được thiết kế tối ưu riêng cho dòng máy tính bảng **Xiaomi Redmi Pad SE 8.7** (Màn hình 1340 × 800 pixel, Android 14 / MIUI / HyperOS).

Dự án bao gồm 2 phần chính:
1. **Ứng dụng Native Android (`/android/`)**: Dự án Android Studio viết bằng Kotlin hỗ trợ 2 WebView độc lập, thanh kéo chia màn hình linh hoạt hiển thị tỉ lệ %, chuyển đổi chế độ Desktop/Mobile User-Agent, phát phương tiện liên tục ở nền, lưu và quản lý cặp ứng dụng (App Pairs), và chế độ **Cửa sổ nổi (System Alert Window Floating Overlay)** hiển thị đè lên các ứng dụng Android khác.
2. **Trình giả lập Web Tương tác (`/simulator/`)**: Web app giao diện Glassmorphism mô phỏng chính xác khung hình máy tính bảng Redmi Pad SE 8.7 với thanh kéo chia màn hình, xem trước chế độ Desktop và điều khiển cửa sổ nổi.

---

## 🎯 Vấn Đề Ứng Dụng Giải Quyết (Problem Statement)

Trên các dòng máy tính bảng cỡ nhỏ như **Xiaomi Redmi Pad SE 8.7** (8.7 inch), tính năng đa nhiệm chia đôi màn hình mặc định của Android / HyperOS thường gặp các hạn chế sau:

- ❌ **Giới hạn ứng dụng**: Nhiều ứng dụng hoặc trang web không hỗ trợ tính năng chia đôi màn hình mặc định của hệ thống.
- ❌ **Tỉ lệ chia cứng nhắc**: Thanh phân cách mặc định bị giới hạn tỉ lệ, không hiển thị rõ phần trăm diện tích hiển thị và khó điều chỉnh chính xác theo nhu cầu làm việc.
- ❌ **Tạm dừng âm thanh/video**: Khi chuyển thao tác giữa 2 cửa sổ hoặc thu nhỏ app, phương tiện đa truyền thông (YouTube, nhạc, podcast) thường tự động bị tạm dừng (pause).
- ❌ **Thiếu tuỳ chỉnh User-Agent**: Không thể mở 1 bên ở giao diện Máy tính (Desktop view) để xem đầy đủ tính năng và 1 bên ở giao diện Điện thoại (Mobile view) để tiết kiệm diện tích.
- ❌ **Không có Cửa sổ Nổi tự do over-app**: Khi đang dùng một ứng dụng Native Android khác (game, ghi chú, đọc sách), người dùng khó có thể bật nhanh một cửa sổ duyệt web nổi đè lên trên để tra cứu thông tin mà không làm ngắt quãng ứng dụng chính.
- ❌ **Phải mở ứng dụng thủ công từng bên**: Mất thời gian chọn từng app mỗi khi muốn làm việc đa nhiệm.

### 💡 Giải Pháp Từ "Split Screen Pad":
- ✅ **Hỗ trợ 2 WebView độc lập mạnh mẽ**: Duyệt bất kỳ trang web hoặc dịch vụ trực tuyến nào song song.
- ✅ **Thanh kéo linh hoạt 30/70 - 50/50 - 70/30**: Hiển thị tỉ lệ % thực tế (`50% : 50%`) giúp bạn căn chỉnh diện tích xem tối ưu nhất.
- ✅ **Chế độ Cửa sổ Nổi Overlay (`SYSTEM_ALERT_WINDOW`)**: Cho phép mở cửa sổ trình duyệt nổi đè lên **BẤT KỲ** ứng dụng Android nào trên máy.
- ✅ **Phát phương tiện liên tục**: Nhạc/video vẫn tiếp tục chạy mượt mà ở khung bên này trong khi bạn thao tác, gõ phím ở khung bên kia.
- ✅ **Quản lý Cặp ứng dụng (App Pair Manager)**: Lưu các bộ trang web/app thường dùng (VD: YouTube + Google Search, ChatGPT + Wikipedia, Google Docs + Từ điển) và mở lại chỉ với **1 chạm**.
- ✅ **Chuyển đổi Desktop / Mobile View riêng biệt**: Tùy chỉnh User-Agent và tỉ lệ phóng to (Viewport scale) độc lập cho từng bên màn hình.

---

## ⚡ Các Tính Năng Nổi Bật

- 📱 **Chia Màn Hình Dọc & Ngang**: Chuyển đổi linh hoạt giữa chế độ chia Trên - Dưới (Portrait) và chia Trái - Phải (Landscape).
- 🔲 **Cửa sổ Nổi Overlay trên Android 14**: Thu nhỏ thành cửa sổ nổi có thể di chuyển, phóng to, thu nhỏ hoặc ẩn đè lên mọi ứng dụng khác.
- ⭐ **Trình Quản Lý Cặp Ứng Dụng**: Lưu trữ, chỉnh sửa và xóa danh sách các Cặp Ứng Dụng yêu thích.
- 🚀 **Trình Chọn Ứng Dụng Native (App Picker)**: Hỗ trợ hiển thị biểu tượng icon ứng dụng đã cài trên máy để mở nhanh vào Pane 1, Pane 2 hoặc Floating Overlay.
- 🎵 **Continuous Media Playback**: Phát video/âm thanh không bị gián đoạn khi chuyển tiêu điểm (focus).
- ↻ **Thao tác Điều hướng Độc lập**: Nút Tải lại (Reload) riêng cho từng pane và tích hợp bộ xử lý nút Back thông minh (`OnBackPressedDispatcher`) của Android 14.

---

## 🛠️ Hướng Dẫn Biên Dịch & Cài Đặt File APK Android (`/android/`)

### Yêu cầu tiên quyết:
- **Android Studio** (bản Hedgehog / Iguana / Jellyfish trở lên) HOẶC **JDK 17** đã cài đặt Android SDK 34.
- Thiết bị Android (Khuyên dùng: **Xiaomi Redmi Pad SE 8.7** chạy HyperOS / Android 14).

### Các bước biên dịch APK:
1. Mở cửa sổ dòng lệnh (Terminal / PowerShell) và di chuyển vào thư mục `android`:
   ```bash
   cd android
   ```
2. Chạy lệnh Gradle để build file APK Debug:
   - **Trên Windows (PowerShell / CMD)**:
     ```powershell
     .\gradlew.bat assembleDebug
     ```
   - **Trên Linux / macOS**:
     ```bash
     chmod +x gradlew
     ./gradlew assembleDebug
     ```
3. Sau khi biên dịch thành công, file APK sẽ nằm tại đường dẫn:
   ```
   android/app/build/outputs/apk/debug/app-debug.apk
   ```
4. Copy file `app-debug.apk` vào máy tính bảng **Xiaomi Redmi Pad SE 8.7** và tiến hành cài đặt.

---

## ⚙️ Hướng Dẫn Cấp Quyền Trên Xiaomi HyperOS / MIUI

Đề sử dụng **Chế độ Cửa sổ Nổi (Overlay Mode)** hiển thị đè lên các ứng dụng Android khác:

1. Khởi chạy ứng dụng **Split Screen Pad** trên máy tính bảng.
2. Nhấn vào nút **🔲 Overlay Mode** trên thanh công cụ phía trên.
3. Chọn **Cấp quyền (Grant Permission)** khi biểu mẫu hiện ra.
4. Màn hình cài đặt Android sẽ tự động mở đến mục **Hiển thị trên các ứng dụng khác (Display over other apps)**.
5. Tìm ứng dụng **Split Screen Pad** và chuyển trạng thái sang **BẬT (ON)**.

---

## 🌐 Hướng Dẫn Chạy Trình Giả Lập Web (`/simulator/`)

Bạn có thể trải nghiệm trước giao diện chia màn hình ngay trên trình duyệt máy tính mà không cần cài đặt APK:

1. Mở trực tiếp file `index.html` hoặc `simulator/index.html` bằng trình duyệt web bất kỳ.
2. Hoặc khởi chạy local development server từ thư mục gốc dự án:
   ```bash
   npx http-server . -p 8080
   ```
3. Truy cập đường dẫn: `http://localhost:8080` trên trình duyệt.

---

## 📁 Cấu Trúc Dự Án

```
Resize application/
├── android/                   # Mã nguồn ứng dụng Native Android (Kotlin)
│   ├── app/src/main/
│   │   ├── java/              # Các Activity, Service (Floating Overlay), WebView Managers
│   │   ├── res/               # Giao diện XML, icon, layout, màu sắc
│   │   └── AndroidManifest.xml # Khai báo quyền SYSTEM_ALERT_WINDOW & cấu hình app
│   └── gradlew.bat            # Lệnh build ứng dụng trên Windows
├── simulator/                 # Mã nguồn bản Giả lập trên Web (HTML5/CSS3/JS)
│   ├── index.html             # Giao diện web mô phỏng Redmi Pad SE 8.7
│   ├── style.css              # Style Glassmorphic & layout đa nhiệm
│   └── app.js                 # Xử lý logic chia màn hình & cửa sổ nổi trên web
├── index.html                 # Trang chuyển hướng nhanh tới Simulator
├── README.md                  # Tài liệu hướng dẫn sử dụng (Tiếng Việt)
└── .gitignore                 # Cấu hình bỏ qua file build tạm thời
```

---

## 🤝 Đóng Góp & Bảo Trì

Mọi đóng góp, báo lỗi (issue) hoặc đề xuất tính năng mới đều được hoan nghênh. Xin vui lòng tạo Pull Request hoặc gửi Issue trực tiếp trên GitHub repository.

**License**: Open Source / MIT License.
