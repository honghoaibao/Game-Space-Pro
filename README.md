# GAME SPACE

Game booster / optimizer / game space cho Android. Xem `.claude/PROJECT_PLAN.md` để biết trạng thái từng hệ thống, `.claude/ARCHITECTURE.md` cho quyết định kiến trúc, `.claude/TASK_BACKLOG.md` cho việc còn lại theo từng phiên làm việc tiếp theo.

## Mở dự án
1. Mở bằng **Android Studio Ladybug (2024.2)+** — File → Open → chọn thư mục `GameSpace/`.
2. Android Studio sẽ tự tạo `gradle-wrapper.jar` còn thiếu (do môi trường tạo project này không có mạng để tải file nhị phân). Nếu build qua CLI trước, chạy:
   ```bash
   gradle wrapper --gradle-version 8.9
   ```
   (cần có Gradle cài sẵn trên máy một lần duy nhất để sinh wrapper, các lần sau dùng `./gradlew`).
3. Sync Gradle, chạy cấu hình `app` trên thiết bị/emulator API 26+.

## Đã hoàn thành (Phiên 1-6 — xem `.claude/PROJECT_PLAN.md` để biết chi tiết từng mục)
- Project scaffold: Gradle Kotlin DSL, Hilt, Room, Compose (Material 3), Shizuku API.
- `hardware/` `profile/` `shizuku/` `optimizer/` `packagemanager/`: Hardware Detection, Performance Profiles (kể cả giảm độ phân giải Low Mode), Optimizer Engine, Thư viện Game.
- `overlay/`: Floating HUD (FPS/RAM/Nhiệt/Pin + Quick Tools) và Floating WebView (Popup Apps).
- `automation/` `thermal/`: Smart Automation phát hiện game qua UsageStatsManager, Thermal Protection tự hạ/khôi phục Profile.
- `accessibility/` `diagnostics/` `recovery/`: Accessibility Engine (tùy chọn), màn Diagnostics xuất báo cáo, Recovery Manager khôi phục sau crash.
- `protection/`: Bảo vệ app nhạc (Spotify/YouTube Music/...) khỏi bị Optimizer dọn/buộc dừng — thay cho "Music Hub" theo yêu cầu.
- `ui/performance/`: Performance Center — biểu đồ FPS/RAM/Pin realtime.
- Floating Chat: shortcut mở nhanh Discord/Telegram/Messenger/Zalo.
- `logging/`: `LogManager` (Room-backed) + export log ra file `.txt` chia sẻ được.
- `.github/workflows/ci.yml`: build debug/release, test, lint, ktlint, detekt, ký APK nếu có secrets.

## Phiên 7 — UI cao cấp theo màu logo + fix CPU/popup
- `ui/theme/`: bảng màu đo từ logo (xanh neon `#00B3FD` + navy `#062240`), luôn Dark Theme, bo góc rộng hơn, nền gradient + quầng sáng neon toàn app, TopAppBar trong suốt xuyên suốt 5 màn hình.
- `hardware/CpuUsageReader.kt`: CPU% giờ đọc thật qua `/proc/stat` (fallback Shizuku) thay vì luôn `null`.
- Dashboard: thêm card "Floating HUD" — mở/đóng popup HUD thủ công, độc lập với Smart Automation.
- `GameDetectionService`: thêm grace period trước khi kết thúc session để popup không còn tắt/nhấp nháy nhầm khi có dialog/thông báo thoáng qua; giảm chu kỳ poll cho cảm giác mở nhanh hơn; báo lỗi rõ ràng qua Toast khi popup không mở được thay vì im lặng.

## Icon
Đã thay bằng logo GAME SPACE thật (Trr gửi ngày 12/07). Icon mark (G-swirl + tay cầm, không kèm chữ) dùng cho:
- Adaptive icon (API 26+): `drawable/ic_launcher_background.xml` (nền trắng) + `drawable-xxxhdpi/ic_launcher_foreground.png`
- Icon legacy (`mipmap-*/ic_launcher.png` + `ic_launcher_round.png`) cho launcher không hỗ trợ adaptive
- `/mnt/user-data/outputs/gamespace_playstore_icon_512.png` (512×512) — ảnh riêng để upload thủ công lên Play Console khi cần, không nằm trong APK
- Logo đầy đủ (kèm chữ "GAME SPACE") lưu tại `drawable-nodpi/logo_full.png`, chưa dùng ở đâu trong UI — có thể dùng làm header cho màn About/Diagnostics sau này nếu muốn.

## Bước tiếp theo
Toàn bộ 6 phiên trong `.claude/TASK_BACKLOG.md` đã hoàn thành (đặc tả gốc + phần mở rộng). Muốn làm tiếp, mô tả tính năng/thay đổi mới trực tiếp trong chat, hoặc gõ **"Tiếp tục"** để rà lại các mục "Lưu ý còn để lại" ghi trong từng phiên (polish nhỏ, chưa phải bug chặn dùng).
