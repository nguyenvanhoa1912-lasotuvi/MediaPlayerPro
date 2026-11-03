package com.mediaplayerpro.app.ui.cassette

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import com.mediaplayerpro.app.ui.util.formatTime

/** Các giao diện màn hình Đang phát. Thiết kế nguyên bản, không mô phỏng sản phẩm/nhãn hiệu cụ thể. */
enum class PlayerSkin(val label: String) {
    BOOMBOX("Radio-cassette"),
    DECK("Deck Hi-Fi"),
    POCKET("Máy bỏ túi"),
    TAPE("Cận cảnh băng"),
    MODERN("Hiện đại"),
}

data class DeckUi(
    val title: String,
    val artist: String,
    val progress: Float,
    val positionMs: Long,
    val durationMs: Long,
    val playing: Boolean,
)

private fun counterOf(ui: DeckUi) = ((ui.positionMs / 1000.0) * 1.6).toInt()
private fun sideInfo(ui: DeckUi) = if (ui.durationMs > 0) formatTime(ui.durationMs) else ""

private fun DrawScope.tape(ui: DeckUi, anim: DeckAnim, look: CassetteLook, x: Float, y: Float, w: Float, tm: TextMeasurer) =
    drawCassette(
        Offset(x, y), w, look, anim.leftAngle, anim.rightAngle, ui.progress,
        ui.title, ui.artist, sideInfo(ui), tm
    )

// ============================ 1. Radio-cassette bạc ============================

private val BOOMBOX_TAPE = CassetteLook(
    shell = Color(0xFF3A3F47), shellEdge = Color(0xFF9AA1AB), label = Color(0xFFF7F4EC),
    stripeA = Color(0xFF1E6FD9), stripeB = Color(0xFFE53935), ink = Color(0xFF1C2430),
)

@Composable
fun BoomboxFace(ui: DeckUi, anim: DeckAnim) {
    val tm = rememberTextMeasurer()
    Canvas(Modifier.fillMaxWidth().aspectRatio(1.18f)) {
        val w = size.width
        val h = size.height
        val dark = Color(0xFF2B2F36)
        label(tm, "STEREO RADIO CASSETTE RECORDER", Offset(w * 0.035f, h * 0.045f), w * 0.028f, dark, FontWeight.Bold, align = -1, spacing = w * 0.003f)
        label(tm, "4 BAND • 2 WAY", Offset(w * 0.965f, h * 0.045f), w * 0.024f, Color(0xFF59606B), FontWeight.Bold, align = 1)

        // Mặt dò đài: kim đỏ chạy theo tiến độ bài
        val dial = Rect(w * 0.035f, h * 0.085f, w * 0.965f, h * 0.2f)
        drawRoundRect(Color(0xFF14171B), dial.topLeft, dial.size, CornerRadius(h * 0.015f))
        val sx0 = dial.left + dial.width * 0.08f
        val sx1 = dial.right - dial.width * 0.08f
        for (i in 0..40) {
            val x = sx0 + (sx1 - sx0) * i / 40f
            val long = i % 8 == 0
            drawLine(Color(0xFFE6E1D3), Offset(x, dial.top + dial.height * 0.18f), Offset(x, dial.top + dial.height * (if (long) 0.42f else 0.3f)), w * 0.002f)
            if (long) label(tm, "${88 + i / 2}", Offset(x, dial.top + dial.height * 0.62f), dial.height * 0.24f, Color(0xFFE6E1D3), FontWeight.Bold)
        }
        label(tm, "FM", Offset(dial.left + dial.width * 0.035f, dial.center.y), dial.height * 0.26f, Color(0xFFFFB74D), FontWeight.Black)
        label(tm, "MHz", Offset(dial.right - dial.width * 0.035f, dial.center.y), dial.height * 0.2f, Color(0xFFFFB74D), FontWeight.Bold)
        val px = sx0 + (sx1 - sx0) * ui.progress
        drawLine(Color(0xFFFF3D00), Offset(px, dial.top + dial.height * 0.08f), Offset(px, dial.bottom - dial.height * 0.08f), w * 0.005f)
        drawRect(
            Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.18f), Color.Transparent), dial.top, dial.center.y),
            dial.topLeft, Size(dial.width, dial.height / 2)
        )

        // Hai loa
        val spR = w * 0.14f
        val spY = h * 0.6f
        drawSpeaker(Offset(w * 0.165f, spY), spR, anim.bass)
        drawSpeaker(Offset(w * 0.835f, spY), spR, anim.bass)

        // Cặp đồng hồ VU
        val vuW = w * 0.17f
        val vuH = vuW * 0.64f
        val vuTop = h * 0.245f
        val face = Brush.verticalGradient(listOf(Color(0xFFFFE9A8), Color(0xFFF4C35A)))
        drawVuMeter(Rect(w * 0.5f - vuW - w * 0.006f, vuTop, w * 0.5f - w * 0.006f, vuTop + vuH), anim.needleL, tm, face, Color(0xFF2A1E0A), Color(0xFF1B1D21), "L")
        drawVuMeter(Rect(w * 0.5f + w * 0.006f, vuTop, w * 0.5f + vuW + w * 0.006f, vuTop + vuH), anim.needleR, tm, face, Color(0xFF2A1E0A), Color(0xFF1B1D21), "R")

        // Cửa khay băng
        val door = Rect(w * 0.315f, vuTop + vuH + h * 0.025f, w * 0.685f, vuTop + vuH + h * 0.025f + h * 0.29f)
        drawRoundRect(Color(0xFF5C626B), door.topLeft, door.size, CornerRadius(w * 0.02f))
        val glass = door.deflate(w * 0.008f)
        drawRoundRect(Color(0xFF15181C), glass.topLeft, glass.size, CornerRadius(w * 0.015f))
        val tw = glass.width * 0.9f
        tape(ui, anim, BOOMBOX_TAPE, glass.center.x - tw / 2, glass.center.y - tw * 0.635f / 2, tw, tm)
        drawRoundRect(Color.White.copy(alpha = 0.07f), glass.topLeft, Size(glass.width, glass.height * 0.45f), CornerRadius(w * 0.015f))
        label(tm, "▲ EJECT", Offset(door.center.x, door.top - h * 0.012f), w * 0.02f, dark, FontWeight.Bold)

        // Bộ đếm + công tắc chức năng
        val ctr = Rect(w * 0.36f, door.bottom + h * 0.03f, w * 0.52f, door.bottom + h * 0.1f)
        drawCounter(ctr, counterOf(ui), tm)
        label(tm, "COUNTER", Offset(ctr.center.x, ctr.bottom + h * 0.025f), w * 0.018f, dark, FontWeight.Bold)
        val sw = Rect(w * 0.55f, ctr.top + ctr.height * 0.2f, w * 0.65f, ctr.bottom - ctr.height * 0.2f)
        drawRoundRect(Color(0xFF1B1D21), sw.topLeft, sw.size, CornerRadius(sw.height / 2))
        drawRoundRect(Color(0xFFE0E3E7), Offset(sw.right - sw.width * 0.5f, sw.top), Size(sw.width * 0.5f, sw.height), CornerRadius(sw.height / 2))
        label(tm, "RADIO", Offset(sw.left + sw.width * 0.25f, sw.bottom + h * 0.025f), w * 0.016f, dark)
        label(tm, "TAPE", Offset(sw.right - sw.width * 0.25f, sw.bottom + h * 0.025f), w * 0.016f, Color(0xFFD84315), FontWeight.Bold)

        drawBrushed(Rect(0f, h * 0.9f, w, h), 0.08f)
    }
}

// ============================ 2. Deck Hi-Fi đen ============================

private val DECK_TAPE = CassetteLook(
    shell = Color(0xFFD9D4C7), shellEdge = Color(0xFF8C8679), label = Color(0xFF151515),
    stripeA = Color(0xFFC9A227), stripeB = Color(0xFFC9A227), ink = Color(0xFFEFE6CC),
    hub = Color(0xFFEDEDED),
)

@Composable
fun DeckFace(ui: DeckUi, anim: DeckAnim) {
    val tm = rememberTextMeasurer()
    Canvas(Modifier.fillMaxWidth().aspectRatio(0.88f)) {
        val w = size.width
        val h = size.height
        val silver = Color(0xFFC9CDD3)
        drawBrushed(Rect(0f, 0f, w, h), 0.035f)
        label(tm, "STEREO CASSETTE DECK", Offset(w * 0.05f, h * 0.035f), w * 0.034f, silver, FontWeight.Bold, align = -1, spacing = w * 0.004f)
        drawCircle(Color(0xFF4CE07A), w * 0.008f, Offset(w * 0.9f, h * 0.035f))
        label(tm, "POWER", Offset(w * 0.88f, h * 0.035f), w * 0.022f, silver, align = 1)

        // VU lớn có đèn nền hổ phách
        val face = Brush.verticalGradient(listOf(Color(0xFFFFD98A), Color(0xFFE9A43A)))
        val vuTop = h * 0.07f
        val vuH = w * 0.26f
        drawVuMeter(Rect(w * 0.05f, vuTop, w * 0.485f, vuTop + vuH), anim.needleL, tm, face, Color(0xFF2A1A05), Color(0xFF050505), "L")
        drawVuMeter(Rect(w * 0.515f, vuTop, w * 0.95f, vuTop + vuH), anim.needleR, tm, face, Color(0xFF2A1A05), Color(0xFF050505), "R")

        // Khoang băng kính khói
        val bay = Rect(w * 0.07f, vuTop + vuH + h * 0.035f, w * 0.93f, vuTop + vuH + h * 0.035f + w * 0.54f)
        drawRoundRect(Color(0xFF3A3C41), bay.topLeft, bay.size, CornerRadius(w * 0.015f))
        val glass = bay.deflate(w * 0.012f)
        drawRoundRect(Color(0xFF0B0C0E), glass.topLeft, glass.size, CornerRadius(w * 0.01f))
        val tw = glass.width * 0.86f
        tape(ui, anim, DECK_TAPE, glass.center.x - tw / 2, glass.center.y - tw * 0.635f / 2 - glass.height * 0.02f, tw, tm)
        drawRoundRect(Color(0x33000000), glass.topLeft, glass.size, CornerRadius(w * 0.01f))
        drawRect(
            Brush.linearGradient(
                listOf(Color.Transparent, Color.White.copy(alpha = 0.08f), Color.Transparent),
                start = glass.topLeft, end = Offset(glass.right, glass.bottom)
            ),
            glass.topLeft, glass.size
        )
        label(tm, "AUTO REVERSE  ⇄", Offset(bay.center.x, bay.bottom + h * 0.025f), w * 0.024f, silver, FontWeight.Bold, spacing = w * 0.003f)

        // Hàng dưới: bộ đếm + loại băng
        val rowY = bay.bottom + h * 0.065f
        val ctr = Rect(w * 0.07f, rowY, w * 0.3f, rowY + h * 0.07f)
        drawCounter(ctr, counterOf(ui), tm, Color(0xFF050505))
        label(tm, "TAPE COUNTER", Offset(ctr.center.x, ctr.bottom + h * 0.022f), w * 0.02f, silver)
        val types = listOf("NORMAL", "HIGH", "METAL")
        types.forEachIndexed { i, t ->
            val cx = w * (0.45f + i * 0.17f)
            drawCircle(if (i == 0) Color(0xFFFFB300) else Color(0xFF3A2A05), w * 0.01f, Offset(cx, ctr.center.y - h * 0.012f))
            label(tm, t, Offset(cx, ctr.center.y + h * 0.022f), w * 0.02f, silver, FontWeight.Bold)
        }
    }
}

// ============================ 3. Máy nghe bỏ túi ============================

private val POCKET_TAPE = CassetteLook(
    shell = Color(0xCC1A1A1A), shellEdge = Color(0xFF6E6E6E), label = Color(0xFFFFF3D6),
    stripeA = Color(0xFFFF7043), stripeB = Color(0xFFFFB300), ink = Color(0xFF3E2723),
)

@Composable
fun PocketFace(ui: DeckUi, anim: DeckAnim) {
    val tm = rememberTextMeasurer()
    Canvas(Modifier.fillMaxWidth().aspectRatio(0.7f)) {
        val w = size.width
        val h = size.height
        val white = Color(0xFFEAF2FF)
        // viền bạc hai bên thân
        drawRect(Color.White.copy(alpha = 0.14f), Offset(0f, 0f), Size(w * 0.015f, h))
        drawRect(Color.White.copy(alpha = 0.14f), Offset(w * 0.985f, 0f), Size(w * 0.015f, h))
        label(tm, "STEREO CASSETTE PLAYER", Offset(w * 0.06f, h * 0.04f), w * 0.042f, white, FontWeight.Black, align = -1, spacing = w * 0.004f)
        label(tm, "AUTO REVERSE • BASS BOOST", Offset(w * 0.06f, h * 0.072f), w * 0.026f, white.copy(alpha = 0.7f), align = -1)

        // Nắp trong suốt
        val lid = Rect(w * 0.06f, h * 0.1f, w * 0.94f, h * 0.55f)
        drawRoundRect(Color(0xFF0E1E3A), lid.topLeft, lid.size, CornerRadius(w * 0.03f))
        val tw = lid.width * 0.9f
        tape(ui, anim, POCKET_TAPE, lid.center.x - tw / 2, lid.center.y - tw * 0.635f / 2, tw, tm)
        drawRoundRect(Color.White.copy(alpha = 0.06f), lid.topLeft, lid.size, CornerRadius(w * 0.03f))
        drawRoundRect(Color.White.copy(alpha = 0.45f), lid.topLeft, lid.size, CornerRadius(w * 0.03f), style = Stroke(w * 0.008f))
        drawRect(
            Brush.linearGradient(
                listOf(Color.Transparent, Color.White.copy(alpha = 0.16f), Color.Transparent),
                start = lid.topLeft, end = Offset(lid.right, lid.bottom)
            ),
            lid.topLeft, lid.size
        )

        // Đèn LED mức âm
        val ledL = Rect(w * 0.16f, h * 0.6f, w * 0.9f, h * 0.622f)
        val ledR = Rect(w * 0.16f, h * 0.637f, w * 0.9f, h * 0.659f)
        label(tm, "L", Offset(w * 0.1f, ledL.center.y), w * 0.032f, white, FontWeight.Bold)
        label(tm, "R", Offset(w * 0.1f, ledR.center.y), w * 0.032f, white, FontWeight.Bold)
        drawLedMeter(ledL, anim.needleL)
        drawLedMeter(ledR, anim.needleR)

        // Bộ đếm + đèn pin
        val ctr = Rect(w * 0.08f, h * 0.7f, w * 0.42f, h * 0.775f)
        drawCounter(ctr, counterOf(ui), tm, Color(0xFF0A1830))
        label(tm, "COUNTER", Offset(ctr.center.x, ctr.bottom + h * 0.02f), w * 0.026f, white.copy(alpha = 0.8f))
        drawCircle(if (ui.playing) Color(0xFF4CE07A) else Color(0xFF1E4D2B), w * 0.016f, Offset(w * 0.56f, ctr.center.y))
        label(tm, "BATT", Offset(w * 0.56f, ctr.bottom + h * 0.02f), w * 0.026f, white.copy(alpha = 0.8f))
        drawCircle(Color(0xFF0A1830), w * 0.04f, Offset(w * 0.8f, ctr.center.y))
        drawCircle(Color(0xFF8A96A8), w * 0.022f, Offset(w * 0.8f, ctr.center.y))
        drawCircle(Color(0xFF05080F), w * 0.011f, Offset(w * 0.8f, ctr.center.y))
        label(tm, "PHONES", Offset(w * 0.8f, ctr.bottom + h * 0.02f), w * 0.026f, white.copy(alpha = 0.8f))

        // Rãnh chống trượt
        for (i in 0 until 9) {
            val y = h * (0.84f + i * 0.017f)
            drawLine(Color.Black.copy(alpha = 0.22f), Offset(w * 0.1f, y), Offset(w * 0.9f, y), w * 0.006f)
        }
        // Bánh xe âm lượng ở mép phải
        val wheel = Rect(w * 0.955f, h * 0.15f, w, h * 0.33f)
        drawRoundRect(Color(0xFFB0B8C4), wheel.topLeft, wheel.size, CornerRadius(w * 0.01f))
        for (i in 0 until 14) {
            val y = wheel.top + wheel.height * (i + 0.5f) / 14f
            drawLine(Color(0xFF5F6B7A), Offset(wheel.left, y), Offset(wheel.right, y), w * 0.004f)
        }
    }
}

// ============================ 4. Cận cảnh băng ============================

private val CLOSE_TAPE = CassetteLook(
    shell = Color(0xE6202226), shellEdge = Color(0xFF7A7F88), label = Color(0xFFF3E9D2),
    stripeA = Color(0xFFE65100), stripeB = Color(0xFFFFA000), ink = Color(0xFF3B2A1A),
    window = Color(0x55000000),
)

@Composable
fun TapeFace(ui: DeckUi, anim: DeckAnim) {
    val tm = rememberTextMeasurer()
    Canvas(Modifier.fillMaxWidth().aspectRatio(1.3f)) {
        val w = size.width
        val h = size.height
        drawCircle(
            Brush.radialGradient(listOf(Color(0x33FF8A00), Color.Transparent), Offset(w / 2, h * 0.42f), w * 0.6f),
            w * 0.6f, Offset(w / 2, h * 0.42f)
        )
        val tw = w * 0.94f
        tape(ui, anim, CLOSE_TAPE, (w - tw) / 2, h * 0.05f, tw, tm)
        val y = h * 0.05f + tw * 0.635f + h * 0.08f
        drawLedMeter(Rect(w * 0.1f, y, w * 0.9f, y + h * 0.025f), anim.needleL, 24)
        drawLedMeter(Rect(w * 0.1f, y + h * 0.04f, w * 0.9f, y + h * 0.065f), anim.needleR, 24)
    }
}
