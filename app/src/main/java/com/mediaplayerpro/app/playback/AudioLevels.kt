package com.mediaplayerpro.app.playback

import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.TeeAudioProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

data class StereoLevel(val left: Float, val right: Float)

/**
 * Mức âm lượng thực (RMS) của bài đang phát, dùng cho kim đồng hồ VU.
 * Lấy mẫu trực tiếp từ luồng âm thanh của ExoPlayer nên KHÔNG cần quyền ghi âm.
 */
object AudioLevels {
    private val _levels = MutableStateFlow(StereoLevel(0f, 0f))
    val levels: StateFlow<StereoLevel> = _levels.asStateFlow()

    internal fun publish(left: Float, right: Float) {
        _levels.value = StereoLevel(left, right)
    }

    internal fun reset() {
        _levels.value = StereoLevel(0f, 0f)
    }
}

@OptIn(UnstableApi::class)
class LevelSink : TeeAudioProcessor.AudioBufferSink {
    @Volatile private var channels = 2
    @Volatile private var encoding = C.ENCODING_PCM_16BIT
    private var accL = 0.0
    private var accR = 0.0
    private var count = 0
    private var lastEmit = 0L

    override fun flush(sampleRateHz: Int, channelCount: Int, encoding: Int) {
        channels = channelCount.coerceAtLeast(1)
        this.encoding = encoding
        accL = 0.0; accR = 0.0; count = 0
        AudioLevels.reset()
    }

    override fun handleBuffer(buffer: ByteBuffer) {
        val b = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        val ch = channels
        when (encoding) {
            C.ENCODING_PCM_16BIT -> {
                val frame = 2 * ch
                var i = b.position()
                val end = b.limit() - frame
                while (i <= end) {
                    val l = b.getShort(i) / 32768.0
                    val r = if (ch > 1) b.getShort(i + 2) / 32768.0 else l
                    accL += l * l; accR += r * r; count++
                    i += frame
                }
            }
            C.ENCODING_PCM_FLOAT -> {
                val frame = 4 * ch
                var i = b.position()
                val end = b.limit() - frame
                while (i <= end) {
                    val l = b.getFloat(i).toDouble()
                    val r = if (ch > 1) b.getFloat(i + 4).toDouble() else l
                    accL += l * l; accR += r * r; count++
                    i += frame
                }
            }
            else -> return
        }
        val now = SystemClock.uptimeMillis()
        if (count > 0 && now - lastEmit >= 33) {
            AudioLevels.publish(sqrt(accL / count).toFloat(), sqrt(accR / count).toFloat())
            accL = 0.0; accR = 0.0; count = 0
            lastEmit = now
        }
    }
}
