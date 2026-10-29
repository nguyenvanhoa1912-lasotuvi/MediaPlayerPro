package com.mediaplayerpro.app.ui

import android.app.Application
import android.content.ComponentName
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.mediaplayerpro.app.data.MediaFile
import com.mediaplayerpro.app.data.MediaRepository
import com.mediaplayerpro.app.playback.PlaybackService
import com.mediaplayerpro.app.ui.util.foldVi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.Collator
import java.util.Locale

enum class Screen { LIBRARY, NOW_PLAYING, VIDEO }

enum class SortMode(val label: String) {
    DATE("Mới thêm gần đây"),
    TITLE("Tên A → Z"),
    DURATION("Thời lượng dài nhất"),
    SIZE("Dung lượng lớn nhất"),
}

data class PlayerUiState(
    val current: MediaFile? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val durationMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val speed: Float = 1f,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val sleepRemainingMs: Long? = null,
    val queueSize: Int = 0,
    val queueIndex: Int = 0,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = MediaRepository(app)
    private val collator = Collator.getInstance(Locale("vi", "VN"))

    private val _audio = MutableStateFlow<List<MediaFile>>(emptyList())
    val audio: StateFlow<List<MediaFile>> = _audio.asStateFlow()
    private val _videos = MutableStateFlow<List<MediaFile>>(emptyList())
    val videos: StateFlow<List<MediaFile>> = _videos.asStateFlow()

    val query = MutableStateFlow("")
    val sortMode = MutableStateFlow(SortMode.DATE)
    val screen = MutableStateFlow(Screen.LIBRARY)
    val inPip = MutableStateFlow(false)
    val hasPermission = MutableStateFlow(false)
    val loading = MutableStateFlow(false)

    val filteredAudio: StateFlow<List<MediaFile>> =
        combine(_audio, query, sortMode) { l, q, s -> filterSort(l, q, s) }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val filteredVideos: StateFlow<List<MediaFile>> =
        combine(_videos, query, sortMode) { l, q, s -> filterSort(l, q, s) }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _player = MutableStateFlow(PlayerUiState())
    val player: StateFlow<PlayerUiState> = _player.asStateFlow()

    /** Vị trí phát tách riêng để tránh recompose toàn màn hình mỗi 0,5 giây. */
    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

    private val _controller = MutableStateFlow<MediaController?>(null)
    val controller: StateFlow<MediaController?> = _controller.asStateFlow()

    private val index = HashMap<String, MediaFile>()
    private var pendingExternal: Pair<Uri, String?>? = null
    private var sleepEndAt: Long? = null
    private var reloadJob: Job? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = syncState()
    }

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = scheduleReload()
    }

    init {
        connectController()
        app.contentResolver.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, observer)
        app.contentResolver.registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, observer)
        viewModelScope.launch {
            while (isActive) {
                delay(500)
                tick()
            }
        }
    }

    // ---------------- Kết nối MediaSession ----------------

    private fun connectController() {
        val app = getApplication<Application>()
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        viewModelScope.launch {
            runCatching {
                val c = MediaController.Builder(app, token).buildAsync().await()
                c.addListener(listener)
                _controller.value = c
                syncState()
                pendingExternal?.let { (u, t) ->
                    pendingExternal = null
                    playExternal(u, t)
                }
            }
        }
    }

    private fun syncState() {
        val c = _controller.value ?: return
        val item = c.currentMediaItem
        val file = item?.let { index[it.mediaId] ?: it.toMediaFile() }
        val dur = c.duration
        _position.value = c.currentPosition.coerceAtLeast(0)
        _player.update { s ->
            s.copy(
                current = file,
                isPlaying = c.isPlaying,
                isBuffering = c.playbackState == Player.STATE_BUFFERING,
                durationMs = if (dur == C.TIME_UNSET) (file?.durationMs ?: 0L) else dur.coerceAtLeast(0),
                shuffle = c.shuffleModeEnabled,
                repeatMode = c.repeatMode,
                speed = c.playbackParameters.speed,
                videoWidth = c.videoSize.width,
                videoHeight = c.videoSize.height,
                queueSize = c.mediaItemCount,
                queueIndex = c.currentMediaItemIndex,
            )
        }
    }

    private fun tick() {
        val c = _controller.value ?: return
        if (c.isPlaying) _position.value = c.currentPosition.coerceAtLeast(0)
        val end = sleepEndAt ?: return
        val remain = (end - System.currentTimeMillis()).coerceAtLeast(0)
        if (remain == 0L) sleepEndAt = null
        _player.update { it.copy(sleepRemainingMs = if (remain == 0L) null else remain) }
    }

    // ---------------- Thư viện ----------------

    fun onPermissionResult(granted: Boolean) {
        hasPermission.value = granted
        if (granted) reload()
    }

    fun reload() {
        if (!hasPermission.value) return
        viewModelScope.launch {
            loading.value = true
            try {
                val a = repo.loadAudio()
                val v = repo.loadVideos()
                (a + v).forEach { index[it.uri.toString()] = it }
                _audio.value = a
                _videos.value = v
            } finally {
                loading.value = false
            }
        }
    }

    private fun scheduleReload() {
        reloadJob?.cancel()
        reloadJob = viewModelScope.launch {
            delay(1500)
            reload()
        }
    }

    private fun filterSort(list: List<MediaFile>, q: String, s: SortMode): List<MediaFile> {
        val needle = q.trim().foldVi()
        val filtered = if (needle.isEmpty()) list else list.filter {
            it.title.foldVi().contains(needle) ||
                it.artist.foldVi().contains(needle) ||
                it.album.foldVi().contains(needle)
        }
        return when (s) {
            SortMode.DATE -> filtered.sortedByDescending { it.dateAddedSec }
            SortMode.TITLE -> filtered.sortedWith { a, b -> collator.compare(a.title, b.title) }
            SortMode.DURATION -> filtered.sortedByDescending { it.durationMs }
            SortMode.SIZE -> filtered.sortedByDescending { it.sizeBytes }
        }
    }

    // ---------------- Điều khiển phát ----------------

    fun playList(list: List<MediaFile>, start: Int, shuffle: Boolean = false) {
        val c = _controller.value ?: return
        if (list.isEmpty()) return
        val startIndex = if (shuffle) list.indices.random() else start.coerceIn(list.indices)
        c.shuffleModeEnabled = shuffle
        c.setMediaItems(list.map { it.toMediaItem() }, startIndex, 0L)
        c.prepare()
        c.play()
        if (list[startIndex].isVideo) screen.value = Screen.VIDEO
    }

    fun playExternal(uri: Uri, mimeType: String?) {
        if (_controller.value == null) {
            pendingExternal = uri to mimeType
            return
        }
        val app = getApplication<Application>()
        val type = mimeType
            ?: runCatching { app.contentResolver.getType(uri) }.getOrNull()
            ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                MimeTypeMap.getFileExtensionFromUrl(uri.toString()).lowercase()
            )
        val isVideo = type?.startsWith("video") == true
        val name = runCatching {
            app.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { if (it.moveToFirst()) it.getString(0) else null }
        }.getOrNull() ?: uri.lastPathSegment ?: "Tệp phương tiện"

        val file = MediaFile(
            id = -1, uri = uri, title = name.substringBeforeLast('.'), artist = "",
            album = "", albumId = -1, durationMs = 0, sizeBytes = 0, dateAddedSec = 0,
            isVideo = isVideo
        )
        index[uri.toString()] = file
        playList(listOf(file), 0)
        if (!isVideo) screen.value = Screen.NOW_PLAYING
    }

    fun togglePlayPause() {
        val c = _controller.value ?: return
        if (c.isPlaying) {
            c.pause()
            return
        }
        when (c.playbackState) {
            Player.STATE_IDLE -> c.prepare()
            Player.STATE_ENDED -> c.seekTo(c.currentMediaItemIndex, 0L)
        }
        c.play()
    }

    fun pause() { _controller.value?.pause() }
    fun next() { _controller.value?.seekToNext() }
    fun previous() { _controller.value?.seekToPrevious() }

    fun seekTo(ms: Long) {
        _controller.value?.seekTo(ms)
        _position.value = ms
    }

    fun toggleShuffle() {
        val c = _controller.value ?: return
        c.shuffleModeEnabled = !c.shuffleModeEnabled
    }

    fun cycleRepeat() {
        val c = _controller.value ?: return
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun setSpeed(speed: Float) { _controller.value?.setPlaybackSpeed(speed) }

    /** Hẹn giờ tắt chạy trong Service nên vẫn hoạt động khi đã đóng giao diện. */
    fun setSleepTimer(minutes: Int) {
        _controller.value?.sendCustomCommand(
            SessionCommand(PlaybackService.CMD_SLEEP_TIMER, Bundle.EMPTY),
            Bundle().apply { putInt(PlaybackService.ARG_MINUTES, minutes) }
        )
        sleepEndAt = if (minutes > 0) System.currentTimeMillis() + minutes * 60_000L else null
        _player.update { it.copy(sleepRemainingMs = sleepEndAt?.let { e -> e - System.currentTimeMillis() }) }
    }

    // ---------------- Điều hướng ----------------

    fun openPlayer() {
        screen.value = if (_player.value.current?.isVideo == true) Screen.VIDEO else Screen.NOW_PLAYING
    }

    fun backToLibrary() { screen.value = Screen.LIBRARY }

    fun closeVideo() {
        _controller.value?.pause()
        screen.value = Screen.LIBRARY
    }

    override fun onCleared() {
        getApplication<Application>().contentResolver.unregisterContentObserver(observer)
        _controller.value?.run {
            removeListener(listener)
            release()
        }
        super.onCleared()
    }

    // ---------------- Chuyển đổi ----------------

    private fun MediaFile.toMediaItem(): MediaItem =
        MediaItem.Builder()
            .setMediaId(uri.toString())
            .setUri(uri)
            .setRequestMetadata(MediaItem.RequestMetadata.Builder().setMediaUri(uri).build())
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist.ifBlank { null })
                    .setAlbumTitle(album.ifBlank { null })
                    .setArtworkUri(artworkUri)
                    .setMediaType(if (isVideo) MediaMetadata.MEDIA_TYPE_VIDEO else MediaMetadata.MEDIA_TYPE_MUSIC)
                    .build()
            )
            .build()

    private fun MediaItem.toMediaFile(): MediaFile {
        val md = mediaMetadata
        val u = localConfiguration?.uri ?: requestMetadata.mediaUri ?: Uri.parse(mediaId)
        return MediaFile(
            id = -1, uri = u,
            title = md.title?.toString() ?: "Không rõ tiêu đề",
            artist = md.artist?.toString().orEmpty(),
            album = md.albumTitle?.toString().orEmpty(),
            albumId = -1, durationMs = 0, sizeBytes = 0, dateAddedSec = 0,
            isVideo = md.mediaType == MediaMetadata.MEDIA_TYPE_VIDEO
        )
    }
}
