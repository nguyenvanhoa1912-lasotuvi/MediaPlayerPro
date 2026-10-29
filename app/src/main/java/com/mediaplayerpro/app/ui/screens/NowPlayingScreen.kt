package com.mediaplayerpro.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import com.mediaplayerpro.app.ui.MainViewModel
import com.mediaplayerpro.app.ui.components.MediaThumbnail
import com.mediaplayerpro.app.ui.theme.AppColors
import com.mediaplayerpro.app.ui.util.formatSpeed
import com.mediaplayerpro.app.ui.util.formatTime

private val SPEEDS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)
private val SLEEP_OPTIONS = listOf(15, 30, 45, 60, 90)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NowPlayingScreen(vm: MainViewModel) {
    val s by vm.player.collectAsStateWithLifecycle()
    val position by vm.position.collectAsStateWithLifecycle()
    val file = s.current
    var dragging by remember { mutableStateOf<Float?>(null) }
    var speedMenu by remember { mutableStateOf(false) }
    var sleepMenu by remember { mutableStateOf(false) }

    BackHandler { vm.backToLibrary() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF2A1A08), AppColors.Bg, AppColors.Bg)))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = vm::backToLibrary) {
                    Icon(Icons.Rounded.KeyboardArrowDown, "Thu nhỏ", modifier = Modifier.size(32.dp))
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ĐANG PHÁT", style = MaterialTheme.typography.labelMedium, color = AppColors.Muted, letterSpacing = 2.sp)
                    if (s.queueSize > 0) {
                        Text(
                            "${s.queueIndex + 1} / ${s.queueSize}",
                            style = MaterialTheme.typography.labelSmall, color = AppColors.Muted
                        )
                    }
                }
                Spacer(Modifier.size(48.dp))
            }

            Spacer(Modifier.weight(0.5f))
            if (file != null) {
                MediaThumbnail(
                    file,
                    Modifier
                        .widthIn(max = 420.dp)
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .shadow(24.dp, RoundedCornerShape(20.dp)),
                    sizePx = 720, corner = 20.dp, iconSize = 96.dp
                )
            }
            Spacer(Modifier.weight(0.5f))

            Text(
                file?.title ?: "Chưa phát",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .basicMarquee()
            )
            val sub = listOfNotNull(file?.artist, file?.album).filter { it.isNotBlank() }.joinToString(" • ")
            if (sub.isNotBlank()) {
                Text(sub, color = AppColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Spacer(Modifier.height(16.dp))
            val dur = s.durationMs.coerceAtLeast(1L).toFloat()
            val pos = dragging ?: position.toFloat()
            Slider(
                value = pos.coerceIn(0f, dur),
                onValueChange = { dragging = it },
                onValueChangeFinished = {
                    dragging?.let { vm.seekTo(it.toLong()) }
                    dragging = null
                },
                valueRange = 0f..dur,
                colors = SliderDefaults.colors(
                    thumbColor = AppColors.Accent,
                    activeTrackColor = AppColors.Accent,
                    inactiveTrackColor = AppColors.Divider
                )
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(pos.toLong()), style = MaterialTheme.typography.labelMedium, color = AppColors.Muted)
                Text(formatTime(s.durationMs), style = MaterialTheme.typography.labelMedium, color = AppColors.Muted)
            }

            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = vm::toggleShuffle) {
                    Icon(Icons.Rounded.Shuffle, "Trộn bài", tint = if (s.shuffle) AppColors.Accent else AppColors.Muted)
                }
                IconButton(onClick = vm::previous, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Rounded.SkipPrevious, "Bài trước", modifier = Modifier.size(40.dp))
                }
                FilledIconButton(
                    onClick = vm::togglePlayPause,
                    modifier = Modifier.size(76.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = AppColors.Accent, contentColor = AppColors.OnAccent
                    )
                ) {
                    if (s.isBuffering) {
                        CircularProgressIndicator(Modifier.size(32.dp), color = AppColors.OnAccent, strokeWidth = 3.dp)
                    } else {
                        Icon(
                            if (s.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            if (s.isPlaying) "Tạm dừng" else "Phát",
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
                IconButton(onClick = vm::next, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.Rounded.SkipNext, "Bài tiếp", modifier = Modifier.size(40.dp))
                }
                IconButton(onClick = vm::cycleRepeat) {
                    Icon(
                        if (s.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                        "Lặp lại",
                        tint = if (s.repeatMode == Player.REPEAT_MODE_OFF) AppColors.Muted else AppColors.Accent
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
            ) {
                Box {
                    AssistChip(
                        onClick = { speedMenu = true },
                        label = { Text("Tốc độ ${formatSpeed(s.speed)}") },
                        leadingIcon = { Icon(Icons.Rounded.Speed, null, tint = AppColors.Accent2) },
                        colors = AssistChipDefaults.assistChipColors(labelColor = AppColors.Text)
                    )
                    DropdownMenu(expanded = speedMenu, onDismissRequest = { speedMenu = false }) {
                        SPEEDS.forEach { sp ->
                            DropdownMenuItem(
                                text = { Text(formatSpeed(sp), color = if (sp == s.speed) AppColors.Accent else AppColors.Text) },
                                onClick = { vm.setSpeed(sp); speedMenu = false }
                            )
                        }
                    }
                }
                Box {
                    AssistChip(
                        onClick = { sleepMenu = true },
                        label = { Text(s.sleepRemainingMs?.let { "Tắt sau ${formatTime(it)}" } ?: "Hẹn giờ tắt") },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Bedtime, null,
                                tint = if (s.sleepRemainingMs != null) AppColors.Accent else AppColors.Accent2
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(labelColor = AppColors.Text)
                    )
                    DropdownMenu(expanded = sleepMenu, onDismissRequest = { sleepMenu = false }) {
                        SLEEP_OPTIONS.forEach { m ->
                            DropdownMenuItem(text = { Text("$m phút") }, onClick = { vm.setSleepTimer(m); sleepMenu = false })
                        }
                        if (s.sleepRemainingMs != null) {
                            DropdownMenuItem(
                                text = { Text("Huỷ hẹn giờ", color = AppColors.Accent) },
                                onClick = { vm.setSleepTimer(0); sleepMenu = false }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.weight(0.3f))
        }
    }
}
