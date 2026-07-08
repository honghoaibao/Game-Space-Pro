package com.gamespacepro.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.gamespacepro.ui.theme.GameSpaceProTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-activity host for Game Space Pro's Compose navigation graph.
 *
 * The screen content below is a deliberate placeholder: Navigation Compose
 * is wired in during the next scaffolding pass, once feature_dashboard
 * exists to navigate to.
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
        Scaffold { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = stringResource(R.string.app_name) + " — scaffolding OK")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GameSpaceProAppPreview() {
    GameSpaceProApp()
}

