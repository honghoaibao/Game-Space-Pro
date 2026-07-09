package com.gamespacepro.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gamespacepro.feature.dashboard.DashboardRoute
import com.gamespacepro.ui.theme.GameSpaceProTheme
import dagger.hilt.android.AndroidEntryPoint

private const val DASHBOARD_ROUTE = "dashboard"

/**
 * Single-activity host for Game Space Pro's Compose navigation graph.
 * Dashboard is the only destination so far; add further `composable(...)`
 * entries here as more `feature_*` screens land.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GameSpaceProApp()
        }
    }
}

@Composable
private fun GameSpaceProApp() {
    GameSpaceProTheme {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = DASHBOARD_ROUTE) {
            composable(DASHBOARD_ROUTE) {
                DashboardRoute()
            }
        }
    }
}


