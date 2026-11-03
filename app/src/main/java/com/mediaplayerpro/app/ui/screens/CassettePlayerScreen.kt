package com.mediaplayerpro.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mediaplayerpro.app.ui.MainViewModel
import com.mediaplayerpro.app.ui.cassette.BoomboxFace
import com.mediaplayerpro.app.ui.cassette.DeckFace
import com.mediaplayerpro.app.ui.cassette.DeckUi
import com.mediaplayerpro.app.ui.cassette.KeyStyle
import com.mediaplayerpro.app.ui.cassette.MechKeyRow
import com.mediaplayerpro.app.ui.cassette.PlayerSkin
import com.mediaplayerpro.app.ui.cassette.PocketFace
import com.mediaplayerpro.app.ui.cassette.TapeFace
import com.mediaplayerpro.app.ui.cassette.rememberDeckAnim
import com.mediaplayerpro.app.ui.components.PlayerExtras
import com.mediaplayerpro.app.ui.components.PlayerTopBar
import com.mediaplayerpro.app.ui.components.SeekBar
import com.mediaplayerpro.app.ui.components.SkinPicker
import com.mediaplayerpro.app.ui.theme.AppColors

private class SkinSpec(
    val body: Brush,
    val shape: Shape,
    val rim: Color,
    val keys: KeyStyle,
    val widthFraction: Float,
    val maxWidth: Dp,
    val padding: PaddingValues,
    val elevation: Dp,
)

private fun specOf(skin: PlayerSkin): SkinSpec = when (skin) {
    PlayerSkin.BOOMBOX -> SkinSpec(
        body = Brush.verticalGradient(listOf(Color(0xFFE4E7EB), Color(0xFFB4BAC2), Color(0xFFCDD2D8))),
        shape = RoundedCornerShape(22.dp),
        rim = Color(0xFF8A9099),
        keys = KeyStyle(Color(0xFFF4F5F7), Color(0xFFB3B8C0), Color(0xFF2B2F36), Color(0xFF6E747D), Color(0xFF2B2F36)),
        widthFraction = 1f, maxWidth = 600.dp,
        padding = PaddingValues(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 12.dp),
        elevation = 18.dp,
    )
    PlayerSkin.DECK -> SkinSpec(
        body = Brush.verticalGradient(listOf(Color(0xFF2E2F33), Color(0xFF141518))),
        shape = RoundedCornerShape(10.dp),
        rim = Color(0xFF4A4C52),
        keys = KeyStyle(Color(0xFFD5D8DD), Color(0xFF8E9298), Color(0xFF16171A), Color(0xFF3A3C40), Color(0xFF16171A)),
        widthFraction = 1f, maxWidth = 520.dp,
        padding = PaddingValues(start = 10.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
        elevation = 18.dp,
    )
    PlayerSkin.POCKET -> SkinSpec(
        body = Brush.verticalGradient(listOf(Color(0xFF3468B3), Color(0xFF173A6E))),
        shape = RoundedCornerShape(26.dp),
        rim = Color(0xFF8FB3E8),
        keys = KeyStyle(Color(0xFFEDF1F7), Color(0xFFAFB9C7), Color(0xFF173A6E), Color(0xFF0E2547), Color(0xFF173A6E)),
        widthFraction = 0.9f, maxWidth = 420.dp,
        padding = PaddingValues(start = 10.dp, end = 10.dp, top = 12.dp, bottom = 14.dp),
        elevation = 18.dp,
    )
    PlayerSkin.TAPE, PlayerSkin.MODERN -> SkinSpec(
        body = Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent)),
        shape = RoundedCornerShape(16.dp),
        rim = Color.Transparent,
        keys = KeyStyle(Color(0xFF3A3E46), Color(0xFF1F2227), Color(0xFFF1F5F9), Color(0xFF050607), Color(0xFF94A3B8)),
        widthFraction = 1f, maxWidth = 600.dp,
        padding = PaddingValues(0.dp),
        elevation = 0.dp,
    )
}

@Composable
fun CassettePlayerScreen(vm: MainViewModel, skin: PlayerSkin) {
    val s by vm.player.collectAsStateWithLifecycle()
    val position by vm.position.collectAsStateWithLifecycle()
    var wind by remember { mutableIntStateOf(0) }
    val progress = if (s.durationMs > 0) (position.toFloat() / s.durationMs).coerceIn(0f, 1f) else 0f
    val anim = rememberDeckAnim(s.isPlaying, wind, progress)
    val ui = DeckUi(
        title = s.current?.title ?: "Chưa chọn bài",
        artist = s.current?.artist.orEmpty(),
        progress = progress,
        positionMs = position,
        durationMs = s.durationMs,
        playing = s.isPlaying,
    )
    val spec = specOf(skin)

    BackHandler { vm.backToLibrary() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1B2232), AppColors.Bg)))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PlayerTopBar(vm, s)
            SkinPicker(skin, vm::setSkin)
            Spacer(Modifier.height(16.dp))

            Column(
                Modifier
                    .widthIn(max = spec.maxWidth)
                    .fillMaxWidth(spec.widthFraction)
                    .shadow(spec.elevation, spec.shape)
                    .background(spec.body, spec.shape)
                    .border(1.dp, spec.rim, spec.shape)
                    .padding(spec.padding)
            ) {
                when (skin) {
                    PlayerSkin.BOOMBOX -> BoomboxFace(ui, anim)
                    PlayerSkin.DECK -> DeckFace(ui, anim)
                    PlayerSkin.POCKET -> PocketFace(ui, anim)
                    else -> TapeFace(ui, anim)
                }
                Spacer(Modifier.height(10.dp))
                MechKeyRow(
                    style = spec.keys,
                    playing = s.isPlaying,
                    paused = !s.isPlaying && s.current != null && position > 0,
                    onPrev = vm::previous,
                    onNext = vm::next,
                    onPlay = vm::play,
                    onStop = vm::stop,
                    onPause = vm::togglePlayPause,
                    onSeek = vm::seekBy,
                    onWind = { wind = it },
                )
            }

            Spacer(Modifier.height(14.dp))
            SeekBar(vm, position, s.durationMs)
            PlayerExtras(vm, s, showModes = true)
            Spacer(Modifier.height(16.dp))
        }
    }
}
