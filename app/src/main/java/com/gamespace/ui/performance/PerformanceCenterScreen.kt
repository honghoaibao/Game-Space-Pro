package com.gamespace.ui.performance

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gamespace.overlay.HudMetrics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceCenterScreen(viewModel: PerformanceCenterViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Performance Center") }) },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(padding),
        ) {
            item {
                Text(
                    "FPS đo được là của chính GAME SPACE, không phải FPS thật của game đang chơi " +
                        "(Android không cho app đọc frame counter của app khác).",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            item {
                MetricChartCard(
                    title = "FPS",
                    values = state.history.map { it.fps.toFloat() },
                    maxValue = 120f,
                    color = Color(0xFF3498FF),
                    currentLabel = state.latest?.let { "${it.fps} FPS" } ?: "—",
                )
            }
            item {
                MetricChartCard(
                    title = "RAM sử dụng",
                    values = state.history.map { it.ramUsedPercent.toFloat() },
                    maxValue = 100f,
                    color = Color(0xFF2ECC71),
                    currentLabel = state.latest?.let { "${it.ramUsedPercent}% (${it.ramAvailableMb}MB trống)" } ?: "—",
                )
            }
            item {
                MetricChartCard(
                    title = "Pin",
                    values = state.history.map { it.batteryPercent.toFloat() },
                    maxValue = 100f,
                    color = Color(0xFFFF9800),
                    currentLabel = state.latest?.let {
                        "${it.batteryPercent}%" + if (it.isCharging) " ⚡ Đang sạc" else ""
                    } ?: "—",
                )
            }
            item { SnapshotCard(state.latest) }
        }
    }
}

@Composable
private fun MetricChartCard(title: String, values: List<Float>, maxValue: Float, color: Color, currentLabel: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(currentLabel, style = MaterialTheme.typography.titleMedium, color = color)
            }
            Spacer(Modifier.height(8.dp))
            MiniLineChart(
                values = values,
                maxValue = maxValue,
                color = color,
                modifier = Modifier.fillMaxWidth().height(80.dp),
            )
        }
    }
}

@Composable
private fun MiniLineChart(values: List<Float>, maxValue: Float, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val stepX = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = index * stepX
            val ratio = (value / maxValue).coerceIn(0f, 1f)
            val y = size.height - (ratio * size.height)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = color, style = Stroke(width = 4f))
    }
}

@Composable
private fun SnapshotCard(latest: HudMetrics?) {
    if (latest == null) return
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Snapshot hiện tại", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            SnapshotRow("Profile", "${latest.activeProfile.emoji} ${latest.activeProfile.displayName}")
            SnapshotRow("Nhiệt độ", latest.thermalLabel)
            SnapshotRow("CPU", latest.cpuUsagePercent?.let { "$it%" } ?: "Chưa đọc được (cần Shizuku)")
        }
    }
}

@Composable
private fun SnapshotRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
