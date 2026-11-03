package com.mediaplayerpro.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.mediaplayerpro.app.ui.MainViewModel
import com.mediaplayerpro.app.ui.PlayerUiState
import com.mediaplayerpro.app.ui.cassette.PlayerSkin
import com.mediaplayerpro.app.ui.theme.AppColors
import com.mediaplayerpro.app.ui.util.formatSpeed
import com.mediaplayerpro.app.ui.util.formatTime

private val SPEEDS = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)
private val SLEEP_OPTIONS = listOf(15, 30, 45, 60, 90)

@Composable
fun PlayerTopBar(vm: MainViewModel, s: PlayerUiState) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = vm::backToLibrary) {
            Icon(Icons.Rounded.KeyboardArrowDown, "Thu nhỏ", modifier = Modifier.size(32.dp))
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("ĐANG PHÁT", style = MaterialTheme.typography.labelMedium, color = AppColors.Muted, letterSpacing = 2.sp)
            if (s.queueSize > 0) {
                Text("${s.queueIndex + 1} / ${s.queueSize}", style = MaterialTheme.typography.labelSmall, color = AppColors.Muted)
            }
        }
        Spacer(Modifier.size(48.dp))
    }
}

/** Hàng chọn giao diện: 4 mẫu cassette + Hiện đại. */
@Composable
fun SkinPicker(current: PlayerSkin, onSelect: (PlayerSkin) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PlayerSkin.entries.forEach { skin ->
            FilterChip(
                selected = skin == current,
                onClick = { onSelect(skin) },
                label = { Text(skin.label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AppColors.Accent,
                    selectedLabelColor = AppColors.OnAccent,
                    labelColor = AppColors.Text,
                )
            )
        }
    }
}

@Composable
fun SeekBar(vm: MainViewModel, position: Long, durationMs: Long) {
    var dragging by remember { mutableStateOf<Float?>(null) }
    val dur = durationMs.coerceAtLeast(1L).toFloat()
    val pos = dragging ?: position.toFloat()
    Column(Modifier.fillMaxWidth()) {
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
            Text(formatTime(durationMs), style = MaterialTheme.typography.labelMedium, color = AppColors.Muted)
        }
    }
}

/** Tốc độ phát, hẹn giờ tắt; tuỳ chọn thêm trộn bài / lặp lại. */
@Composable
fun PlayerExtras(vm: MainViewModel, s: PlayerUiState, showModes: Boolean) {
    var speedMenu by remember { mutableStateOf(false) }
    var sleepMenu by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showModes) {
            IconButton(onClick = vm::toggleShuffle) {
                Icon(Icons.Rounded.Shuffle, "Trộn bài", tint = if (s.shuffle) AppColors.Accent else AppColors.Muted)
            }
            IconButton(onClick = vm::cycleRepeat) {
                Icon(
                    if (s.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                    "Lặp lại",
                    tint = if (s.repeatMode == Player.REPEAT_MODE_OFF) AppColors.Muted else AppColors.Accent
                )
            }
        }
        Box {
            AssistChip(
                onClick = { speedMenu = true },
                label = { Text(formatSpeed(s.speed)) },
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
                label = { Text(s.sleepRemainingMs?.let { "Tắt sau ${formatTime(it)}" } ?: "Hẹn giờ") },
                leadingIcon = {
                    Icon(Icons.Rounded.Bedtime, null, tint = if (s.sleepRemainingMs != null) AppColors.Accent else AppColors.Accent2)
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
}
