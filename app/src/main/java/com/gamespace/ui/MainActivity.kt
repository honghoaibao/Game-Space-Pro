package com.gamespace.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gamespace.ui.dashboard.DashboardScreen
import com.gamespace.ui.diagnostics.DiagnosticsScreen
import com.gamespace.ui.library.GameLibraryScreen
import com.gamespace.ui.performance.PerformanceCenterScreen
import com.gamespace.ui.protection.ProtectedAppsScreen
import com.gamespace.ui.theme.GameSpaceTheme
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
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameSpaceTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
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
