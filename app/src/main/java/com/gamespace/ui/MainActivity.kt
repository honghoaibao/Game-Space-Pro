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
import androidx.compose.foundation.layout.padding
import com.gamespace.ui.icons.filled.AccessTime as AccessTimeFilled
import com.gamespace.ui.icons.filled.Home as HomeFilled
import com.gamespace.ui.icons.filled.Person as PersonFilled
import com.gamespace.ui.icons.outlined.AccessTime as AccessTimeOutlined
import com.gamespace.ui.icons.outlined.Home as HomeOutlined
import com.gamespace.ui.icons.outlined.Person as PersonOutlined
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gamespace.ui.ghome.GHomeScreen
import com.gamespace.ui.playtime.PlaytimeScreen
import com.gamespace.ui.profile.ProfileScreen
import com.gamespace.ui.theme.GameSpaceTheme
import com.gamespace.ui.theme.GsAppBackground
import com.gamespace.ui.theme.GsPrimary
import com.gamespace.ui.theme.GsSurfaceElevated
import dagger.hilt.android.AndroidEntryPoint

/**
 * Điều hướng chính — CHỈ 3 tab theo đúng yêu cầu redesign (Thời gian chơi / G-Home / Hồ sơ).
 * Các màn cũ (Thư viện Game, Diagnostics, Protected Apps, Performance Center, Dashboard cũ) đã
 * gỡ khỏi UI — file Compose của chúng vẫn còn trong repo (không xoá) để dùng lại sau này, chỉ
 * không còn route nào trỏ tới nữa. Xem ghi chú trong TASK_BACKLOG.md.
 */
object GameSpaceDestinations {
    const val PLAYTIME = "playtime"
    const val G_HOME = "g_home"
    const val PROFILE = "profile"
}

private data class GsNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

// Thứ tự đúng theo yêu cầu: Thời gian chơi -> G-Home -> Hồ sơ. 100% icon, không emoji.
private val gsNavItems = listOf(
    GsNavItem(GameSpaceDestinations.PLAYTIME, "Thời gian chơi", AccessTimeFilled, AccessTimeOutlined),
    GsNavItem(GameSpaceDestinations.G_HOME, "G-Home", HomeFilled, HomeOutlined),
    GsNavItem(GameSpaceDestinations.PROFILE, "Hồ sơ", PersonFilled, PersonOutlined),
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // installSplashScreen() BẮT BUỘC gọi trước super.onCreate() (yêu cầu của
        // androidx.core.splashscreen) — giữ nguyên splash hệ thống (nền + icon app) cho tới khi
        // Compose vẽ xong khung hình đầu tiên, tránh khoảng trắng/đen trống giữa lúc chạm icon
        // và lúc G-Home thật sự hiện ra.
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Hiệu ứng thoát splash tùy chỉnh (mặc định chỉ biến mất đột ngột): icon phóng nhẹ lên
        // rồi toàn bộ splash mờ dần — cảm giác "chuyển cảnh" cao cấp hơn thay vì cắt cứng.
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
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            NavigationBar(containerColor = GsSurfaceElevated) {
                gsNavItems.forEach { item ->
                    val selected = currentRoute == item.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                // Pattern chuẩn cho bottom nav: giữ lại state của các tab đã
                                // ghé qua (danh sách cuộn, dialog...) thay vì tạo lại từ đầu
                                // mỗi lần chuyển tab.
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label,
                            )
                        },
                        label = { Text(item.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GsPrimary,
                            indicatorColor = GsPrimary.copy(alpha = 0.16f),
                        ),
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = GameSpaceDestinations.G_HOME, // "trang chủ của app"
            modifier = Modifier.padding(padding),
        ) {
            composable(GameSpaceDestinations.PLAYTIME) { PlaytimeScreen() }
            composable(GameSpaceDestinations.G_HOME) { GHomeScreen() }
            composable(GameSpaceDestinations.PROFILE) { ProfileScreen() }
        }
    }
}
