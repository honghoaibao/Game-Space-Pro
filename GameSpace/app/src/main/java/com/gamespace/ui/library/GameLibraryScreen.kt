package com.gamespace.ui.library

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.ExposedDropdownMenu
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gamespace.packagemanager.GameEntity
import com.gamespace.profile.ProfileType
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameLibraryScreen(viewModel: GameLibraryViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Thư viện Game") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
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
                        "Chưa tìm thấy game nào. Nhấn + để thêm thủ công.",
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
        AddGameDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { pkg ->
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

@Composable
private fun AddGameDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm game thủ công") },
        text = {
            Column {
                Text("Nhập package name (vd: com.tencent.ig)", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true)
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text("Thêm") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
    )
}

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
