package com.sonora.player.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore

data class RawSong(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val track: Int,
    val year: Int,
    val durationMs: Long,
    val dateAdded: Long,
)

/** Lee las canciones guardadas en el teléfono usando el índice de Android (MediaStore). */
object MediaScanner {

    fun scan(context: Context): List<RawSong> {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
        )
        // Solo música (sin tonos ni notas de voz cortas)
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= ?"
        val args = arrayOf("20000")
        val result = ArrayList<RawSong>()

        try {
            context.contentResolver.query(collection, projection, selection, args, null)?.use { c ->
                val iId = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val iTitle = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val iName = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val iArtist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val iAlbum = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val iAlbumId = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val iTrack = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val iYear = c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val iDuration = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val iDate = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

                while (c.moveToNext()) {
                    val id = c.getLong(iId)
                    val fileName = c.getString(iName)?.substringBeforeLast('.')
                    val title = c.getString(iTitle)?.takeIf { it.isNotBlank() }
                        ?: fileName ?: "Sin título"
                    val artist = c.getString(iArtist)
                        ?.takeIf { it.isNotBlank() && it != MediaStore.UNKNOWN_STRING }
                        ?: UNKNOWN_ARTIST
                    val album = c.getString(iAlbum)
                        ?.takeIf { it.isNotBlank() && it != MediaStore.UNKNOWN_STRING }
                        ?: "Álbum desconocido"

                    result += RawSong(
                        id = id,
                        uri = ContentUris.withAppendedId(collection, id),
                        title = title,
                        artist = artist,
                        album = album,
                        albumId = c.getLong(iAlbumId),
                        track = c.getInt(iTrack) % 1000,
                        year = c.getInt(iYear),
                        durationMs = c.getLong(iDuration),
                        dateAdded = c.getLong(iDate),
                    )
                }
            }
        } catch (e: SecurityException) {
            // Sin permiso todavía: la biblioteca queda vacía.
        }
        return result
    }

    const val UNKNOWN_ARTIST = "Artista desconocido"
}
