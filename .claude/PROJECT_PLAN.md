# GAME SPACE — Kế hoạch dự án

> App booster/game space cho Android. Package: `com.gamespace`.
> Kiến trúc: Kotlin, Jetpack Compose (Material 3), Hilt, Room, Shizuku API, Foreground Services, AccessibilityService.

## Trạng thái tổng quan (cập nhật mỗi phiên)

| # | Hệ thống | Trạng thái | Ghi chú |
|---|----------|-----------|---------|
| 1 | Project scaffold (Gradle, Manifest, theme) | ✅ Xong (Phiên 1) | |
| 2 | Hardware Detection | ✅ Xong (Phiên 1) | CPU/RAM/Display/Thermal API/Shizuku status |
| 3 | Performance Profiles (Low/Balanced/Performance) | ✅ Xong (Phiên 1, bổ sung Phiên 4) | Data layer + apply logic; Phiên 4 thêm giảm/khôi phục độ phân giải (Low Mode) |
| 4 | Shizuku Engine (Shell/Command Executor, Capability Detector) | ✅ Xong (Phiên 1) | Fallback khi chưa bật Shizuku |
| 5 | Logging | ✅ Xong (Phiên 1) | Room-backed, export .txt |
| 6 | Dashboard UI (Compose) | ✅ Xong (Phiên 1) | Màn hình chính + chọn Profile + Device status |
| 7 | CI/CD (GitHub Actions) | ✅ Xong (Phiên 1) | Build debug/release, lint, test, ktlint, detekt |
| 8 | Package Manager (danh sách game, thời gian chơi) | ✅ Xong (Phiên 2) | Room + quét category GAME, màn "Thư viện Game" |
| 9 | Optimizer Engine (RAM cleaner, cache, force-stop) | ✅ Xong (Phiên 2) | Deep RAM Cleaner/Cache/Storage Trim/Smart Compile, đã wiring vào Dashboard + ProfileEngine |
| 10 | Game Space Overlay (Floating HUD FPS/RAM/Nhiệt/Pin) | ✅ Xong (Phiên 3) | WindowManager+ComposeView, kéo thả, Quick Tools |
| 11 | Smart Automation (auto detect game mở/đóng) | ✅ Xong (Phiên 3) | UsageStatsManager polling, áp Profile/mở Overlay/ghi thời gian chơi |
| 12 | Thermal Protection (theo dõi & tự hạ Profile) | ✅ Xong (Phiên 3) | `ThermalGuard` chạy chung vòng đời Automation Service (ADR-005) |
| 13 | Accessibility Engine | ✅ Xong (Phiên 4) | Tùy chọn — hỗ trợ Automation qua `AccessibilityStateBus`, không phải phụ thuộc lõi |
| 14 | Diagnostics + export report | ✅ Xong (Phiên 4) | Màn "Diagnostics" mới + xuất report/log qua `ACTION_SEND` |
| 15 | Recovery Manager (khôi phục sau crash/kill) | ✅ Xong (Phiên 4) | Dirty-bit qua `ProcessLifecycleOwner`, khôi phục resolution/refresh-rate/Profile |
| 16 | Popup Apps / Floating windows (TikTok, YouTube PiP, Chrome...) | ✅ Xong (Phiên 5) | Floating WebView tự kiểm soát + gợi ý mở YouTube cho PiP (ADR-001) |
| 17 | Bảo vệ App Nhạc (thay Music Hub theo yêu cầu) | ✅ Xong (Phiên 6) | Loại trừ app nhạc khỏi Deep RAM Cleaner/Force Stop — xem ADR-009 |
| 18 | Floating Chat (shortcut Discord/Telegram/Messenger/Zalo) | ✅ Xong (Phiên 6) | Chỉ mở app thật qua Intent, không nổi được nội dung (ADR-001) |
| 19 | Performance Center (biểu đồ FPS/RAM/CPU/Nhiệt realtime) | ✅ Xong (Phiên 6) | Canvas tự vẽ, tái dùng `HudMetricsCollector`, không thêm thư viện ngoài |
| 20 | UI cao cấp theo màu logo + fix CPU/popup mở tay/popup delay-biến mất | ✅ Xong (Phiên 7) | Design system mới (`ui/theme/`), `CpuUsageReader`, `FloatingHudCard`, grace period + fix gesture bubble |

## Nguyên tắc làm việc
- Mỗi phiên cập nhật bảng trạng thái ở trên trước khi kết thúc.
- Quyết định kiến trúc quan trọng ghi vào `ARCHITECTURE.md` (mục ADR).
- Việc còn lại, chia nhỏ theo phiên, nằm trong `TASK_BACKLOG.md`.
- Không có kernel patch / ép xung — chỉ dùng API công khai + Shizuku (ADB-level), luôn có fallback khi thiếu quyền.
- Gõ "Tiếp tục" để làm tiếp task tiếp theo trong backlog theo đúng thứ tự ưu tiên.
