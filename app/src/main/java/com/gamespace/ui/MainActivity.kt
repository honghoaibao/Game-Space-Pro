package com.gamespace.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.os.Bundle
import android.view.View
import android.view.animation.AnticipateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.core.splashscreen.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gamespace.ui.dashboard.DashboardScreen
import com.gamespace.ui.diagnostics.DiagnosticsScreen
import com.gamespace.ui.library.GameLibraryScreen
import com.gamespace.ui.performance.PerformanceCenterScreen
import com.gamespace.ui.protection.ProtectedAppsScreen
import com.gamespace.ui.theme.GameSpaceTheme
import com.gamespace.ui.theme.GsAppBackground
import dagger.hilt.android.AndroidEntryPoint

object GameSpaceDestinations {
    const val DASHBOARD = "dashboard"
    const val LIBRARY = "library"
    const val DIAGNOSTICS = "diagnostics"
    const val PROTECTED_APPS = "protected_apps"
    const val PERFORMANCE_CENTER = "performance_center"
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // installSplashScreen() BẮT BUỘC gọi trước super.onCreate() (yêu cầu của
        // androidx.core.splashscreen) — giữ nguyên splash hệ thống (nền + icon app) cho tới khi
        // Compose vẽ xong khung hình đầu tiên, tránh khoảng trắng/đen trống giữa lúc chạm icon
        // và lúc Dashboard thật sự hiện ra.
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Hiệu ứng thoát splash tùy chỉnh (mặc định chỉ biến mất đột ngột): icon phóng nhẹ lên
        // rồi toàn bộ splash mờ dần — cảm giác "chuyển cảnh" cao cấp hơn thay vì cắt cứng sang
        // Dashboard.
        splashScreen.setOnExitAnimationListener { provider ->
            val zoomIcon = ObjectAnimator.ofPropertyValuesHolder(
                provider.iconView,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.15f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.15f),
            ).apply {
                interpolator = OvershootInterpolator()
                duration = 260L
            }
            val fadeOutSplash = ObjectAnimator.ofFloat(provider.view, View.ALPHA, 1f, 0f).apply {
                interpolator = AnticipateInterpolator()
                duration = 220L
                startDelay = 200L
            }
            AnimatorSet().apply {
                playTogether(zoomIcon, fadeOutSplash)
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) = provider.remove()
                })
                start()
            }
        }

        setContent {
            GameSpaceTheme {
                // Nền gradient thương hiệu dùng chung cho mọi màn hình (thay Surface phẳng
                // mặc định) — mỗi Scaffold bên trong dùng containerColor trong suốt để lộ nền này.
                GsAppBackground {
                    GameSpaceNavHost()
                }
            }
        }
    }
}

@Composable
private fun GameSpaceNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = GameSpaceDestinations.DASHBOARD) {
        composable(GameSpaceDestinations.DASHBOARD) {
            DashboardScreen(
                onOpenLibrary = { navController.navigate(GameSpaceDestinations.LIBRARY) },
                onOpenDiagnostics = { navController.navigate(GameSpaceDestinations.DIAGNOSTICS) },
                onOpenProtectedApps = { navController.navigate(GameSpaceDestinations.PROTECTED_APPS) },
                onOpenPerformanceCenter = { navController.navigate(GameSpaceDestinations.PERFORMANCE_CENTER) },
            )
        }
        composable(GameSpaceDestinations.LIBRARY) {
            GameLibraryScreen()
        }
        composable(GameSpaceDestinations.DIAGNOSTICS) {
            DiagnosticsScreen()
        }
        composable(GameSpaceDestinations.PROTECTED_APPS) {
            ProtectedAppsScreen()
        }
        composable(GameSpaceDestinations.PERFORMANCE_CENTER) {
            PerformanceCenterScreen()
        }
    }
}
