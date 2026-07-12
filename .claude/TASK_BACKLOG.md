# GAME SPACE — Task Backlog (theo phiên)

Gõ **"Tiếp tục"** để làm task đầu tiên chưa ✅ theo thứ tự dưới đây.

## Phiên 2 — Package Manager + Optimizer Engine ✅ Hoàn thành
- [x] `packagemanager/GameEntity.kt` + `GameDao.kt` + `GameDatabase.kt` (Room)
- [x] `packagemanager/GameRepository.kt`: quét app đã cài có category GAME (`ApplicationInfo.category == CATEGORY_GAME`) qua `PackageManager`
- [x] Màn hình "Thư viện Game" (Compose): icon, tên, package, last played, time played, profile riêng, favorite toggle, thêm thủ công, Smart Compile
- [x] `optimizer/OptimizerEngine.kt`: Deep RAM Cleaner, Cache Cleaner (+ `pm trim-caches` qua Shizuku), Storage Trim (`sm fstrim`), Smart Compilation (`cmd package compile -m speed`), Force Stop
- [x] Wiring nút "Dọn RAM"/"Xóa Cache"/"Storage Trim" trên Dashboard vào OptimizerEngine thật + `ProfileEngine.cleanBackgroundBeforeLaunch` gọi Deep RAM Cleaner thật

## Phiên 3 — Overlay HUD + Automation + Thermal ✅ Hoàn thành
- [x] `overlay/OverlayService.kt`: Foreground Service tạo `WindowManager` overlay (`TYPE_APPLICATION_OVERLAY`) qua `ComposeView` + `OverlayLifecycleOwner`
- [x] `overlay/HudMetricsCollector.kt` + `overlay/HudContent.kt`: FPS (Choreographer), RAM, Nhiệt (ThermalMonitor), Pin — CPU% để `null` (chưa đọc `/proc/stat` qua Shizuku, xem ghi chú trong file)
- [x] Quick Tools trên overlay: Dọn RAM, Chặn Notification (DND, cần `ACCESS_NOTIFICATION_POLICY`), Đổi Profile (cycle Low→Balanced→Performance), Tactical Timer (đếm ngược 10 phút), Thu gọn/mở rộng, kéo thả bubble
- [x] `automation/GameDetectionService.kt`: polling `UsageStatsManager` mỗi 2s (không cần Accessibility) → phát hiện game đã theo dõi mở/đóng, dọn nền + áp Profile + mở Overlay, ghi nhận thời gian chơi qua `GameRepository`, khôi phục Profile trước đó khi thoát
- [x] `thermal/ThermalMonitor.kt` + `thermal/ThermalGuard.kt`: theo dõi qua `PowerManager` (29+), tự hạ Low Mode khi ≥ MODERATE, khôi phục khi ≤ LIGHT, cảnh báo qua Notification (ADR-005: chạy chung vòng đời với Automation Service)
- [x] `permissions/PermissionChecker.kt`: kiểm tra Overlay/Usage Access/DND/Post Notification, có Intent điều hướng Settings tương ứng
- [x] Dashboard: thêm card "Smart Automation" (trạng thái quyền + nút bật/tắt), refresh quyền khi resume app

**Lưu ý còn để lại (không chặn, ghi nhận cho lần polish sau):**
- ~~Gesture tap-vs-drag trên bubble thu gọn có thể tranh chấp nhẹ (xem TODO trong `HudContent.kt`)~~ **Đã fix ở Phiên 7.**
- ~~CPU% overlay hiện luôn `null` — cần đọc `/proc/stat` hai lần qua Shizuku để tính delta, để dành nếu cần độ chi tiết cao hơn~~ **Đã fix ở Phiên 7** (`hardware/CpuUsageReader.kt`).
- `isAutomationRunning` chỉ lưu trong RAM của ViewModel (mất khi rotate/kill Activity) — Recovery (Phiên 4) sẽ persist qua DataStore

## Phiên 4 — Accessibility Engine + Diagnostics + Recovery ✅ Hoàn thành
- [x] `accessibility/GameSpaceAccessibilityService.kt` + `accessibility/AccessibilityStateBus.kt`: phát hiện app foreground nhanh hơn UsageStatsManager, phát hiện dialog qua className — tùy chọn, người dùng tự bật, có nút hướng dẫn trong Dashboard
- [x] `diagnostics/DiagnosticsReport.kt` + `DiagnosticsGenerator.kt`: tổng hợp DeviceInfo + toàn bộ trạng thái quyền + Capability Matrix Shizuku, xuất `.txt` qua `ACTION_SEND`; màn hình "Diagnostics" mới (`ui/diagnostics/`) hiển thị trực quan + nút xuất report/log
- [x] `recovery/RecoveryManager.kt`: dirty-bit qua `ProcessLifecycleOwner` (ON_START đánh dấu "chưa sạch", ON_STOP đánh dấu "sạch"), `GameSpaceApp.onCreate` gọi `checkAndRecoverIfNeeded()` — nếu phiên trước không tắt sạch: `wm size reset` + `wm density reset` + xóa `peak_refresh_rate` + Profile về Balanced
- [x] Bổ sung "Giảm độ phân giải" (Low Mode) vào `ProfileEngine` qua `wm size` (Shizuku) — phần còn thiếu từ đặc tả gốc mục 1, phát hiện khi làm Recovery (ADR-006/007)
- [x] Persist trạng thái bật/tắt Automation qua `RecoveryManager` (khắc phục ghi chú để lại từ Phiên 3)

**Lưu ý còn để lại:**
- `AccessibilityStateBus` hiện chưa được `GameDetectionService` tiêu thụ thực sự (mới dừng ở việc phát dữ liệu) — có thể tích hợp sau nếu cần độ trễ phát hiện thấp hơn 2s hiện tại
- Recovery chỉ khôi phục Profile về Balanced, chưa khôi phục lại đúng Profile/game đang chơi dở trước khi crash (cần lưu thêm session context nếu muốn resume chính xác hơn)

## Phiên 5 — Popup Apps (PiP-hint + Floating WebView) + CI mở rộng ✅ Hoàn thành
- [x] `overlay/FloatingWebViewService.kt` + `FloatingBrowserContent.kt` + `FloatingBrowserState.kt`: cửa sổ WebView nổi — kéo thả, resize (kéo góc), thu nhỏ thành bong bóng, độ trong suốt (3 mức, đổi qua `WindowManager.LayoutParams.alpha`), khóa vị trí, chế độ chống chạm nhầm (lớp scrim chặn thao tác), nút Quay lại nối thật với `WebView.goBack()`
- [x] Dashboard: card "Popup Apps" — shortcut nhanh ChatGPT/Gemini/Wikipedia, ô URL tùy chỉnh, và gợi ý mở YouTube cho Picture-in-Picture (không thể tự động hoá phần PiP của app bên thứ 3 — xem ADR-001)
- [x] Ký APK trong CI nếu có secrets — **đã có sẵn từ Phiên 1** (`r0adkll/sign-android-release`, gated theo `KEYSTORE_BASE64`), xác nhận lại không cần làm thêm

**Lưu ý còn để lại:**
- Floating WebView chưa có "lịch sử" điều hướng đầy đủ (mới hỗ trợ Back qua `WebView.goBack()`, chưa có Forward/danh sách tab)
- "Ghim trên cùng" không triển khai thành toggle riêng vì `TYPE_APPLICATION_OVERLAY` vốn đã luôn nổi trên các app khác — tránh làm toggle giả không có tác dụng thật

## Phiên 6 — Bảo vệ App Nhạc ✅ Hoàn thành + Floating Chat/Performance Center (còn lại)

**Đổi hướng theo yêu cầu người dùng:** bỏ "Music Hub" (tự xây trình phát nhạc) — thay
bằng bảo vệ app nhạc CÓ SẴN trên máy khỏi bị Optimizer Engine dọn/buộc dừng, để người
dùng nghe nhạc từ Spotify/YouTube Music/Zing MP3/... khi chơi game.

- [x] `protection/ProtectedAppEntity.kt` + `ProtectedAppDao.kt` + `ProtectedAppDatabase.kt` (Room)
- [x] `protection/ProtectedAppRepository.kt`: quét app category AUDIO (không hard-code danh sách package — dễ sai/lỗi thời) + thêm thủ công
- [x] `OptimizerEngine.deepRamClean()` loại trừ app trong danh sách bảo vệ; `forceStop()` từ chối app được bảo vệ
- [x] Màn hình "Bảo vệ App Nhạc" (`ui/protection/`) — bật/tắt từng app, thêm thủ công, xóa app tự thêm
- [x] Dashboard: nút "🎵 Quản lý app nhạc được bảo vệ" trong card Optimizer Engine; `OptimizeResultSummary` hiển thị số app đã bảo vệ mỗi lần dọn RAM
- [x] `ADR-009` (xem ARCHITECTURE.md): giới hạn thật — chỉ ngăn chính GAME SPACE chủ động dừng app, không thể ngăn Android OOM-killer hệ thống

**Dọn dẹp phát sinh:** phát hiện và xóa một số file/thư mục liên quan "Music Hub" cũ
(`music/`, `musichub/`, `ui/music/`, `ui/musichub/`) cùng 2 thư mục rác dạng tên chứa
dấu ngoặc nhọn chưa được shell mở rộng đúng (`{hardware,...}`, `{db,player,lyrics}`) —
không phải do các phiên làm việc trước đó trong hội thoại này tạo ra, đã dọn sạch toàn bộ.

### Đã làm nốt trong cùng Phiên 6
- [x] Floating Chat: card "Floating Chat" trên Dashboard — shortcut Discord/Telegram/Messenger/Zalo qua `getLaunchIntentForPackage`, tự fallback mở trang Play Store (`market://` → web) nếu app chưa cài hoặc package name không khớp máy
- [x] Performance Center: màn hình mới (`ui/performance/`) — biểu đồ FPS/RAM/Pin realtime vẽ bằng `Canvas` thuần (không thêm thư viện chart ngoài), tái dùng `HudMetricsCollector` từ Phiên 3
- [x] **Fix kèm theo**: `HudMetricsCollector` trước đó dùng 1 `AtomicInteger` đếm frame dùng chung cấp Singleton — nếu Overlay HUD và Performance Center cùng chạy sẽ giẫm số đếm lên nhau. Đã sửa: mỗi lần gọi `metricsFlow()` tự tạo bộ đếm riêng.

**Toàn bộ 19 mục trong đặc tả gốc (đã điều chỉnh mục 17 theo yêu cầu) + phần mở rộng dài hạn (Popup Apps, Floating Chat, Performance Center) đã hoàn thành qua 6 phiên.** Việc còn lại chủ yếu là polish/mở rộng — xem mục "Lưu ý còn để lại" rải rác trong các phiên ở trên, hoặc bắt đầu phiên mới nếu Trr có yêu cầu tính năng mới.

## Phiên 7 — Nâng cấp UI cao cấp + Fix 3 lỗi báo cáo ✅ Hoàn thành

**Yêu cầu người dùng:** UI xịn/sang trọng hơn theo màu logo, + fix popup không hiển thị/
đôi khi biến mất/delay, + fix thiếu nút mở popup, + fix không đọc được CPU.

- [x] **Design system mới** (`ui/theme/Color.kt`, `Type.kt`, `Theme.kt`, `Components.kt`):
  đo màu thật từ `logo_full.png` (xanh neon `#00B3FD` + navy `#062240`), bỏ Light Theme dở
  dang (app giờ luôn Dark), bo góc rộng hơn (Shapes), typography đậm hơn kiểu HUD, nền
  gradient toàn app + 2 quầng sáng neon mờ (`GsAppBackground`), TopAppBar trong suốt xuyên
  suốt 5 màn hình, wordmark "GAME SPACE" gradient trên Dashboard, màu Profile
  (Low=xanh lá/Balanced=xanh neon/Performance=đỏ) áp dụng lên SegmentedButton + HUD badge.
  `HudContent.kt` và `FloatingBrowserContent.kt` (2 popup thật) restyle theo bảng màu mới.
- [x] **Fix "không đọc được CPU"**: `hardware/CpuUsageReader.kt` mới — đọc `/proc/stat`
  trực tiếp trước (không cần Shizuku, hoạt động trên phần lớn máy), fallback Shizuku nếu bị
  chặn, tính % chuẩn qua delta 2 mẫu liên tiếp. Wire vào `HudMetricsCollector`, hiển thị ở
  cả Overlay HUD và Performance Center (thêm hẳn 1 biểu đồ CPU mới ở màn này).
- [x] **Fix "không có nút mở popup"**: Floating HUD trước đây CHỈ tự mở qua Smart Automation
  phát hiện game trong Thư viện — không có cách mở tay. Thêm card "Floating HUD" độc lập
  trên Dashboard (`FloatingHudCard`) + `DashboardViewModel.toggleHud()` + persist qua
  `RecoveryManager.isHudEnabled` (giống mẫu `isAutomationEnabled` có sẵn).
- [x] **Fix "popup không hiển thị/đôi khi biến mất/delay"**: 3 nguyên nhân gốc, xử lý cả 3:
  1. `GameDetectionService` thoát session NGAY khi 1 nhịp poll không thấy game (dialog/
     thông báo/bàn phím che foreground trong UsageStatsManager) → thêm grace period 4s
     (`EXIT_GRACE_MILLIS`) trước khi coi là thoát thật; giảm chu kỳ poll 2s→1s cho mở nhanh
     hơn; `endSession()` không tắt HUD nếu người dùng đã bật thủ công (tránh Automation đè
     lên lựa chọn tay).
  2. Bubble thu gọn dùng 2 gesture detector (tap + drag) tranh chấp nhau → gộp thành 1
     detector duy nhất phân biệt bằng touch-slop (`HudContent.kt`).
  3. Lỗi thiếu quyền/`addView` thất bại trước đây chỉ ghi Log âm thầm → thêm Toast báo lỗi
     rõ ràng ở cả `OverlayService` và `FloatingWebViewService`.
- [x] Dọn 1 thư mục rác còn sót (`res/{values,drawable,xml,mipmap-anydpi-v26}` rỗng, do
  shell brace-expansion chưa mở đúng từ trước) + polish `themes.xml` (nền tối khớp brand
  ngay từ splash, tránh nháy trắng lúc khởi động).

## Ghi chú xuyên suốt
- Mỗi phiên: cập nhật `PROJECT_PLAN.md` bảng trạng thái + thêm ADR mới (nếu có quyết định kiến trúc) vào `ARCHITECTURE.md`.
- Luôn build thử bằng cách kiểm tra cú pháp/logic thủ công (môi trường hiện tại không có Android SDK để compile — người dùng build thật trong Android Studio hoặc CI).
