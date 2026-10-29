package com.mediaplayerpro.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mediaplayerpro.app.data.MediaFile
import com.mediaplayerpro.app.ui.MainViewModel
import com.mediaplayerpro.app.ui.PlayerUiState
import com.mediaplayerpro.app.ui.SortMode
import com.mediaplayerpro.app.ui.components.MediaThumbnail
import com.mediaplayerpro.app.ui.theme.AppColors
import com.mediaplayerpro.app.ui.util.formatSize
import com.mediaplayerpro.app.ui.util.formatTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    vm: MainViewModel,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val audio by vm.filteredAudio.collectAsStateWithLifecycle()
    val videos by vm.filteredVideos.collectAsStateWithLifecycle()
    val totalAudio by vm.audio.collectAsStateWithLifecycle()
    val totalVideo by vm.videos.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val sort by vm.sortMode.collectAsStateWithLifecycle()
    val hasPerm by vm.hasPermission.collectAsStateWithLifecycle()
    val loading by vm.loading.collectAsStateWithLifecycle()
    val player by vm.player.collectAsStateWithLifecycle()

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    var sortMenu by remember { mutableStateOf(false) }

    BackHandler(enabled = searchOpen) {
        searchOpen = false
        vm.query.value = ""
    }

    Scaffold(
        containerColor = AppColors.Bg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppColors.Bg),
                title = {
                    if (searchOpen) {
                        TextField(
                            value = query,
                            onValueChange = { vm.query.value = it },
                            placeholder = { Text("Tìm bài hát, nghệ sĩ, video…") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = AppColors.Accent,
                                unfocusedIndicatorColor = AppColors.Divider,
                                cursorColor = AppColors.Accent,
                            )
                        )
                    } else {
                        Column {
                            Text("Media Player Pro", fontWeight = FontWeight.ExtraBold)
                            Text(
                                "${totalAudio.size} bài hát • ${totalVideo.size} video",
                                style = MaterialTheme.typography.labelMedium,
                                color = AppColors.Muted
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (searchOpen) vm.query.value = ""
                        searchOpen = !searchOpen
                    }) {
                        Icon(if (searchOpen) Icons.Rounded.Close else Icons.Rounded.Search, "Tìm kiếm")
                    }
                    Box {
                        IconButton(onClick = { sortMenu = true }) {
                            Icon(Icons.AutoMirrored.Rounded.Sort, "Sắp xếp")
                        }
                        DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                            SortMode.entries.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(m.label) },
                                    onClick = { vm.sortMode.value = m; sortMenu = false },
                                    leadingIcon = {
                                        if (m == sort) Icon(Icons.Rounded.Check, null, tint = AppColors.Accent)
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = vm::reload) { Icon(Icons.Rounded.Refresh, "Quét lại") }
                }
            )
        },
        bottomBar = {
            player.current?.let { MiniPlayer(vm, it, player) }
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            TabRow(selectedTabIndex = tab, containerColor = AppColors.Bg, contentColor = AppColors.Accent) {
                Tab(
                    selected = tab == 0, onClick = { tab = 0 },
                    text = { Text("Âm nhạc (${audio.size})") },
                    icon = { Icon(Icons.Rounded.LibraryMusic, null) },
                    unselectedContentColor = AppColors.Muted
                )
                Tab(
                    selected = tab == 1, onClick = { tab = 1 },
                    text = { Text("Video (${videos.size})") },
                    icon = { Icon(Icons.Rounded.VideoLibrary, null) },
                    unselectedContentColor = AppColors.Muted
                )
            }
            val list = if (tab == 0) audio else videos
            when {
                !hasPerm -> PermissionCard(onRequestPermission, onOpenSettings)
                loading && list.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppColors.Accent)
                }
                list.isEmpty() -> EmptyState(
                    when {
                        query.isNotBlank() -> "Không tìm thấy kết quả phù hợp"
                        tab == 0 -> "Chưa có tệp âm thanh trên thiết bị"
                        else -> "Chưa có video trên thiết bị"
                    }
                )
                else -> MediaList(
                    list = list,
                    currentUri = player.current?.uri?.toString(),
                    isPlaying = player.isPlaying,
                    onPlay = { i -> vm.playList(list, i) },
                    onShuffle = { vm.playList(list, 0, shuffle = true) }
                )
            }
        }
    }
}

@Composable
private fun MediaList(
    list: List<MediaFile>,
    currentUri: String?,
    isPlaying: Boolean,
    onPlay: (Int) -> Unit,
    onShuffle: () -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 12.dp)) {
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onPlay(0) },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent, contentColor = AppColors.OnAccent)
                ) {
                    Icon(Icons.Rounded.PlayArrow, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Phát tất cả", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(onClick = onShuffle) {
                    Icon(Icons.Rounded.Shuffle, null, tint = AppColors.Accent2)
                    Spacer(Modifier.width(6.dp))
                    Text("Trộn bài", color = AppColors.Text)
                }
            }
        }
        itemsIndexed(list, key = { _, f -> f.uri.toString() }) { i, f ->
            MediaRow(
                file = f,
                active = f.uri.toString() == currentUri,
                playing = isPlaying,
                onClick = { onPlay(i) }
            )
        }
    }
}

@Composable
private fun MediaRow(file: MediaFile, active: Boolean, playing: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (active) AppColors.Accent.copy(alpha = 0.10f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val thumbModifier = if (file.isVideo) Modifier.size(width = 112.dp, height = 63.dp) else Modifier.size(52.dp)
        Box(thumbModifier) {
            MediaThumbnail(file, Modifier.fillMaxSize())
            if (file.isVideo) {
                Text(
                    formatTime(file.durationMs),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                    fontSize = 10.sp,
                    color = Color.White
                )
            }
            if (active) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(if (playing) Icons.Rounded.GraphicEq else Icons.Rounded.Pause, null, tint = AppColors.Accent)
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                file.title,
                maxLines = if (file.isVideo) 2 else 1,
                overflow = TextOverflow.Ellipsis,
                color = if (active) AppColors.Accent else AppColors.Text,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyLarge
            )
            val subtitle = if (file.isVideo) {
                listOfNotNull(
                    formatSize(file.sizeBytes).ifBlank { null },
                    if (file.width > 0 && file.height > 0) "${file.width}×${file.height}" else null
                ).joinToString(" • ")
            } else {
                listOf(file.artist, file.album).filter { it.isNotBlank() }.joinToString(" • ")
            }
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle, style = MaterialTheme.typography.bodySmall, color = AppColors.Muted,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (!file.isVideo) {
            Spacer(Modifier.width(8.dp))
            Text(formatTime(file.durationMs), style = MaterialTheme.typography.labelMedium, color = AppColors.Muted)
        }
    }
}

@Composable
private fun MiniPlayer(vm: MainViewModel, file: MediaFile, state: PlayerUiState) {
    val position by vm.position.collectAsStateWithLifecycle()
    Surface(color = AppColors.SurfaceHigh, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.navigationBarsPadding()) {
            val progress = if (state.durationMs > 0) position.toFloat() / state.durationMs else 0f
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = AppColors.Accent,
                trackColor = AppColors.Divider,
                gapSize = 0.dp,
                drawStopIndicator = {}
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = vm::openPlayer)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MediaThumbnail(file, Modifier.size(44.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(file.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (file.isVideo) "Video • chạm để mở" else file.artist.ifBlank { "Đang phát" },
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall, color = AppColors.Muted
                    )
                }
                IconButton(onClick = vm::togglePlayPause) {
                    Icon(
                        if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        if (state.isPlaying) "Tạm dừng" else "Phát",
                        modifier = Modifier.size(30.dp)
                    )
                }
                IconButton(onClick = vm::next) { Icon(Icons.Rounded.SkipNext, "Bài tiếp") }
            }
        }
    }
}

@Composable
private fun PermissionCard(onRequest: () -> Unit, onSettings: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Rounded.Lock, null, tint = AppColors.Accent, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text("Cần quyền truy cập thư viện", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Ứng dụng cần quyền đọc âm thanh và video để quét thư viện trên thiết bị. " +
                "Dữ liệu chỉ xử lý cục bộ, không tải lên máy chủ.",
            color = AppColors.Muted, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onRequest,
            colors = ButtonDefaults.buttonColors(containerColor = AppColors.Accent, contentColor = AppColors.OnAccent)
        ) { Text("Cấp quyền", fontWeight = FontWeight.Bold) }
        TextButton(onClick = onSettings) { Text("Mở Cài đặt ứng dụng", color = AppColors.Accent2) }
    }
}

@Composable
private fun EmptyState(message: String) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Rounded.LibraryMusic, null, tint = AppColors.Divider, modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(12.dp))
        Text(message, color = AppColors.Muted)
    }
}
