package com.gamespace.ui.ghome

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import com.gamespace.ui.icons.filled.Add
import com.gamespace.ui.icons.filled.History
import com.gamespace.ui.icons.filled.Search
import com.gamespace.ui.icons.filled.SportsEsports
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gamespace.packagemanager.GameEntity
import com.gamespace.packagemanager.InstalledAppInfo
import com.gamespace.ui.common.AppIconImage
import com.gamespace.ui.theme.GsIconBadge
import com.gamespace.ui.theme.GsOnSurface
import com.gamespace.ui.theme.GsOnSurfaceMuted
import com.gamespace.ui.theme.GsPrimary
import com.gamespace.ui.theme.GsSurfaceElevated
import com.gamespace.ui.theme.LocalGsSpacing
import com.gamespace.ui.theme.gsTransparentTopBarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GHomeScreen(viewModel: GHomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pickerState by viewModel.appPickerUiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    val spacing = LocalGsSpacing.current

    fun openPicker() {
        showAddDialog = true
        viewModel.loadInstalledApps()
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { TopAppBar(title = { Text("G-Home") }, colors = gsTransparentTopBarColors()) },
        floatingActionButton = {
            FloatingActionButton(onClick = { openPicker() }) {
                Icon(Icons.Filled.Add, contentDescription = "Thêm game")
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading) {
                // Không có gì để hiển thị — tránh nháy "Chưa có game" trong khoảnh khắc đầu
                // tiên trước khi Room trả dữ liệu.
            } else if (state.games.isEmpty()) {
                EmptyGHomeState(modifier = Modifier.align(Alignment.Center), onAddClick = { openPicker() })
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 84.dp),
                    contentPadding = PaddingValues(spacing.md),
                    horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(spacing.md),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (state.recentlyPlayed.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            RecentlyPlayedSection(games = state.recentlyPlayed, onLaunch = viewModel::launchGame)
                        }
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                "Tất cả game",
                                style = MaterialTheme.typography.titleMedium,
                                color = GsOnSurface,
                                modifier = Modifier.padding(top = spacing.sm),
                            )
                        }
                    }
                    items(state.games, key = { it.packageName }) { game ->
                        GameTile(game = game, onClick = { viewModel.launchGame(game.packageName) })
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        val alreadyAdded = remember(state.games) { state.games.map { it.packageName }.toSet() }
        AddGameDialog(
            apps = pickerState.apps,
            isLoading = pickerState.isLoading,
            alreadyAdded = alreadyAdded,
            onDismiss = { showAddDialog = false },
            onSelect = { pkg ->
                viewModel.addGame(pkg)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun EmptyGHomeState(modifier: Modifier = Modifier, onAddClick: () -> Unit) {
    val spacing = LocalGsSpacing.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.padding(spacing.xl)) {
        Box(
            modifier = Modifier.size(spacing.iconSizeLarge).background(GsPrimary.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.SportsEsports, contentDescription = null, tint = GsPrimary, modifier = Modifier.size(spacing.iconSize))
        }
        Spacer(Modifier.size(spacing.md))
        Text("Chưa có game nào", style = MaterialTheme.typography.titleMedium, color = GsOnSurface)
        Spacer(Modifier.size(spacing.xs))
        Text(
            "Nhấn nút bên dưới để chọn game từ danh sách ứng dụng đã cài trên máy",
            style = MaterialTheme.typography.bodyMedium,
            color = GsOnSurfaceMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.size(spacing.lg))
        Button(onClick = onAddClick) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(spacing.xs))
            Text("Thêm game")
        }
    }
}

@Composable
private fun RecentlyPlayedSection(games: List<GameEntity>, onLaunch: (String) -> Unit) {
    val spacing = LocalGsSpacing.current
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GsIconBadge(Icons.Filled.History)
            Spacer(Modifier.width(spacing.sm))
            Text("Chơi gần đây", style = MaterialTheme.typography.titleMedium, color = GsOnSurface)
        }
        Spacer(Modifier.size(spacing.sm))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            items(games, key = { it.packageName }) { game ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(76.dp)
                        .clickable { onLaunch(game.packageName) },
                ) {
                    AppIconImage(packageName = game.packageName, size = spacing.iconSize)
                    Spacer(Modifier.size(spacing.xs))
                    Text(
                        game.appLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = GsOnSurfaceMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun GameTile(game: GameEntity, onClick: () -> Unit) {
    val spacing = LocalGsSpacing.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(spacing.cardCorner))
            .clickable(onClick = onClick)
            .padding(spacing.sm),
    ) {
        AppIconImage(packageName = game.packageName, size = spacing.iconSizeLarge)
        Spacer(Modifier.size(spacing.xs))
        Text(
            game.appLabel,
            style = MaterialTheme.typography.labelMedium,
            color = GsOnSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/** Chọn app từ danh sách đã cài thay vì gõ package name — ẩn app đã có sẵn trong G-Home. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddGameDialog(
    apps: List<InstalledAppInfo>,
    isLoading: Boolean,
    alreadyAdded: Set<String>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filteredApps = remember(apps, query, alreadyAdded) {
        apps.filter { it.packageName !in alreadyAdded }
            .filter { query.isBlank() || it.appLabel.contains(query, ignoreCase = true) }
    }
    val spacing = LocalGsSpacing.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GsSurfaceElevated,
        shape = RoundedCornerShape(spacing.cardCorner),
        title = { Text("Chọn game để thêm") },
        text = {
            Column(modifier = Modifier.heightIn(max = 420.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text("Tìm ứng dụng...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.size(spacing.sm))
                when {
                    isLoading -> Box(
                        modifier = Modifier.fillMaxWidth().padding(spacing.lg),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }
                    filteredApps.isEmpty() -> Text(
                        "Không tìm thấy ứng dụng nào.",
                        modifier = Modifier.padding(spacing.md),
                        style = MaterialTheme.typography.bodySmall,
                        color = GsOnSurfaceMuted,
                    )
                    else -> LazyColumn {
                        items(filteredApps, key = { it.packageName }) { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(app.packageName) }
                                    .padding(vertical = spacing.sm),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AppIconImage(packageName = app.packageName)
                                Spacer(Modifier.width(spacing.sm))
                                Text(app.appLabel, style = MaterialTheme.typography.bodyMedium, color = GsOnSurface)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Đóng") } },
    )
}
