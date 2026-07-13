package com.gamespace.ui.protection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gamespace.protection.ProtectedAppEntity
import com.gamespace.ui.theme.gsTransparentTopBarColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtectedAppsScreen(viewModel: ProtectedAppsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = { TopAppBar(title = { Text("Bảo vệ App Nhạc") }, colors = gsTransparentTopBarColors()) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) { Text("+") }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxWidth()) {
            Column {
                Text(
                    "App trong danh sách này sẽ không bị Optimizer Engine dọn RAM/buộc dừng, " +
                        "để nhạc nền không bị ngắt khi chơi game. Không thể ngăn Android tự kill " +
                        "app khi máy quá thiếu RAM — đây chỉ ngăn chính GAME SPACE chủ động dừng chúng.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp),
                )

                when {
                    state.isSyncing && state.apps.isEmpty() -> {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CircularProgressIndicator()
                            Spacer(Modifier.width(8.dp))
                            Text("Đang quét app nhạc đã cài...")
                        }
                    }
                    state.apps.isEmpty() -> {
                        Text(
                            "Chưa tìm thấy app nhạc nào tự nhận diện được. Nhấn + để thêm thủ công.",
                            modifier = Modifier.padding(32.dp),
                        )
                    }
                    else -> {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 24.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(state.apps, key = { it.packageName }) { app ->
                                ProtectedAppRow(
                                    app = app,
                                    onToggle = { viewModel.toggleEnabled(app) },
                                    onRemove = { viewModel.removeManual(app) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddProtectedAppDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { pkg ->
                viewModel.addManually(pkg)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun ProtectedAppRow(app: ProtectedAppEntity, onToggle: () -> Unit, onRemove: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.MusicNote, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(app.appLabel, style = MaterialTheme.typography.titleSmall)
                Text(app.packageName, style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = app.isEnabled, onCheckedChange = { onToggle() })
            if (app.addedManually) {
                IconButton(onClick = onRemove) {
                    Icon(Icons.Filled.Delete, contentDescription = "Xóa")
                }
            }
        }
    }
}

@Composable
private fun AddProtectedAppDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm app nhạc thủ công") },
        text = {
            Column {
                Text(
                    "Nhập package name (vd: com.spotify.music). Dùng khi app nhạc của bạn " +
                        "không tự xuất hiện ở trên.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true)
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text("Thêm") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
    )
}
