package com.mediaplayerpro.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mediaplayerpro.app.data.MediaFile
import com.mediaplayerpro.app.ui.theme.AppColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Cache ảnh bìa/khung hình theo bộ nhớ (1/8 heap). */
private object ThumbCache : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()) {
    override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
}

@Suppress("DEPRECATION")
private fun loadThumbnail(context: Context, file: MediaFile, sizePx: Int): Bitmap? = try {
    when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ->
            context.contentResolver.loadThumbnail(file.uri, Size(sizePx, sizePx), null)
        file.isVideo && file.id >= 0 ->
            MediaStore.Video.Thumbnails.getThumbnail(
                context.contentResolver, file.id, MediaStore.Video.Thumbnails.MINI_KIND, null
            )
        else -> file.artworkUri?.let { uri ->
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }
    }
} catch (_: Throwable) {
    null
}

@Composable
fun MediaThumbnail(
    file: MediaFile,
    modifier: Modifier = Modifier,
    sizePx: Int = 256,
    corner: Dp = 10.dp,
    iconSize: Dp = 24.dp,
) {
    val context = LocalContext.current.applicationContext
    val key = "${file.uri}#$sizePx"
    val bitmap by produceState<ImageBitmap?>(ThumbCache.get(key)?.asImageBitmap(), key) {
        if (value == null) {
            value = withContext(Dispatchers.IO) {
                loadThumbnail(context, file, sizePx)?.also { ThumbCache.put(key, it) }
            }?.asImageBitmap()
        }
    }
    Box(
        modifier
            .clip(RoundedCornerShape(corner))
            .background(Brush.linearGradient(listOf(AppColors.SurfaceHigh, AppColors.Surface))),
        contentAlignment = Alignment.Center
    ) {
        val b = bitmap
        if (b != null) {
            Image(b, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Icon(
                if (file.isVideo) Icons.Rounded.Movie else Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = AppColors.Accent.copy(alpha = 0.85f),
                modifier = Modifier.size(iconSize)
            )
        }
    }
}
