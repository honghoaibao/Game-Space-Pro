package com.gamespace.ui.library

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gamespace.packagemanager.GameEntity
import com.gamespace.packagemanager.InstalledAppInfo
import com.gamespace.profile.ProfileType
import com.gamespace.ui.theme.gsTransparentTopBarColors
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameLibraryScreen(viewModel: GameLibraryViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val appPickerState by viewModel.appPickerUiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { TopAppBar(title = { Text("Thư viện Game") }, colors = gsTransparentTopBarColors()) },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                showAddDialog = true
                viewModel.loadInstalledApps()
            }) {
                Text("+")
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxWidth()) {
            when {
                state.isSyncing && state.games.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.width(8.dp))
                        Text("Đang quét app đã cài...")
                    }
                }
                state.games.isEmpty() -> {
                    Text(
                        "Chưa tìm thấy game nào. Nhấn + để chọn app từ danh sách đã cài.",
                        modifier = Modifier.padding(32.dp),
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.games, key = { it.packageName }) { game ->
                            GameRow(
                                game = game,
                                isCompiling = game.packageName in state.isCompiling,
                                onToggleFavorite = { viewModel.toggleFavorite(game) },
                                onAssignProfile = { viewModel.assignProfile(game, it) },
                                onLaunch = { viewModel.launch(game) },
                                onSmartCompile = { viewModel.smartCompile(game) },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        val alreadyAdded = remember(state.games) { state.games.map { it.packageName }.toSet() }
        AddGameDialog(
            apps = appPickerState.apps,
            isLoading = appPickerState.isLoading,
            alreadyAdded = alreadyAdded,
            onDismiss = { showAddDialog = false },
            onSelect = { pkg ->
                viewModel.addManually(pkg)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun GameRow(
    game: GameEntity,
    isCompiling: Boolean,
    onToggleFavorite: () -> Unit,
    onAssignProfile: (ProfileType?) -> Unit,
    onLaunch: () -> Unit,
    onSmartCompile: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIconImage(packageName = game.packageName)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f, fill = true)) {
                    Text(game.appLabel, style = MaterialTheme.typography.titleSmall)
                    Text(game.packageName, style = MaterialTheme.typography.bodySmall)
                    Text(formatStats(game), style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (game.isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = "Yêu thích",
                    )
                }
                IconButton(onClick = onLaunch) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Mở game")
                }
            }

            Spacer(Modifier.width(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                ProfileDropdown(
                    selected = game.assignedProfile,
                    onSelect = onAssignProfile,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onSmartCompile, enabled = !isCompiling) {
                    Text(if (isCompiling) "Đang compile..." else "Smart Compile")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileDropdown(
    selected: ProfileType?,
    onSelect: (ProfileType?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selected?.let { "${it.emoji} ${it.displayName}" } ?: "Mặc định hệ thống",
            onValueChange = {},
            readOnly = true,
            label = { Text("Profile riêng") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Mặc định hệ thống") }, onClick = { onSelect(null); expanded = false })
            ProfileType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text("${type.emoji} ${type.displayName}") },
                    onClick = { onSelect(type); expanded = false },
                )
            }
        }
    }
}

/**
 * Danh sách app đã cài để người dùng CHỌN thay vì tự gõ package name (dễ gõ sai/không nhớ
 * chính xác). Có thể tìm theo tên hoặc package; app đã có trong Thư viện bị ẩn khỏi danh sách.
 */
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
            .filter {
                query.isBlank() ||
                    it.appLabel.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true)
            }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chọn game để thêm") },
        text = {
            Column(modifier = Modifier.heightIn(max = 420.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text("Tìm theo tên app...") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.width(8.dp))
                when {
                    isLoading -> Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    filteredApps.isEmpty() -> Text(
                        "Không tìm thấy app nào phù hợp.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    else -> LazyColumn {
                        items(filteredApps, key = { it.packageName }) { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(app.packageName) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AppIconImage(packageName = app.packageName)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(app.appLabel, style = MaterialTheme.typography.bodyMedium)
                                    Text(app.packageName, style = MaterialTheme.typography.bodySmall)
                                }
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

@Suppress("ProduceStateDoesNotAssignValue") // false positive khi assign qua `value = withContext(...) {}` — issuetracker.google.com/issues/368420773
@Composable
private fun AppIconImage(packageName: String) {
    val context = LocalContext.current
    val bitmapState = produceState<ImageBitmap?>(initialValue = null, packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val drawable = context.packageManager.getApplicationIcon(packageName)
                drawableToBitmap(drawable).asImageBitmap()
            }.getOrNull()
        }
    }
    val bitmap = bitmapState.value
    Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(40.dp))
        } else {
            CircularProgressIndicator(modifier = Modifier.size(20.dp))
        }
    }
}

private fun drawableToBitmap(drawable: Drawable): Bitmap {
    val width = drawable.intrinsicWidth.coerceAtLeast(1)
    val height = drawable.intrinsicHeight.coerceAtLeast(1)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

private fun formatStats(game: GameEntity): String {
    val playTimeMinutes = TimeUnit.MILLISECONDS.toMinutes(game.totalPlayTimeMillis)
    val lastPlayed = game.lastPlayedMillis?.let {
        SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(it)
    } ?: "Chưa chơi"
    return "Đã chơi $playTimeMinutes phút · Lần gần nhất: $lastPlayed"
}
