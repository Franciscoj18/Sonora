package com.sonora.player.data

import android.net.Uri
import java.io.File

/** Carátula incrustada en el archivo de audio (se lee con el SongArtFetcher). */
data class SongArt(val uri: Uri)

data class Song(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val albumKey: String,
    val track: Int,
    val year: Int,
    val durationMs: Long,
    val dateAdded: Long,
    /** Carátula elegida por el usuario (álbum propio o carátula reemplazada). */
    val coverPath: String?,
    val customAlbumId: Long?,
) {
    val art: Any get() = coverPath?.let { File(it) } ?: SongArt(uri)
}

data class Album(
    val key: String,
    val title: String,
    val artist: String,
    val year: Int,
    val songs: List<Song>,
    val customAlbumId: Long?,
    val coverPath: String?,
    val dateAdded: Long,
) {
    val isCustom: Boolean get() = customAlbumId != null
    val art: Any? get() = coverPath?.let { File(it) } ?: songs.firstOrNull()?.let { SongArt(it.uri) }
    val totalMs: Long get() = songs.sumOf { it.durationMs }
}

data class Artist(
    val name: String,
    val albums: List<Album>,
    val songs: List<Song>,
) {
    val art: Any? get() = albums.firstOrNull()?.art ?: songs.firstOrNull()?.art
}

data class Playlist(
    val id: Long,
    val name: String,
    val coverPath: String?,
    val songs: List<Song>,
    val createdAt: Long,
) {
    val totalMs: Long get() = songs.sumOf { it.durationMs }
}

data class Library(
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val byId: Map<Long, Song> = emptyMap(),
    val loaded: Boolean = false,
) {
    val customAlbums: List<Album> get() = albums.filter { it.isCustom }
    fun album(key: String): Album? = albums.firstOrNull { it.key == key }
    fun playlist(id: Long): Playlist? = playlists.firstOrNull { it.id == id }
    fun artist(name: String): Artist? = artists.firstOrNull { it.name == name }
}
