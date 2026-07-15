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

## Phiên 8 — Fix crash mở app + Thêm game qua danh sách chọn ✅ Hoàn thành

**Yêu cầu người dùng:** app crash ngay khi vừa mở; phần thêm game đổi từ bắt gõ package name
sang hiển thị danh sách ứng dụng đã cài để chọn.

- [x] **Fix crash cold-start**: `hardware/HardwareDetector.readRefreshRates()` gọi
  `WindowManager.getDefaultDisplay()` KHÔNG điều kiện trên mọi API level — hàm này chạy ngay ở
  `DashboardViewModel.init { refreshDeviceInfo() }`, tức ngay khi Dashboard (màn hình đầu tiên)
  hiển thị. Trên Application Context, `getDefaultDisplay()` vẫn nội bộ gọi `Context.getDisplay()`
  nên ném `UnsupportedOperationException` trên app targetSdk 30+ chạy Android 11+ (API 30+) —
  tức crash với gần như mọi máy hiện tại. Comment cũ trong code ghi sai là hàm này "an toàn cho
  mọi API level". Đã sửa: dùng `DisplayManager.getDisplay(Display.DEFAULT_DISPLAY)`, không có
  ràng buộc UI-context, an toàn từ Application Context ở mọi API level. `readScreenResolution()`
  không bị lỗi này vì đã tự nhánh theo `Build.VERSION.SDK_INT >= R` từ trước.
- [x] **Đổi "Thêm game" từ nhập tay sang chọn từ danh sách**: `GameLibraryScreen.AddGameDialog`
  trước đây chỉ có 1 ô nhập package name thủ công (dễ gõ sai). Đã thêm
  `GameRepository.queryAllLaunchableApps()` (quét app có `CATEGORY_LAUNCHER`, tức app có icon
  trên màn hình chính — không liệt kê ~300+ package hệ thống không có UI) +
  `GameLibraryViewModel.loadInstalledApps()` (nạp lười khi mở dialog, không quét lại nếu đã có
  dữ liệu trong phiên màn hình) + dialog mới hiển thị icon/tên/package dạng danh sách có ô tìm
  kiếm, ẩn app đã có sẵn trong Thư viện. Người dùng chạm vào app để thêm ngay (không còn nút
  "Thêm" cần gõ trước).

**Follow-up trong cùng phiên — người dùng báo "vẫn còn crash khi mở app" + yêu cầu thêm splash screen:**

Đã rà lại toàn bộ đường chạy cold-start (`GameSpaceApp.onCreate` → `MainActivity` →
`DashboardViewModel.init` → mọi Card hiển thị ngay ở Dashboard → `PermissionChecker` →
`ProfileRepository`/`LogManager`/Room/DataStore) lần thứ hai, không tìm thêm được lỗi *chắc chắn*
nào khác ngoài lỗi đã sửa ở trên — rất có thể người dùng đang test bản build cũ (chưa gộp fix
`HardwareDetector`) hoặc lỗi chỉ xảy ra trên 1 dòng máy/OEM cụ thể không tái hiện được bằng đọc
code. Vì không có logcat thật để xác nhận, đã bổ sung 2 lớp phòng thủ (không thay code logic cũ,
chỉ bọc thêm an toàn) để nếu còn lỗi tương tự (ở API/máy khác) thì app KHÔNG crash toàn bộ nữa —
và tách rõ để nếu build mới vẫn crash, biết chắc là ở một điểm khác chưa lộ ra:
- [x] `HardwareDetector.detect()` (đã đổi tên logic cũ thành `detectInternal()`) — bọc try/catch ở
  điểm vào duy nhất, có `fallbackDeviceInfo()` khi lỗi không lường trước.
- [x] `GameSpaceApp.applicationScope` — thêm `CoroutineExceptionHandler` (trước đây KHÔNG có, nên
  bất kỳ exception nào trong `recoveryManager.checkAndRecoverIfNeeded()`/`markSessionActive()`/
  `markCleanShutdown()` sẽ crash toàn app do `SupervisorJob` không tự nuốt lỗi).
- [x] **Thêm splash screen "xịn"**: dùng `androidx.core:core-splashscreen` (Android SplashScreen
  API chính thức của Google, có lớp compat chạy đồng nhất từ API 26+ chứ không chỉ 31+) — theme
  `Theme.GameSpace.Splash` (nền `#FF0B1830` + icon `logo_full.png`, `postSplashScreenTheme` tự
  chuyển về `Theme.GameSpace` sau khi splash đóng) + `MainActivity.installSplashScreen()` (gọi
  trước `super.onCreate()` theo đúng yêu cầu của thư viện) + hiệu ứng thoát tùy chỉnh (icon phóng
  nhẹ + toàn splash mờ dần bằng `AnimatorSet`, thay vì hiệu ứng cắt cứng mặc định).

**Nếu build mới từ fix này vẫn còn crash**: cần logcat thật (`adb logcat *:E` ngay lúc mở app,
hoặc Android Studio → App Quality Insights) để xác định chính xác exception/stack trace — đọc code
tĩnh không còn tìm thêm được ứng viên nào khác.

**Cập nhật — người dùng gửi logcat thật, xác định được nguyên nhân CHÍNH XÁC (khác 2 giả thuyết
trên, độc lập với build core-splashscreen bị lỗi import ở trên):**

- `DashboardScreen.kt:107` — `Image(painter = painterResource(R.mipmap.ic_launcher), ...)` trong
  `TopAppBar` của Dashboard. `R.mipmap.ic_launcher` phân giải ra `mipmap-anydpi-v26/ic_launcher.xml`
  (`<adaptive-icon>`), mà `painterResource()` của Compose CHỈ hỗ trợ `VectorDrawable` và ảnh raster
  (PNG/JPG/WEBP) — gặp `<adaptive-icon>` sẽ ném `java.lang.IllegalArgumentException: Only
  VectorDrawables and rasterized asset types are supported ex. PNG, JPG` (đây là bug Compose nổi
  tiếng, đã xác nhận qua log thật khớp 100%: `reason=java.lang.IllegalArgumentException`, stack
  trace tại đúng `DashboardScreen.kt:102-107`). Vì app có `minSdk 26` nên MỌI máy đều phân giải ra
  file adaptive-icon này — tức crash 100% các lần mở app trên máy thật, không phụ thuộc OEM/API cụ
  thể như 2 giả thuyết trước.
- [x] **Fix**: đổi sang `painterResource(R.drawable.logo_full)` (logo raster có sẵn, cũng đang dùng
  cho splash screen) — không cần thêm dependency (Accompanist DrawablePainter) hay tự convert
  Drawable→Bitmap thủ công.

## Phiên 9 — Redesign UI hoàn toàn (3 tab, bỏ emoji, responsive, tối ưu RAM) ✅ Hoàn thành

**Yêu cầu người dùng**: xây lại UI hoàn toàn mới — 100% icon (không emoji), thiết kế cao cấp kiểu
"siêu ứng dụng", điều hướng CHỈ 3 mục theo đúng thứ tự **Thời gian chơi → G-Home → Hồ sơ**, popup
nâng cấp UI, thẻ/cỡ chữ responsive theo tỉ lệ màn hình, tối ưu RAM tối đa, gỡ mọi phần không được
nhắc tới khỏi UI (để dành phát triển sau).

### Điều hướng mới
`MainActivity.kt` viết lại hoàn toàn: `NavigationBar` (Material3) 3 tab đúng thứ tự yêu cầu, icon
Filled/Outlined đổi theo trạng thái chọn (KHÔNG dùng emoji). `GameSpaceDestinations` chỉ còn
`PLAYTIME` / `G_HOME` / `PROFILE` (bỏ `DASHBOARD`/`LIBRARY`/`DIAGNOSTICS`/`PROTECTED_APPS`/
`PERFORMANCE_CENTER`). **G-Home là start destination** (không phải tab đầu trong danh sách) vì
người dùng mô tả rõ đây là "trang chủ của app".

**Các màn cũ (Dashboard, Thư viện Game dạng cũ, Diagnostics, Protected Apps, Performance Center)
KHÔNG bị xoá khỏi repo** — chỉ gỡ khỏi navigation graph nên không còn route nào trỏ tới. Giữ lại
đúng theo ý "những phần không nhắc tới sẽ phát triển sau". Các file này vẫn tự compile độc lập
(đã sửa sẵn 1 số lỗi trong đó — xem Phiên 8 — để không vỡ build dù không dùng tới).

### 3 màn hình mới
- **`ui/playtime/`** (`PlaytimeScreen` + `PlaytimeViewModel`): CHỈ đọc dữ liệu có sẵn từ
  `GameEntity.totalPlayTimeMillis`/`lastPlayedMillis` (cột này đã tồn tại từ trước, được
  `GameDetectionService.recordSession()` ghi nhận) — không xây lại cơ chế tracking mới.
- **`ui/ghome/`** (`GHomeScreen` + `GHomeViewModel`): trang chủ chọn game để mở ngay. **QUAN
  TRỌNG**: `GHomeViewModel` KHÔNG gọi `gameRepository.syncInstalledGames()` (khác
  `GameLibraryViewModel` cũ) — danh sách chỉ gồm game người dùng chủ động thêm qua nút "+", đúng
  yêu cầu "ban đầu không có game, phải tự chọn". Dialog "+" dùng lại pattern chọn app đã cài (từ
  Phiên 8) qua `GameRepository.queryAllLaunchableApps()`. Có mục "Chơi gần đây" (top 5 theo
  `lastPlayedMillis`). Chạm vào game = mở luôn qua `GameRepository.getLaunchIntent()`.
- **`ui/profile/`** (`ProfileScreen` + `ProfileScreenViewModel`): chọn Performance Profile
  (Low/Balanced/Performance — logic dùng lại `ProfileEngine`/`ProfileRepository` có sẵn từ Phiên
  1) + hiển thị Device Info (`HardwareDetector`) + **quyết định thiết kế cần lưu ý**: công tắc "Tự
  động tối ưu & theo dõi thời gian chơi" (bật/tắt `GameDetectionService`) đặt Ở ĐÂY thay vì có UI
  riêng — vì đây là nguồn dữ liệu DUY NHẤT cho tab "Thời gian chơi", và người dùng không nhắc tới
  màn "Automation" riêng trong yêu cầu. Nếu không bật công tắc này, tab "Thời gian chơi" sẽ mãi
  trống.

### Bỏ emoji 100%
`ProfileType` bỏ hẳn field `emoji` (`enum class ProfileType(val displayName: String)`), thêm
`ProfileType.brandIcon(): ImageVector` trong `Components.kt` (BatterySaver/SwapHoriz/Bolt) thay
thế. Sửa 6 điểm dùng `.emoji` trên toàn repo (kể cả trong các file "chết" ở trên — bắt buộc phải
sửa vì Kotlin compile toàn bộ module dù không gọi tới). `HudContent.kt` (floating popup) cũng bỏ
emoji "⚡" (sạc pin) → đổi thành `Icon(Icons.Filled.Bolt)` thật.

### Responsive (thẻ/cỡ chữ theo tỉ lệ màn hình)
`ui/theme/Responsive.kt` (mới): `GsSpacing` + `rememberGsScale()` tính hệ số từ
`LocalConfiguration.screenWidthDp` so với baseline 360dp, clamp `[0.85, 1.3]`. `Type.kt` thêm
`Typography.scaledBy(scale)` (scale toàn bộ cỡ chữ). `Theme.kt` tính `scale` 1 lần ở gốc
`GameSpaceTheme`, cung cấp qua `CompositionLocalProvider(LocalGsSpacing provides ...)` — mọi
Card/Spacer/Icon trong 3 màn mới dùng `LocalGsSpacing.current.*` thay vì hằng số `dp` cố định.
`GHomeScreen` dùng thêm `GridCells.Adaptive` (số cột tự đổi theo bề rộng màn hình).

### Tối ưu RAM
`ui/common/AppIconImage.kt` (mới, thay bản cũ nằm riêng trong `GameLibraryScreen.kt`): decode icon
app theo ĐÚNG kích thước hiển thị (vd 40dp×density) thay vì intrinsic size gốc của adaptive icon
(có thể tới ~747KB/icon ở 432×432 ARGB_8888 dù chỉ hiển thị nhỏ), + cache LRU bounded theo byte
thực tế (6MB) để cuộn danh sách không decode lại icon đã từng hiển thị.

### Popup (Floating HUD)
Giữ nguyên hệ thống Overlay/HUD hiện có (không nằm trong 3 tab chính vì đây là cửa sổ nổi trên
game, không phải màn trong app) — chỉ bỏ emoji như trên. **Chưa redesign sâu UI của HUD** (ngoài
việc bỏ emoji) do giới hạn thời gian — nếu người dùng muốn "nâng cấp UI tối đa" hơn nữa cho riêng
phần popup, đó là việc còn lại.

### Giới hạn đã biết / rủi ro cần build thật để xác nhận
- Môi trường này KHÔNG có Android SDK để compile thật (như mọi phiên trước) — đã tự rà cú pháp,
  cân bằng ngoặc, và tra cứu web để xác nhận các API Compose ít gặp/dễ nhầm trước khi dùng (vd.
  `TextUnit` không có constructor public rõ ràng nên dùng `.sp`/`.em` thay vì đoán; `AlertDialog`
  có `containerColor`/`shape`; `Card(onClick=)` cần `@OptIn(ExperimentalMaterial3Api)`).
- Tên icon Material đã cố gắng xác minh với những cái ít phổ biến (`Balance` → đổi sang
  `SwapHoriz` đã dùng sẵn trong `HudContent.kt`) nhưng KHÔNG thể xác minh 100% từng icon một do
  giới hạn số lần tra cứu — nếu build báo `Unresolved reference` ở icon nào trong
  `ui/theme/Components.kt`, `ui/ghome/GHomeScreen.kt`, `ui/playtime/PlaytimeScreen.kt`, hoặc
  `ui/profile/ProfileScreen.kt`, đó là icon cần đổi tên/thay icon khác — gửi lại lỗi build là sửa
  được ngay.

## Phiên 10 — Redesign sâu Floating HUD + tối ưu dung lượng app ✅ Hoàn thành

### Floating HUD (popup) — redesign sâu
`overlay/HudContent.kt` viết lại hoàn toàn (không chỉ bỏ emoji như Phiên 8):
- **Bubble thu gọn**: thêm badge cảnh báo đỏ nhỏ ở góc (máy nóng từ mức MODERATE trở lên, hoặc pin
  ≤15% mà không sạc) — thấy ngay tình trạng máy mà KHÔNG cần mở rộng panel. Số FPS dùng
  `GsStatNumberStyle` (kiểu số HUD đã có sẵn từ Phiên 9, chưa từng được dùng tới).
- **Panel mở rộng**: đổi từ list `MetricRow` phẳng (label: value) sang lưới 3 "stat tile"
  (icon + số lớn + nhãn) cho FPS/RAM/CPU, có `HorizontalDivider` tách header/thân/quick-tools.
  Nhiệt độ và Pin đổi thành "chip" trạng thái nền màu theo mức độ thay vì text thường.
- **Tô màu theo ngưỡng** (mới hoàn toàn, trước đây mọi số liệu cùng 1 màu): RAM/CPU ≥85% đỏ
  (`GsError`), ≥65% vàng (`GsWarning`), còn lại bình thường. Thêm field `HudMetrics.thermalStatus`
  (giá trị thô `PowerManager.THERMAL_STATUS_*`) song song với `thermalLabel` (chuỗi hiển thị có
  sẵn) — để tô màu theo mức độ nóng máy mà KHÔNG phải so khớp chuỗi tiếng Việt (dễ vỡ nếu
  `ThermalMonitor.label()` đổi câu chữ sau này). `HudMetricsCollector` set field này từ đúng 1 lần
  gọi `thermalMonitor.currentStatus()` đã có sẵn (không tốn thêm system call).
- **Chuyển cảnh mượt**: `Crossfade` + `Modifier.animateContentSize()` khi thu gọn↔mở rộng thay vì
  cắt cứng — animation NGẮN có điểm dừng, không lặp vô hạn, không dùng `blur` (tốn GPU khi vẽ đè
  lên 1 game khác đang chạy — cân nhắc hiệu năng vì đây là overlay TYPE_APPLICATION_OVERLAY).

### Tối ưu dung lượng app
- **`logo_full.png`: 1254×1254, 1.37MB → WebP 512×512 q90, 17KB (giảm ~98.7%)** — đây là file lớn
  NHẤT trong toàn bộ `res/` (tổng `res/` từ ~1.6MB xuống 276KB), dùng cho splash icon + logo
  TopAppBar, trước đó giữ nguyên độ phân giải gốc dù chỉ hiển thị tối đa ~240dp. Vì cùng tên
  resource (`logo_full`, chỉ đổi đuôi .png→.webp) nên KHÔNG cần sửa bất kỳ dòng code/XML nào —
  `R.drawable.logo_full`/`@drawable/logo_full` tự trỏ đúng file mới. Đã xem lại bằng mắt, không
  thấy artifact nén đáng chú ý (logo màu phẳng/gradient, không phải ảnh chụp nên nén lossy WebP
  q90 gần như không mất chi tiết).
- **`app/build.gradle.kts`**: thêm `isShrinkResources = true` (đi kèm `isMinifyEnabled` đã có sẵn
  từ trước — trước đây chỉ tối ưu code, chưa tối ưu resource) + `resConfigs("vi")` (loại string đã
  dịch cho các ngôn ngữ khác mà thư viện AndroidX/Google mang theo, app chỉ dùng tiếng Việt). Đã
  kiểm tra không có `getIdentifier()` (tra resource động) nên bật `shrinkResources` an toàn, không
  cần file `keep.xml` bổ sung.

### Việc đã làm ở phiên trước — CHƯA làm (nay đã làm, xem Phiên 11)
- ~~`material-icons-extended`: chưa migrate vì rủi ro cao~~ → xem Phiên 11.

## Phiên 11 — Gỡ bỏ material-icons-extended (lever tối ưu dung lượng lớn nhất) ✅ Hoàn thành

**Cách làm — KHÔNG đoán path data bằng tay** (đúng nguyên tắc never hallucinate API/asset): dùng
`git sparse-checkout` (blobless clone, chỉ tải đúng file cần) lấy **path data XML gốc** từ
`google/material-design-icons` trên GitHub — đây là chính nguồn dữ liệu mà `material-icons-extended`
dùng để sinh code, nên path data đảm bảo khớp 100% hình dạng icon gốc, không có rủi ro "vẽ tay
sai hình". Quy trình:

1. Quét toàn repo (`grep -rhoE "Icons\.(Filled|Outlined)\.[A-Za-z]+"`) → xác định đúng 41 icon
   Filled + 4 icon Outlined (AccessTime/Home/Person/Star) đang thực sự được dùng trong toàn app.
2. `git clone --filter=blob:none --sparse` repo `google/material-design-icons`, tra cứu đúng 45
   đường dẫn XML (`android/<category>/<icon>/materialicons(outlined)/black/res/drawable/*.xml`)
   qua `git ls-tree` (chỉ đọc metadata, không tải thừa), rồi sparse-checkout đúng 45 file đó.
3. Viết script Python parse `pathData` thật từ mỗi XML, sinh 2 file Kotlin:
   - `ui/icons/filled/GsFilledIcons.kt` (41 icon)
   - `ui/icons/outlined/GsOutlinedIcons.kt` (4 icon)
   Mỗi icon là 1 `val Icons.Filled.TenIcon: ImageVector` (extension property — Kotlin cho phép
   khai báo ở package bất kỳ, không cần sở hữu package gốc của `Icons.Filled`), build qua
   `ImageVector.Builder(...).addPath(pathData = addPathNodes("<pathData thật>"), fill =
   SolidColor(Color.Black)).build()` — API đã tra cứu xác nhận (`ImageVector.Builder.addPath`,
   `addPathNodes`), có cache qua `private var _ten: ImageVector?` giống hệt cách file icon thật
   của Google tự sinh (tránh build lại ImageVector mỗi lần recompose — quan trọng vì HUD popup
   vẽ lại icon liên tục).
4. Đổi TOÀN BỘ import trên 10 file dùng icon: `androidx.compose.material.icons.filled.X` →
   `com.gamespace.ui.icons.filled.X` (và tương tự `outlined`) — **chỉ đổi dòng import, không đổi
   bất kỳ dòng code nào khác** (`Icons.Filled.X` ở nơi gọi giữ nguyên 100% nhờ cơ chế extension
   property theo import, không phải theo package sở hữu).
5. Gỡ dòng `implementation("androidx.compose.material:material-icons-extended")` khỏi
   `app/build.gradle.kts` — chỉ còn `material-icons-core` (đã tự có sẵn qua `material3`, chứa
   `Icons`/`Icons.Filled`/`Icons.Outlined` + vài trăm icon cơ bản, không xung đột với 45 icon tự
   build ở trên vì Kotlin resolve theo import, không phải theo tên).

**Kết quả**: không còn ~2000+ icon thừa trong dependency graph — chỉ còn đúng 45 icon thực sự
dùng, mỗi icon chỉ vài trăm byte path data. Đã kiểm tra: không còn import nào trỏ tới
`androidx.compose.material.icons.filled.*`/`.outlined.*` trong toàn repo (grep xác nhận 0 kết
quả), toàn bộ 11 file bị ảnh hưởng cân bằng ngoặc `{}`/`()` bình thường.

**Nếu cần thêm icon mới sau này**: KHÔNG tự đoán/vẽ tay pathData — lặp lại đúng quy trình 5 bước
trên (sparse-checkout đúng icon cần từ `google/material-design-icons`, dùng path data thật).

**Lỗi build sau khi áp dụng (đã sửa)**: `MainActivity.kt` dùng `import ... as AccessTimeFilled`
(cần alias vì Filled/Outlined cùng tên "AccessTime"/"Home"/"Person" trong 1 file) rồi gọi thẳng
`AccessTimeFilled` KHÔNG kèm receiver — nhưng đây là **extension property** trên `Icons.Filled`,
alias qua `as` chỉ đổi tên gọi, KHÔNG biến nó thành property độc lập không cần receiver. Phải viết
`Icons.Filled.AccessTimeFilled` (giữ nguyên receiver, chỉ đổi tên member phía sau dấu chấm). Đã
sửa cả 3 cặp (AccessTime/Home/Person) + thêm `import androidx.compose.material.icons.Icons` còn
thiếu. Đây là lỗi CHỈ xảy ra ở file có alias — 39 icon còn lại dùng import thường
(`Icons.Filled.TenIcon` không alias) không bị ảnh hưởng.

## Ghi chú xuyên suốt
- Mỗi phiên: cập nhật `PROJECT_PLAN.md` bảng trạng thái + thêm ADR mới (nếu có quyết định kiến trúc) vào `ARCHITECTURE.md`.
- Luôn build thử bằng cách kiểm tra cú pháp/logic thủ công (môi trường hiện tại không có Android SDK để compile — người dùng build thật trong Android Studio hoặc CI).
