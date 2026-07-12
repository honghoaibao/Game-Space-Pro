# GAME SPACE — Kiến trúc

## Stack
- Kotlin 2.0, Jetpack Compose (Material 3), Hilt (DI), Room (persistence), Kotlin Coroutines/Flow.
- Min SDK 26 (Android 8.0) — Overlay/Foreground Service yêu cầu. Target/Compile SDK 35.
- Shizuku API (`dev.rikka.shizuku:api` + `:provider`) cho các lệnh cần quyền shell (fstrim, force-stop, refresh-rate override...), luôn có fallback UI-only khi Shizuku chưa cài/chưa cấp quyền.

## Cấu trúc package (`com.gamespace`)
```
hardware/       Đọc thông tin phần cứng & khả năng hệ thống (DeviceInfo, HardwareDetector)
profile/        Performance Profiles (Low/Balanced/Performance) — model + engine áp dụng
shizuku/        Shell Executor, Command Executor, Capability Detector, Permission Checker
thermal/        Theo dõi nhiệt độ, PowerManager.THERMAL_STATUS_*
optimizer/      RAM cleaner, cache cleaner, force-stop nền, package optimizer
packagemanager/ Room entity/dao cho danh sách game đã theo dõi
accessibility/  AccessibilityService: phát hiện app foreground, dialog
automation/     Foreground service lắng nghe game mở/đóng, áp Profile tự động
overlay/        Floating HUD (WindowManager overlay), Quick Tools
logging/        LogManager (Room-backed), export log
recovery/       Khôi phục resolution/density/refresh-rate/profile sau crash
diagnostics/    Tổng hợp báo cáo chẩn đoán, export
ui/             Compose: theme, dashboard, common components
```

## ADR-001: Không giả lập "biến mọi app thành cửa sổ nổi"
Android không cho phép một app thứ ba ép buộc app khác chạy dạng floating window tùy ý.
Popup Apps sẽ triển khai theo 2 hướng khả thi thực tế:
1. **PiP (Picture-in-Picture)**: chỉ hoạt động với app đích đã hỗ trợ PiP (YouTube, một số trình phát video). GAME SPACE có thể *gợi ý* người dùng bật PiP, không thể ép app khác vào PiP.
2. **Floating WebView nội bộ**: với nội dung web (Chrome-like: tra cứu, hướng dẫn, wiki, ChatGPT/Gemini web) GAME SPACE tự vẽ một cửa sổ WebView nổi (`TYPE_APPLICATION_OVERLAY`) do chính app kiểm soát — khả thi 100%.
Không dùng Accessibility để "ép" app khác vào overlay — vi phạm chính sách Google Play và không ổn định.

## ADR-002: Shizuku là "nâng cao", không phải bắt buộc
Mọi tính năng cần quyền cao (fstrim, refresh-rate override, force-stop hàng loạt, thay device_config) đều đi qua `ShellExecutor` có interface chung; nếu Shizuku chưa sẵn sàng, `CapabilityDetector` trả về `false` và UI ẩn/disable tính năng đó kèm hướng dẫn bật Shizuku — không crash, không giả lập kết quả giả.

## ADR-003: Thermal & Refresh rate qua API công khai trước
- Nhiệt độ: `PowerManager.getCurrentThermalStatus()` (API 29+) là nguồn chính; API <29 fallback dùng `BatteryManager.EXTRA_TEMPERATURE` (chỉ đo nhiệt pin, không phải nhiệt CPU — ghi rõ trong UI).
- Refresh rate: `Display.getSupportedModes()` + `Display.Mode` để đổi qua `Window.setPreferredDisplayMode` (không cần root/Shizuku, hoạt động trong phạm vi app).
- Override refresh rate toàn hệ thống (ngoài app) cần Shizuku + `settings put system peak_refresh_rate` tuỳ máy — optional, có Capability check riêng.

## ADR-004: Overlay HUD dùng Foreground Service + WindowManager, không dùng root
`TYPE_APPLICATION_OVERLAY` + quyền `SYSTEM_ALERT_WINDOW` do người dùng cấp thủ công (Android không cho xin quyền này qua runtime request thông thường, phải điều hướng sang `Settings.ACTION_MANAGE_OVERLAY_PERMISSION`).

## Data model cốt lõi (Phiên 1)

### `PerformanceProfile` (enum + config)
```kotlin
enum class ProfileType { LOW, BALANCED, PERFORMANCE }
data class ProfileConfig(
    val type: ProfileType,
    val limitRefreshRate: Boolean,
    val targetRefreshRateHz: Int?,   // null = không giới hạn
    val enableBatterySaverHint: Boolean,
    val requestHighRefreshRate: Boolean,
    val requestPerformanceGameMode: Boolean, // Android 12+ Game Mode API
    val addBatteryOptimizationWhitelistHint: Boolean,
)
```

### `DeviceInfo` (kết quả HardwareDetector)
CPU (ABI, số core), RAM tổng/khả dụng, Android version/API level, refresh rate hỗ trợ, độ phân giải, hỗ trợ Game Mode (`GameManager`, API 31+), hỗ trợ Thermal API (29+), trạng thái Shizuku.

## ADR-005: Thermal Guard chạy chung vòng đời với Automation Service
Không tạo thêm một Foreground Service riêng chỉ để theo dõi nhiệt độ — `ThermalGuard.run()`
được gọi trong cùng `CoroutineScope` của `GameDetectionService` (Phiên 3). Lý do: giảm số
Foreground Service thường trực (tốt cho pin + đúng tinh thần "specialUse" tối thiểu), và về
mặt sản phẩm, bảo vệ nhiệt chỉ thực sự cần thiết khi đang trong phiên chơi game do Automation
theo dõi. Nếu sau này cần Thermal Protection hoạt động độc lập (không cần bật Automation),
tách thành Service riêng khi đó.

## ADR-006: "Giảm độ phân giải" dùng `wm size` theo tỉ lệ tương đối, không có sẵn danh sách cứng
Low Mode giảm còn ~80% độ phân giải gốc (đọc từ `HardwareDetector`, làm chẵn số pixel) thay vì
đặt một độ phân giải cố định — vì thiết bị Android có vô số tỉ lệ khung hình khác nhau, một giá
trị cứng (vd. 720x1280) có thể làm méo hình trên máy tỉ lệ khác. Đổi lại toàn hệ thống (không chỉ
game) hiển thị nhỏ hơn khi Low Mode bật, kể cả UI của chính GAME SPACE — người dùng cần được báo
trước qua UI (`ProfileApplyResult.applied`) khi hành động này thực thi.

## ADR-007: Recovery dùng "dirty bit" ở cấp tiến trình (ProcessLifecycleOwner), không phải Activity
`Activity.onStop()`/`onDestroy()` cũng chạy khi xoay màn hình hoặc chuyển Activity nội bộ — dùng
nó để đánh dấu "sạch" sẽ gây false positive (tưởng nhầm là thoát sạch trong khi app vẫn sống).
`ProcessLifecycleOwner` chỉ báo `ON_STOP` khi KHÔNG còn Activity nào của app hiển thị (app thực sự
lùi về nền), đúng ngữ nghĩa cần cho Recovery. Nhược điểm: nếu app bị hệ thống kill trong lúc đang
ở nền (sau khi đã `ON_STOP`/đánh dấu sạch) thì không có gì để khôi phục — nhưng đó cũng chính xác
là hành vi mong muốn (không cần khôi phục nếu không có gì đang dở dang).

## ADR-008: Floating WebView tự vẽ toàn bộ UI, không nhúng WebView của app khác
`FloatingWebViewService` (Phiên 5) chỉ hiển thị nội dung web do chính GAME SPACE tải qua
`android.webkit.WebView` của riêng nó — xác nhận lại ADR-001. Không có "toggle Ghim trên cùng"
giả vì `TYPE_APPLICATION_OVERLAY` vốn dĩ luôn vẽ trên các app thường; thêm một công tắc không
có tác dụng thật sẽ đánh lừa người dùng. "Chống chạm nhầm" triển khai bằng một lớp scrim Compose
chặn sự kiện chạm ở tầng UI (không dùng cờ `FLAG_NOT_TOUCHABLE` của WindowManager, vì cờ đó làm
toàn bộ cửa sổ xuyên thấu mọi thao tác xuống app bên dưới — ngược với mục đích).

## ADR-009: Bảo vệ App Nhạc thay cho Music Hub
Theo yêu cầu người dùng ở Phiên 6, bỏ kế hoạch tự xây trình phát nhạc ("Music Hub") —
thay bằng bảo vệ các app nghe nhạc CÓ SẴN (Spotify, YouTube Music, Zing MP3...) khỏi bị
`OptimizerEngine` dọn RAM/buộc dừng, để nhạc nền không bị ngắt khi chơi game. Lý do đổi
hướng: người dùng đã có sẵn app nhạc quen dùng, không cần GAME SPACE làm thêm một trình
phát nhạc riêng — chỉ cần đảm bảo GAME SPACE không tự tay "giết" chúng.

Phát hiện app nhạc qua `ApplicationInfo.category == CATEGORY_AUDIO` (giống cách
`GameRepository` phát hiện game qua `CATEGORY_GAME`) thay vì hard-code danh sách package
name cụ thể — tránh rủi ro sai/lỗi thời khi tên package đổi hoặc không chắc chắn (vd.
không tự tin 100% vào package name chính xác của Zing MP3/NhacCuaTui). Người dùng luôn
tự thêm thủ công được nếu app không tự nhận diện.

**Giới hạn thật cần nói rõ:** đây chỉ ngăn chính GAME SPACE chủ động dọn/buộc dừng các
app trong danh sách — KHÔNG thể ngăn Android tự kill app nền khi hệ thống thật sự thiếu
RAM (đó là hành vi ở tầng OS, ngoài khả năng của app thứ ba, kể cả khi có Shizuku).

## Việc cần làm tiếp (chi tiết trong TASK_BACKLOG.md)
Xem file `TASK_BACKLOG.md` để lấy task kế tiếp theo đúng thứ tự ưu tiên.
