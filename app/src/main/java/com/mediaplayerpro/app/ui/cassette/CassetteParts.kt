package com.mediaplayerpro.app.ui.cassette

import android.view.SoundEffectConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediaplayerpro.app.playback.AudioLevels
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

// ======================= Trạng thái chuyển động =======================

/** Góc quay 2 trục băng, vị trí kim VU và độ rung loa – cập nhật theo từng khung hình. */
@Stable
class DeckAnim {
    var leftAngle by mutableFloatStateOf(0f)
    var rightAngle by mutableFloatStateOf(0f)
    var needleL by mutableFloatStateOf(0f)
    var needleR by mutableFloatStateOf(0f)
    var bass by mutableFloatStateOf(0f)
}

/** Bán kính cuộn băng trái/phải (tỉ lệ với bán kính tối đa) theo tiến độ bài. */
internal fun packRadii(progress: Float): Pair<Float, Float> {
    val p = progress.coerceIn(0f, 1f)
    val r0 = 0.36f
    val area = 1f - r0 * r0
    return sqrt(r0 * r0 + area * (1f - p)) to sqrt(r0 * r0 + area * p)
}

/** RMS (0..1) -> vị trí kim (0..1). 0 VU ≈ -10 dBFS. */
internal fun vuPos(rms: Float): Float {
    val db = 20f * log10(max(rms, 1e-5f))
    return ((db + 30f) / 23f).coerceIn(0f, 1.04f)
}

/**
 * @param wind -1 tua lui, 1 tua tới, 0 bình thường
 */
@Composable
fun rememberDeckAnim(playing: Boolean, wind: Int, progress: Float): DeckAnim {
    val anim = remember { DeckAnim() }
    val p by rememberUpdatedState(progress)
    val pl by rememberUpdatedState(playing)
    val wd by rememberUpdatedState(wind)
    LaunchedEffect(anim) {
        var last = 0L
        while (isActive) {
            var idle = false
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(0.12f)
                last = now
                // Băng chạy tốc độ dài không đổi -> trục nhỏ quay nhanh, trục lớn quay chậm
                val speed = when {
                    wd != 0 -> 11f * wd
                    pl -> 1f
                    else -> 0f
                }
                if (speed != 0f && dt > 0f) {
                    val (rl, rr) = packRadii(p)
                    val v = 1.9f * speed
                    anim.leftAngle = (anim.leftAngle + Math.toDegrees((v / rl * dt).toDouble()).toFloat()) % 360f
                    anim.rightAngle = (anim.rightAngle + Math.toDegrees((v / rr * dt).toDouble()).toFloat()) % 360f
                }
                val lv = AudioLevels.levels.value
                val tl = if (pl) vuPos(lv.left) else 0f
                val tr = if (pl) vuPos(lv.right) else 0f
                fun follow(cur: Float, target: Float): Float {
                    val k = ((if (target > cur) 14f else 5f) * dt).coerceAtMost(1f)
                    val n = cur + (target - cur) * k
                    return if (abs(n - target) < 0.002f) target else n
                }
                anim.needleL = follow(anim.needleL, tl)
                anim.needleR = follow(anim.needleR, tr)
                val bassTarget = if (pl) ((tl + tr) / 2f - 0.45f).coerceAtLeast(0f) * 1.8f else 0f
                anim.bass = follow(anim.bass, bassTarget)
                idle = speed == 0f && anim.needleL == 0f && anim.needleR == 0f && anim.bass == 0f
            }
            if (idle) {
                delay(150)
                last = 0L
            }
        }
    }
    return anim
}

// ======================= Tiện ích vẽ chữ =======================

internal fun DrawScope.label(
    tm: TextMeasurer,
    text: String,
    anchor: Offset,
    px: Float,
    color: Color,
    weight: FontWeight = FontWeight.Normal,
    maxWidth: Float = size.width,
    family: FontFamily = FontFamily.SansSerif,
    italic: Boolean = false,
    spacing: Float = 0f,
    align: Int = 0, // -1 trái, 0 giữa, 1 phải
) {
    if (text.isEmpty() || px <= 0.5f) return
    val unit = density * fontScale
    val style = TextStyle(
        color = color,
        fontSize = (px / unit).sp,
        fontWeight = weight,
        fontFamily = family,
        fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
        letterSpacing = (spacing / unit).sp,
    )
    val layout = tm.measure(
        AnnotatedString(text), style,
        overflow = TextOverflow.Ellipsis, softWrap = false, maxLines = 1,
        constraints = Constraints(maxWidth = maxWidth.toInt().coerceAtLeast(1))
    )
    val x = when (align) {
        -1 -> anchor.x
        1 -> anchor.x - layout.size.width
        else -> anchor.x - layout.size.width / 2f
    }
    drawText(layout, topLeft = Offset(x, anchor.y - layout.size.height / 2f))
}

// ======================= Băng cassette =======================

data class CassetteLook(
    val shell: Color,
    val shellEdge: Color,
    val label: Color,
    val stripeA: Color,
    val stripeB: Color,
    val ink: Color,
    val window: Color = Color(0x66000000),
    val hub: Color = Color(0xFFF1F1F1),
    val tape: Color = Color(0xFF3B2618),
)

/** Vẽ băng cassette tỉ lệ thật (100 × 63,5). Chiều cao = width × 0,635. */
internal fun DrawScope.drawCassette(
    topLeft: Offset,
    width: Float,
    look: CassetteLook,
    leftAngle: Float,
    rightAngle: Float,
    progress: Float,
    title: String,
    subtitle: String,
    sideInfo: String,
    tm: TextMeasurer,
) {
    val w = width
    val h = w * 0.635f
    val x0 = topLeft.x
    val y0 = topLeft.y
    val corner = CornerRadius(w * 0.035f)

    drawRoundRect(Color.Black.copy(alpha = 0.35f), Offset(x0 + w * 0.012f, y0 + h * 0.035f), Size(w, h), corner)
    drawRoundRect(look.shell, Offset(x0, y0), Size(w, h), corner)
    drawRoundRect(look.shellEdge, Offset(x0, y0), Size(w, h), corner, style = Stroke(w * 0.006f))

    val cy = y0 + h * 0.47f
    val cl = Offset(x0 + w * 0.287f, cy)
    val cr = Offset(x0 + w * 0.713f, cy)
    val rMax = w * 0.19f
    val (packL, packR) = packRadii(progress)

    // Hai cuộn băng từ
    for ((c, rr) in listOf(cl to packL, cr to packR)) {
        drawCircle(look.tape, rr * rMax, c)
        drawCircle(Color.White.copy(alpha = 0.07f), rr * rMax * 0.82f, c, style = Stroke(w * 0.004f))
        drawCircle(Color.Black.copy(alpha = 0.25f), rr * rMax, c, style = Stroke(w * 0.003f))
    }

    // Nhãn có ô cửa sổ hình con nhộng
    val holeR = w * 0.072f
    val slot = RoundRect(cl.x - holeR, cy - holeR, cr.x + holeR, cy + holeR, CornerRadius(holeR))
    val labelRect = RoundRect(x0 + w * 0.055f, y0 + h * 0.065f, x0 + w * 0.945f, y0 + h * 0.74f, CornerRadius(w * 0.02f))
    val labelPath = Path().apply {
        addRoundRect(labelRect)
        addRoundRect(slot)
        fillType = PathFillType.EvenOdd
    }
    drawPath(labelPath, look.label)
    clipPath(labelPath) {
        drawRect(look.stripeA, Offset(labelRect.left, y0 + h * 0.300f), Size(labelRect.width, h * 0.048f))
        drawRect(look.stripeB, Offset(labelRect.left, y0 + h * 0.615f), Size(labelRect.width, h * 0.048f))
        // dòng kẻ viết tay
        drawLine(look.ink.copy(alpha = 0.18f), Offset(x0 + w * 0.1f, y0 + h * 0.275f), Offset(x0 + w * 0.9f, y0 + h * 0.275f), w * 0.002f)
    }

    // Kính cửa sổ + hai trục
    drawRoundRect(look.window, Offset(slot.left, slot.top), Size(slot.width, slot.height), CornerRadius(holeR))
    drawHub(cl, w, leftAngle, look)
    drawHub(cr, w, rightAngle, look)
    drawRoundRect(
        Color.White.copy(alpha = 0.10f),
        Offset(slot.left + holeR, slot.top + slot.height * 0.12f),
        Size(slot.width - 2 * holeR, slot.height * 0.16f),
        CornerRadius(slot.height * 0.08f)
    )

    // Chữ trên nhãn
    label(tm, title, Offset(x0 + w / 2, y0 + h * 0.15f), w * 0.05f, look.ink, FontWeight.Bold, maxWidth = w * 0.8f, family = FontFamily.Serif, italic = true)
    label(tm, subtitle, Offset(x0 + w / 2, y0 + h * 0.235f), w * 0.034f, look.ink.copy(alpha = 0.75f), maxWidth = w * 0.8f)
    label(tm, "A", Offset(x0 + w * 0.13f, cy), w * 0.08f, look.ink, FontWeight.Black)
    label(tm, sideInfo, Offset(x0 + w * 0.87f, cy), w * 0.032f, look.ink, FontWeight.Bold)

    // Khe đầu từ phía dưới
    val trap = Path().apply {
        moveTo(x0 + w * 0.17f, y0 + h)
        lineTo(x0 + w * 0.215f, y0 + h * 0.8f)
        lineTo(x0 + w * 0.785f, y0 + h * 0.8f)
        lineTo(x0 + w * 0.83f, y0 + h)
        close()
    }
    drawPath(trap, Color.Black.copy(alpha = 0.18f))
    drawPath(trap, look.shellEdge, style = Stroke(w * 0.004f))
    for (fx in listOf(0.3f, 0.38f, 0.62f, 0.7f)) {
        drawCircle(Color.Black.copy(alpha = 0.55f), w * 0.013f, Offset(x0 + w * fx, y0 + h * 0.9f))
    }
    for (p in listOf(Offset(0.035f, 0.06f), Offset(0.965f, 0.06f), Offset(0.035f, 0.94f), Offset(0.965f, 0.94f), Offset(0.5f, 0.9f))) {
        drawScrew(Offset(x0 + w * p.x, y0 + h * p.y), w * 0.014f, look.shellEdge)
    }
}

private fun DrawScope.drawHub(c: Offset, w: Float, angle: Float, look: CassetteLook) {
    val ro = w * 0.06f
    val ri = w * 0.036f
    drawCircle(look.hub, ro, c)
    drawCircle(Color.Black.copy(alpha = 0.25f), ro, c, style = Stroke(w * 0.004f))
    drawCircle(Color(0xFF161616), ri, c)
    rotate(angle, pivot = c) {
        for (k in 0 until 6) {
            rotate(k * 60f, pivot = c) {
                drawRect(look.hub, Offset(c.x - w * 0.0055f, c.y - ri - w * 0.001f), Size(w * 0.011f, w * 0.013f))
            }
        }
        for (k in 0 until 3) {
            rotate(k * 120f, pivot = c) {
                drawLine(
                    Color.Black.copy(alpha = 0.35f),
                    Offset(c.x, c.y - ri - w * 0.004f), Offset(c.x, c.y - ro + w * 0.005f),
                    strokeWidth = w * 0.006f
                )
            }
        }
    }
}

internal fun DrawScope.drawScrew(c: Offset, r: Float, color: Color) {
    drawCircle(color, r, c)
    drawCircle(Color.Black.copy(alpha = 0.35f), r, c, style = Stroke(r * 0.2f))
    drawLine(Color.Black.copy(alpha = 0.6f), c - Offset(r * 0.6f, 0f), c + Offset(r * 0.6f, 0f), r * 0.25f)
    drawLine(Color.Black.copy(alpha = 0.6f), c - Offset(0f, r * 0.6f), c + Offset(0f, r * 0.6f), r * 0.25f)
}

// ======================= Đồng hồ VU kim =======================

private val VU_RED = Color(0xFFD32F2F)

internal fun DrawScope.drawVuMeter(
    rect: Rect,
    pos: Float,
    tm: TextMeasurer,
    face: Brush,
    ink: Color,
    frame: Color,
    channel: String,
) {
    drawRoundRect(frame, rect.topLeft, rect.size, CornerRadius(rect.height * 0.08f))
    val f = rect.deflate(rect.height * 0.07f)
    clipRect(f.left, f.top, f.right, f.bottom) {
        drawRect(face, f.topLeft, f.size)
        val pivot = Offset(f.center.x, f.bottom + f.height * 0.32f)
        val rad = f.height * 1.02f
        val span = 44f
        val zero = 20f / 23f
        fun ang(p: Float) = -span + 2 * span * p.coerceIn(0f, 1.04f)
        fun pt(a: Float, rr: Float): Offset {
            val t = Math.toRadians(a.toDouble())
            return Offset(pivot.x + rr * sin(t).toFloat(), pivot.y - rr * cos(t).toFloat())
        }
        val arcR = rad * 0.8f
        val arcTopLeft = Offset(pivot.x - arcR, pivot.y - arcR)
        val arcSize = Size(arcR * 2, arcR * 2)
        drawArc(ink, 270f - span, 2 * span * zero, false, arcTopLeft, arcSize, style = Stroke(f.height * 0.014f))
        drawArc(VU_RED, 270f - span + 2 * span * zero, 2 * span * (1 - zero), false, arcTopLeft, arcSize, style = Stroke(f.height * 0.05f))
        for (m in listOf(-20, -10, -7, -5, -3, -2, -1, 0, 1, 2, 3)) {
            val a = ang((m + 20f) / 23f)
            val markColor = if (m > 0) VU_RED else ink
            drawLine(markColor, pt(a, rad * 0.8f), pt(a, rad * 0.9f), strokeWidth = f.height * 0.016f)
            if (m == -20 || m == -10 || m == -5 || m == -3 || m == 0 || m == 3) {
                label(tm, if (m > 0) "+$m" else "$m", pt(a, rad * 0.99f), f.height * 0.09f, markColor, FontWeight.Bold)
            }
        }
        label(tm, "VU", Offset(f.center.x, f.bottom - f.height * 0.32f), f.height * 0.17f, ink, FontWeight.Black)
        label(tm, channel, Offset(f.left + f.width * 0.09f, f.bottom - f.height * 0.15f), f.height * 0.11f, ink, FontWeight.Bold)
        drawLine(Color(0xFF101010), pivot, pt(ang(pos), rad * 0.97f), strokeWidth = f.height * 0.02f, cap = StrokeCap.Round)
        drawCircle(Color(0xFF202020), f.height * 0.17f, Offset(pivot.x, f.bottom + f.height * 0.06f))
        drawRect(
            Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.25f), Color.Transparent), f.top, f.top + f.height * 0.5f),
            f.topLeft, Size(f.width, f.height * 0.5f)
        )
    }
    val peak = pos > 0.95f
    drawCircle(
        if (peak) Color(0xFFFF3B30) else Color(0xFF4A1512),
        rect.height * 0.045f,
        Offset(rect.right - rect.height * 0.13f, rect.top + rect.height * 0.14f)
    )
}

/** Thanh LED nhiều nấc (xanh – vàng – đỏ). */
internal fun DrawScope.drawLedMeter(rect: Rect, pos: Float, segments: Int = 14) {
    val gap = rect.width * 0.012f
    val segW = (rect.width - gap * (segments - 1)) / segments
    val lit = (pos.coerceIn(0f, 1f) * segments).roundToInt()
    for (i in 0 until segments) {
        val col = when {
            i >= segments - 2 -> Color(0xFFFF3B30)
            i >= segments - 5 -> Color(0xFFFFC107)
            else -> Color(0xFF4CE07A)
        }
        drawRoundRect(
            if (i < lit) col else col.copy(alpha = 0.15f),
            Offset(rect.left + i * (segW + gap), rect.top), Size(segW, rect.height),
            CornerRadius(rect.height * 0.25f)
        )
    }
}

/** Bộ đếm băng 3 số kiểu cơ. */
internal fun DrawScope.drawCounter(rect: Rect, value: Int, tm: TextMeasurer, body: Color = Color(0xFF151515)) {
    drawRoundRect(body, rect.topLeft, rect.size, CornerRadius(rect.height * 0.15f))
    val digits = (value.coerceAtLeast(0) % 1000).toString().padStart(3, '0')
    val cw = rect.width * 0.25f
    val gap = rect.width * 0.04f
    val startX = rect.center.x - (cw * 3 + gap * 2) / 2
    val top = rect.top + rect.height * 0.14f
    val bottom = rect.bottom - rect.height * 0.14f
    for (i in 0 until 3) {
        val l = startX + i * (cw + gap)
        drawRoundRect(
            Brush.verticalGradient(listOf(Color(0xFF9E9E9E), Color.White, Color(0xFF9E9E9E)), top, bottom),
            Offset(l, top), Size(cw, bottom - top), CornerRadius(rect.height * 0.06f)
        )
        label(tm, digits[i].toString(), Offset(l + cw / 2, rect.center.y), rect.height * 0.52f, Color(0xFF111111), FontWeight.Bold, family = FontFamily.Monospace)
    }
}

/** Loa có màng rung theo tiếng bass. */
internal fun DrawScope.drawSpeaker(c: Offset, r: Float, pulse: Float) {
    drawCircle(Brush.radialGradient(listOf(Color(0xFFB9BEC5), Color(0xFF5A5F66)), c, r), r, c)
    drawCircle(Color(0xFF121212), r * 0.9f, c)
    // lưới tản âm
    val step = r * 0.1f
    var gy = c.y - r * 0.86f
    while (gy <= c.y + r * 0.86f) {
        var gx = c.x - r * 0.86f
        while (gx <= c.x + r * 0.86f) {
            val dx = gx - c.x
            val dy = gy - c.y
            if (dx * dx + dy * dy < (r * 0.86f) * (r * 0.86f)) drawCircle(Color(0xFF2A2A2A), step * 0.28f, Offset(gx, gy))
            gx += step
        }
        gy += step
    }
    val k = 1f + 0.045f * pulse.coerceIn(0f, 1.2f)
    drawCircle(Color(0xFF262626).copy(alpha = 0.85f), r * 0.66f * k, c)
    drawCircle(Brush.radialGradient(listOf(Color(0xFF454545), Color(0xFF0D0D0D)), c, r * 0.58f * k), r * 0.58f * k, c)
    for (i in 1..4) drawCircle(Color.White.copy(alpha = 0.05f), r * 0.58f * k * i / 5f, c, style = Stroke(r * 0.012f))
    drawCircle(
        Brush.radialGradient(listOf(Color(0xFF8A8A8A), Color(0xFF1C1C1C)), c - Offset(r * 0.05f, r * 0.05f), r * 0.2f * k),
        r * 0.18f * k, c
    )
    drawCircle(Color.Black.copy(alpha = 0.4f), r * 0.9f, c, style = Stroke(r * 0.03f))
}

/** Vân kim loại xước ngang. */
internal fun DrawScope.drawBrushed(rect: Rect, alpha: Float) {
    var y = rect.top
    var i = 0
    while (y < rect.bottom) {
        drawLine(
            Color.White.copy(alpha = if (i % 3 == 0) alpha else alpha * 0.35f),
            Offset(rect.left, y), Offset(rect.right, y), 1f
        )
        y += 3f
        i++
    }
}

// ======================= Phím bấm cơ =======================

data class KeyStyle(
    val top: Color,
    val bottom: Color,
    val icon: Color,
    val edge: Color,
    val text: Color,
)

@Composable
fun MechKeyRow(
    style: KeyStyle,
    playing: Boolean,
    paused: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    onPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onWind: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        MechKey(Icons.Rounded.SkipPrevious, "PREV", style, latched = false, onClick = onPrev)
        MechKey(
            Icons.Rounded.FastRewind, "REW", style, latched = false,
            onClick = { onSeek(-10_000) },
            onHold = { onSeek(-4_000) },
            onHoldChange = { onWind(if (it) -1 else 0) }
        )
        MechKey(Icons.Rounded.PlayArrow, "PLAY", style, latched = playing, led = true, onClick = onPlay)
        MechKey(Icons.Rounded.Stop, "STOP", style, latched = false, onClick = onStop)
        MechKey(Icons.Rounded.Pause, "PAUSE", style, latched = paused, onClick = onPause)
        MechKey(
            Icons.Rounded.FastForward, "FF", style, latched = false,
            onClick = { onSeek(10_000) },
            onHold = { onSeek(4_000) },
            onHoldChange = { onWind(if (it) 1 else 0) }
        )
        MechKey(Icons.Rounded.SkipNext, "NEXT", style, latched = false, onClick = onNext)
    }
}

@Composable
private fun RowScope.MechKey(
    icon: ImageVector,
    text: String,
    style: KeyStyle,
    latched: Boolean,
    led: Boolean = false,
    onClick: () -> Unit,
    onHold: (() -> Unit)? = null,
    onHoldChange: (Boolean) -> Unit = {},
) {
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var pressed by remember { mutableStateOf(false) }
    val click by rememberUpdatedState(onClick)
    val hold by rememberUpdatedState(onHold)
    val holdChange by rememberUpdatedState(onHoldChange)
    val down = pressed || latched
    val shape = RoundedCornerShape(6.dp)

    Box(
        Modifier
            .weight(1f)
            .height(60.dp)
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    pressed = true
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    var holding = false
                    val h = hold
                    val job = if (h != null) scope.launch {
                        delay(380)
                        holding = true
                        holdChange(true)
                        while (isActive) {
                            h()
                            delay(110)
                        }
                    } else null
                    val released = tryAwaitRelease()
                    job?.cancel()
                    if (holding) holdChange(false)
                    pressed = false
                    if (released && !holding) click()
                })
            },
        contentAlignment = Alignment.TopCenter
    ) {
        // độ dày phím (cạnh tối phía dưới)
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(top = 6.dp)
                .background(style.edge, shape)
        )
        Column(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .offset(y = if (down) 7.dp else 0.dp)
                .background(
                    Brush.verticalGradient(if (down) listOf(style.bottom, style.top) else listOf(style.top, style.bottom)),
                    shape
                )
                .border(1.dp, style.edge.copy(alpha = 0.7f), shape),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, text, tint = style.icon, modifier = Modifier.size(20.dp))
            Text(text, color = style.text, fontSize = 8.sp, fontWeight = FontWeight.Bold, maxLines = 1, letterSpacing = 0.5.sp)
        }
        if (led) {
            Box(
                Modifier
                    .offset(y = if (down) 10.dp else 3.dp)
                    .size(5.dp)
                    .background(if (latched) Color(0xFF4CE07A) else Color(0x332E7D32), CircleShape)
            )
        }
    }
}
