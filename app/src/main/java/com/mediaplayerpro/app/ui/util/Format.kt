package com.mediaplayerpro.app.ui.util

import java.text.Normalizer
import java.util.Locale

private val MARKS = Regex("\\p{Mn}+")

/** Bỏ dấu tiếng Việt để tìm kiếm: "Xóm Trường" -> "xom truong". */
fun String.foldVi(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(MARKS, "")
        .replace('đ', 'd').replace('Đ', 'D')
        .lowercase(Locale.ROOT)

fun formatTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%d:%02d", m, s)
}

fun formatSize(bytes: Long): String = when {
    bytes >= 1L shl 30 -> String.format(Locale.US, "%.2f GB", bytes / 1_073_741_824.0)
    bytes >= 1L shl 20 -> String.format(Locale.US, "%.1f MB", bytes / 1_048_576.0)
    bytes > 0 -> "${bytes / 1024} KB"
    else -> ""
}

fun formatSpeed(speed: Float): String = speed.toString().removeSuffix(".0") + "x"
