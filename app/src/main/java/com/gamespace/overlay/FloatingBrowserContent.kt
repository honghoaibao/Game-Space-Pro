package com.gamespace.overlay

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.gamespace.ui.theme.GsAccentGradient
import com.gamespace.ui.theme.GsOnAccent
import com.gamespace.ui.theme.GsOnSurface
import com.gamespace.ui.theme.GsPrimary
import com.gamespace.ui.theme.GsSurfaceElevated

@Composable
fun FloatingBrowserContent(
    state: FloatingBrowserState,
    onAddressChange: (String) -> Unit,
    onNavigate: (String) -> Unit,
    onGoBack: () -> Unit,
    onToggleMinimize: () -> Unit,
    onToggleLock: () -> Unit,
    onCycleAlpha: () -> Unit,
    onToggleTouchGuard: () -> Unit,
    onClose: () -> Unit,
    onCanGoBackChanged: (Boolean) -> Unit,
    onWebViewReady: (WebView) -> Unit,
    onDragMove: (Float, Float) -> Unit,
    onDragResize: (Float, Float) -> Unit,
) {
    if (state.isMinimized) {
        BrowserBubble(onClick = onToggleMinimize, onDrag = onDragMove)
    } else {
        BrowserPanel(
            state = state,
            onAddressChange = onAddressChange,
            onNavigate = onNavigate,
            onGoBack = onGoBack,
            onToggleMinimize = onToggleMinimize,
            onToggleLock = onToggleLock,
            onCycleAlpha = onCycleAlpha,
            onToggleTouchGuard = onToggleTouchGuard,
            onClose = onClose,
            onCanGoBackChanged = onCanGoBackChanged,
            onWebViewReady = onWebViewReady,
            onDragMove = onDragMove,
            onDragResize = onDragResize,
        )
    }
}

@Composable
private fun BrowserBubble(onClick: () -> Unit, onDrag: (Float, Float) -> Unit) {
    val dragModifier = Modifier.pointerInput(Unit) {
        detectDragGestures { change, amount ->
            change.consume()
            onDrag(amount.x, amount.y)
        }
    }
    Surface(
        shape = CircleShape,
        color = GsSurfaceElevated.copy(alpha = 0.94f),
        contentColor = GsOnSurface,
        modifier = dragModifier
            .size(54.dp)
            .border(1.5.dp, GsAccentGradient, CircleShape),
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Public, contentDescription = "Mở trình duyệt nổi", tint = GsPrimary)
        }
    }
}

@Composable
private fun BrowserPanel(
    state: FloatingBrowserState,
    onAddressChange: (String) -> Unit,
    onNavigate: (String) -> Unit,
    onGoBack: () -> Unit,
    onToggleMinimize: () -> Unit,
    onToggleLock: () -> Unit,
    onCycleAlpha: () -> Unit,
    onToggleTouchGuard: () -> Unit,
    onClose: () -> Unit,
    onCanGoBackChanged: (Boolean) -> Unit,
    onWebViewReady: (WebView) -> Unit,
    onDragMove: (Float, Float) -> Unit,
    onDragResize: (Float, Float) -> Unit,
) {
    val dragModifier = if (state.isLocked) {
        Modifier
    } else {
        Modifier.pointerInput(Unit) {
            detectDragGestures { change, amount ->
                change.consume()
                onDragMove(amount.x, amount.y)
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = GsSurfaceElevated.copy(alpha = 0.97f),
        contentColor = GsOnSurface,
        modifier = Modifier
            .fillMaxSize()
            .border(1.dp, GsAccentGradient, RoundedCornerShape(16.dp)),
    ) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                // Title bar — gradient thương hiệu, kéo để di chuyển cả cửa sổ (trừ khi đã khóa).
                Row(
                    modifier = dragModifier
                        .fillMaxWidth()
                        .background(GsAccentGradient, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .padding(8.dp, 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Public, contentDescription = null, tint = GsOnAccent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Floating Browser",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                        color = GsOnAccent,
                    )
                    TitleBarIcon(
                        icon = if (state.isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                        description = if (state.isLocked) "Mở khóa vị trí" else "Khóa vị trí",
                        onClick = onToggleLock,
                    )
                    TitleBarIcon(Icons.Filled.Opacity, "Đổi độ trong suốt", onCycleAlpha)
                    TitleBarIcon(Icons.Filled.TouchApp, "Chống chạm nhầm", onToggleTouchGuard)
                    TitleBarIcon(Icons.Filled.UnfoldLess, "Thu nhỏ", onToggleMinimize)
                    TitleBarIcon(Icons.Filled.Close, "Đóng", onClose)
                }

                // Thanh địa chỉ.
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onGoBack, enabled = state.canGoBack, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Quay lại", tint = GsOnSurface)
                    }
                    OutlinedTextField(
                        value = state.addressBarText,
                        onValueChange = onAddressChange,
                        singleLine = true,
                        modifier = Modifier.weight(1f).height(48.dp),
                        textStyle = MaterialTheme.typography.bodySmall,
                        shape = RoundedCornerShape(10.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = GsPrimary,
                        contentColor = GsOnAccent,
                        modifier = Modifier.height(36.dp),
                        onClick = { onNavigate(state.addressBarText) },
                    ) {
                        Box(Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                            Text("Đi", fontWeight = FontWeight.Bold, color = GsOnAccent)
                        }
                    }
                }

                // Nội dung WebView.
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    WebViewContent(
                        url = state.currentUrl,
                        onCanGoBackChanged = onCanGoBackChanged,
                        onWebViewReady = onWebViewReady,
                    )

                    // Resize handle — góc dưới phải.
                    if (!state.isLocked) {
                        ResizeHandle(
                            modifier = Modifier.align(Alignment.BottomEnd),
                            onDrag = onDragResize,
                        )
                    }

                    // Lớp chắn "chống chạm nhầm": mọi thao tác đều bị lớp này hứng thay vì
                    // xuống tới WebView, chỉ có nút mở khóa mới phản hồi.
                    if (state.isTouchGuardActive) {
                        TouchGuardOverlay(onUnlock = onToggleTouchGuard)
                    }
                }
            }
        }
    }
}

@Composable
private fun TitleBarIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(28.dp)) {
        Icon(icon, contentDescription = description, tint = GsOnAccent, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun ResizeHandle(modifier: Modifier, onDrag: (Float, Float) -> Unit) {
    Box(
        modifier = modifier
            .size(28.dp)
            .padding(4.dp)
            .background(GsPrimary.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
            .pointerInput(Unit) {
                detectDragGestures { change, amount ->
                    change.consume()
                    onDrag(amount.x, amount.y)
                }
            },
    )
}

@Composable
private fun TouchGuardOverlay(onUnlock: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center,
    ) {
        Surface(shape = RoundedCornerShape(10.dp), color = GsSurfaceElevated, onClick = onUnlock) {
            Row(Modifier.padding(12.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = GsPrimary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Chạm để mở khóa thao tác", color = GsOnSurface, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebViewContent(url: String, onCanGoBackChanged: (Boolean) -> Unit, onWebViewReady: (WebView) -> Unit) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                        onCanGoBackChanged(canGoBack())
                    }
                }
                loadUrl(url)
                onWebViewReady(this)
            }
        },
        update = { webView ->
            if (webView.url != url) {
                webView.loadUrl(url)
            }
        },
    )
}
