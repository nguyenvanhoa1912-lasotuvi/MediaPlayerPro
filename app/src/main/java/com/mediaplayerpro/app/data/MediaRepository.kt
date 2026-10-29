package com.mediaplayerpro.app.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Immutable
data class MediaFile(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAddedSec: Long,
    val isVideo: Boolean,
    val width: Int = 0,
    val height: Int = 0,
) {
    val artworkUri: Uri?
        get() = if (!isVideo && albumId > 0) ContentUris.withAppendedId(ALBUM_ART_URI, albumId) else null

    companion object {
        private val ALBUM_ART_URI: Uri = Uri.parse("content://media/external/audio/albumart")
    }
}

/** Tên cột MediaStore dạng chuỗi – tránh xung đột hằng số giữa các API level (lint NewApi). */
private object Col {
    const val ID = "_id"
    const val TITLE = "title"
    const val DISPLAY_NAME = "_display_name"
    const val ARTIST = "artist"
    const val ALBUM = "album"
    const val ALBUM_ID = "album_id"
    const val DURATION = "duration"
    const val SIZE = "_size"
    const val DATE_ADDED = "date_added"
    const val WIDTH = "width"
    const val HEIGHT = "height"
}

class MediaRepository(private val context: Context) {

    private val audioCollection: Uri =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

    private val videoCollection: Uri =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        else MediaStore.Video.Media.EXTERNAL_CONTENT_URI

    suspend fun loadAudio(): List<MediaFile> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            Col.ID, Col.TITLE, Col.DISPLAY_NAME, Col.ARTIST, Col.ALBUM,
            Col.ALBUM_ID, Col.DURATION, Col.SIZE, Col.DATE_ADDED
        )
        val out = ArrayList<MediaFile>()
        runCatching {
            context.contentResolver.query(
                audioCollection, projection, "${Col.DURATION} > 0", null, "${Col.DATE_ADDED} DESC"
            )?.use { c ->
                val iId = c.getColumnIndexOrThrow(Col.ID)
                val iTitle = c.getColumnIndexOrThrow(Col.TITLE)
                val iName = c.getColumnIndexOrThrow(Col.DISPLAY_NAME)
                val iArtist = c.getColumnIndexOrThrow(Col.ARTIST)
                val iAlbum = c.getColumnIndexOrThrow(Col.ALBUM)
                val iAlbumId = c.getColumnIndexOrThrow(Col.ALBUM_ID)
                val iDur = c.getColumnIndexOrThrow(Col.DURATION)
                val iSize = c.getColumnIndexOrThrow(Col.SIZE)
                val iDate = c.getColumnIndexOrThrow(Col.DATE_ADDED)
                while (c.moveToNext()) {
                    val id = c.getLong(iId)
                    val name = c.getString(iName).orEmpty()
                    out += MediaFile(
                        id = id,
                        uri = ContentUris.withAppendedId(audioCollection, id),
                        title = c.getString(iTitle).clean() ?: name.substringBeforeLast('.'),
                        artist = c.getString(iArtist).clean() ?: "Không rõ nghệ sĩ",
                        album = c.getString(iAlbum).clean().orEmpty(),
                        albumId = c.getLong(iAlbumId),
                        durationMs = c.getLong(iDur),
                        sizeBytes = c.getLong(iSize),
                        dateAddedSec = c.getLong(iDate),
                        isVideo = false
                    )
                }
            }
        }
        out
    }

    suspend fun loadVideos(): List<MediaFile> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            Col.ID, Col.TITLE, Col.DISPLAY_NAME, Col.DURATION, Col.SIZE,
            Col.DATE_ADDED, Col.WIDTH, Col.HEIGHT
        )
        val out = ArrayList<MediaFile>()
        runCatching {
            context.contentResolver.query(
                videoCollection, projection, "${Col.DURATION} > 0", null, "${Col.DATE_ADDED} DESC"
            )?.use { c ->
                val iId = c.getColumnIndexOrThrow(Col.ID)
                val iTitle = c.getColumnIndexOrThrow(Col.TITLE)
                val iName = c.getColumnIndexOrThrow(Col.DISPLAY_NAME)
                val iDur = c.getColumnIndexOrThrow(Col.DURATION)
                val iSize = c.getColumnIndexOrThrow(Col.SIZE)
                val iDate = c.getColumnIndexOrThrow(Col.DATE_ADDED)
                val iW = c.getColumnIndexOrThrow(Col.WIDTH)
                val iH = c.getColumnIndexOrThrow(Col.HEIGHT)
                while (c.moveToNext()) {
                    val id = c.getLong(iId)
                    val name = c.getString(iName).orEmpty()
                    out += MediaFile(
                        id = id,
                        uri = ContentUris.withAppendedId(videoCollection, id),
                        title = name.substringBeforeLast('.').ifBlank { c.getString(iTitle).clean() ?: "Video" },
                        artist = "",
                        album = "",
                        albumId = -1,
                        durationMs = c.getLong(iDur),
                        sizeBytes = c.getLong(iSize),
                        dateAddedSec = c.getLong(iDate),
                        isVideo = true,
                        width = c.getInt(iW),
                        height = c.getInt(iH)
                    )
                }
            }
        }
        out
    }

    private fun String?.clean(): String? =
        this?.trim()?.takeIf { it.isNotEmpty() && it != MediaStore.UNKNOWN_STRING }
}
