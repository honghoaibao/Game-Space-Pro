package com.gamespace.shizuku

import android.content.pm.PackageManager
import javax.inject.Inject
import javax.inject.Singleton
import rikka.shizuku.Shizuku

/**
 * "Capability Matrix" (mục 15. An toàn trong đặc tả): mọi tính năng cần quyền cao
 * phải hỏi qua đây trước, không bao giờ giả định Shizuku sẵn sàng.
 */
@Singleton
class CapabilityDetector @Inject constructor() {

    fun isShizukuAvailable(): Boolean = try {
        Shizuku.pingBinder()
    } catch (_: Throwable) {
        false
    }

    fun isShizukuPermissionGranted(): Boolean = try {
        isShizukuAvailable() && !Shizuku.isPreV11() &&
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    } catch (_: Throwable) {
        false
    }

    fun canRunShellCommands(): Boolean = isShizukuPermissionGranted()

    fun canForceStopOtherApps(): Boolean = isShizukuPermissionGranted()

    fun canOverrideSystemRefreshRate(): Boolean = isShizukuPermissionGranted()

    /** `wm size` / `wm density` — dùng cho Low Mode "Giảm độ phân giải" và Recovery khôi phục. */
    fun canOverrideResolution(): Boolean = isShizukuPermissionGranted()

    fun canRunFstrim(): Boolean = isShizukuPermissionGranted()

    /** Yêu cầu quyền Shizuku qua dialog hệ thống của Shizuku (chỉ gọi khi [isShizukuAvailable] = true). */
    fun requestPermission(requestCode: Int = REQUEST_CODE_SHIZUKU) {
        if (!isShizukuAvailable() || Shizuku.isPreV11()) return
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(requestCode)
        }
    }

    /**
     * Đăng ký lắng nghe kết quả sau khi người dùng bấm Allow/Deny trên dialog của Shizuku.
     * Bắt buộc để UI cập nhật ngay (không cần người dùng thoát app rồi mở lại) — gọi
     * [removePermissionResultListener] tương ứng khi ViewModel/Component bị huỷ.
     */
    fun addPermissionResultListener(listener: Shizuku.OnRequestPermissionResultListener) {
        Shizuku.addRequestPermissionResultListener(listener)
    }

    fun removePermissionResultListener(listener: Shizuku.OnRequestPermissionResultListener) {
        Shizuku.removeRequestPermissionResultListener(listener)
    }

    companion object {
        const val REQUEST_CODE_SHIZUKU = 1001
    }
}
