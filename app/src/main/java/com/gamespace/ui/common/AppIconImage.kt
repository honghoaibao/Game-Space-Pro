package com.gamespace.ui.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Icon app đã cài, dùng ở mọi nơi cần hiển thị icon game (G-Home, Thời gian chơi, dialog chọn
 * app) — TỐI ƯU RAM so với bản cũ (từng nằm riêng trong GameLibraryScreen):
 * 1. Decode bitmap giới hạn theo kích thước hiển thị thực tế (dp × density), KHÔNG decode ở
 *    intrinsic size gốc của icon (adaptive icon có thể tới 432×432px ARGB_8888 ≈ 747KB/icon dù
 *    chỉ hiển thị 40dp — lãng phí RAM đáng kể khi danh sách có hàng chục app).
 * 2. Cache LRU trong RAM theo packageName — LazyColumn dispose/compose lại item khi cuộn, không
 *    cache thì mỗi lần cuộn qua lại sẽ decode lại icon dù không đổi.
 */
// ProduceStateDoesNotAssignValue is suppressed below because of a confirmed Compose lint
// false positive (Google Issue Tracker #368420773) — the lambda does assign `value` in
// every path. Fixed upstream in newer Studio/lint releases; CI here still hits it.
@Suppress("ProduceStateDoesNotAssignValue")
@Composable
fun AppIconImage(packageName: String, size: Dp = 40.dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val targetPx = with(density) { size.roundToPx() }.coerceAtLeast(1)

    val bitmapState = produceState<ImageBitmap?>(initialValue = AppIconCache.get(packageName), packageName, targetPx) {
        val cached = AppIconCache.get(packageName)
        if (cached != null) {
            value = cached
            return@produceState
        }
        value = withContext(Dispatchers.IO) {
            runCatching { loadBoundedIcon(context, packageName, targetPx) }
                .getOrNull()
                ?.also { AppIconCache.put(packageName, it) }
        }
    }

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        val bitmap = bitmapState.value
        if (bitmap != null) {
            Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(size))
        } else {
            CircularProgressIndicator(modifier = Modifier.size(size / 2))
        }
    }
}

private fun loadBoundedIcon(context: Context, packageName: String, targetPx: Int): ImageBitmap {
    val drawable = context.packageManager.getApplicationIcon(packageName)
    return drawableToBoundedBitmap(drawable, targetPx).asImageBitmap()
}

/** Vẽ Drawable (kể cả AdaptiveIconDrawable) vào bitmap đúng [targetPx] thay vì intrinsic size gốc. */
private fun drawableToBoundedBitmap(drawable: Drawable, targetPx: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(targetPx, targetPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, targetPx, targetPx)
    drawable.draw(canvas)
    return bitmap
}

/**
 * Cache RAM bounded theo SỐ BYTE thực tế (không phải số lượng entry) — 6MB đủ cho ~vài trăm
 * icon ở kích thước hiển thị thực (40-64dp), tự động evict icon ít dùng nhất khi đầy.
 */
private object AppIconCache {
    private const val MAX_BYTES = 6 * 1024 * 1024

    private val cache = object : LruCache<String, ImageBitmap>(MAX_BYTES) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
    }

    fun get(key: String): ImageBitmap? = cache.get(key)
    fun put(key: String, value: ImageBitmap) {
        cache.put(key, value)
    }
}
