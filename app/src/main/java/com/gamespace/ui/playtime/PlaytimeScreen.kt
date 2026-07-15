package com.gamespace.ui.playtime

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import com.gamespace.ui.icons.filled.AccessTime
import com.gamespace.ui.icons.filled.EmojiEvents
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gamespace.packagemanager.GameEntity
import com.gamespace.ui.common.AppIconImage
import com.gamespace.ui.theme.GsIconBadge
import com.gamespace.ui.theme.GsOnSurface
import com.gamespace.ui.theme.GsOnSurfaceMuted
import com.gamespace.ui.theme.GsPrimary
import com.gamespace.ui.theme.GsSurfaceElevated
import com.gamespace.ui.theme.LocalGsSpacing
import com.gamespace.ui.theme.gsTransparentTopBarColors
import java.util.concurrent.TimeUnit

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PlaytimeScreen(viewModel: PlaytimeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val spacing = LocalGsSpacing.current

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { TopAppBar(title = { Text("Thời gian chơi") }, colors = gsTransparentTopBarColors()) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading) {
                // Chờ Room trả dữ liệu — không hiện gì để tránh nháy "Chưa có dữ liệu".
            } else if (state.games.isEmpty()) {
                EmptyPlaytimeState(modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(spacing.md),
                    verticalArrangement = Arrangement.spacedBy(spacing.sm),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item { TotalPlaytimeCard(totalMillis = state.totalPlayTimeMillis) }
                    itemsIndexed(state.games, key = { _, game -> game.packageName }) { index, game ->
                        GamePlaytimeRow(game = game, rank = index + 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun TotalPlaytimeCard(totalMillis: Long) {
    val spacing = LocalGsSpacing.current
    Card(
        shape = RoundedCornerShape(spacing.cardCorner),
        colors = CardDefaults.cardColors(containerColor = GsSurfaceElevated),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(spacing.md), verticalAlignment = Alignment.CenterVertically) {
            GsIconBadge(Icons.Filled.AccessTime)
            Spacer(Modifier.width(spacing.sm))
            Column {
                Text("Tổng thời gian chơi", style = MaterialTheme.typography.bodySmall, color = GsOnSurfaceMuted)
                Text(
                    formatDuration(totalMillis),
                    style = MaterialTheme.typography.titleLarge,
                    color = GsOnSurface,
                )
            }
        }
    }
}

@Composable
private fun GamePlaytimeRow(game: GameEntity, rank: Int) {
    val spacing = LocalGsSpacing.current
    Card(
        shape = RoundedCornerShape(spacing.cardCorner),
        colors = CardDefaults.cardColors(containerColor = GsSurfaceElevated.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(spacing.sm).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (rank <= 3) {
                Box(
                    modifier = Modifier.size(20.dp).background(GsPrimary.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.EmojiEvents, contentDescription = "Hạng $rank", tint = GsPrimary, modifier = Modifier.size(12.dp))
                }
                Spacer(Modifier.width(spacing.xs))
            }
            AppIconImage(packageName = game.packageName)
            Spacer(Modifier.width(spacing.sm))
            Column(Modifier.weight(1f)) {
                Text(game.appLabel, style = MaterialTheme.typography.titleSmall, color = GsOnSurface)
                Text(
                    lastPlayedLabel(game.lastPlayedMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = GsOnSurfaceMuted,
                )
            }
            Text(
                formatDuration(game.totalPlayTimeMillis),
                style = MaterialTheme.typography.titleSmall,
                color = GsPrimary,
            )
        }
    }
}

@Composable
private fun EmptyPlaytimeState(modifier: Modifier = Modifier) {
    val spacing = LocalGsSpacing.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.padding(spacing.xl)) {
        Box(
            modifier = Modifier.size(spacing.iconSizeLarge).background(GsPrimary.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.AccessTime, contentDescription = null, tint = GsPrimary, modifier = Modifier.size(spacing.iconSize))
        }
        Spacer(Modifier.size(spacing.md))
        Text("Chưa có dữ liệu thời gian chơi", style = MaterialTheme.typography.titleMedium, color = GsOnSurface)
        Spacer(Modifier.size(spacing.xs))
        Text(
            "Thêm game ở G-Home và bật theo dõi tự động ở Hồ sơ để bắt đầu ghi nhận",
            style = MaterialTheme.typography.bodyMedium,
            color = GsOnSurfaceMuted,
        )
    }
}

private fun formatDuration(millis: Long): String {
    val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(millis)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}p"
        totalMinutes > 0 -> "${minutes}p"
        else -> "< 1p"
    }
}

private fun lastPlayedLabel(lastPlayedMillis: Long?): String {
    if (lastPlayedMillis == null) return "Chưa chơi lần nào"
    val diffMillis = System.currentTimeMillis() - lastPlayedMillis
    val diffDays = TimeUnit.MILLISECONDS.toDays(diffMillis)
    return when {
        diffDays <= 0 -> "Chơi hôm nay"
        diffDays == 1L -> "Chơi hôm qua"
        else -> "Chơi $diffDays ngày trước"
    }
}
