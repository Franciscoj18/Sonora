package com.sonora.player.data

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator

class MusicRepository(private val context: Context, private val dao: MusicDao) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val raw = MutableStateFlow<List<RawSong>?>(null)
    private var scanJob: Job? = null
    private val collator: Collator = Collator.getInstance().apply { strength = Collator.PRIMARY }

    /** Detecta canciones nuevas (por ejemplo, descargas) y actualiza la biblioteca sola. */
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            requestScan(delayMs = 1500)
        }
    }

    init {
        context.contentResolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, observer
        )
    }

    fun requestScan(delayMs: Long = 0) {
        scanJob?.cancel()
        scanJob = scope.launch(Dispatchers.IO) {
            if (delayMs > 0) delay(delayMs)
            raw.value = MediaScanner.scan(context)
        }
    }

    private val playlistData = combine(dao.playlists(), dao.playlistSongs()) { p, s -> p to s }

    val library: StateFlow<Library> = combine(
        raw, dao.overrides(), dao.customAlbums(), dao.albumCovers(), playlistData
    ) { r, overrides, customAlbums, covers, pl ->
        build(r, overrides, customAlbums, covers, pl.first, pl.second)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(scope, SharingStarted.Eagerly, Library())

    private fun build(
        raw: List<RawSong>?,
        overrides: List<SongOverrideEntity>,
        customAlbums: List<CustomAlbumEntity>,
        covers: List<AlbumCoverEntity>,
        playlists: List<PlaylistEntity>,
        playlistSongs: List<PlaylistSongEntity>,
    ): Library {
        if (raw == null) return Library()

        val ovMap = overrides.associateBy { it.songId }
        val caMap = customAlbums.associateBy { it.id }
        val coverMap = covers.associate { it.albumKey to it.coverPath }

        val songs = raw.map { r ->
            val o = ovMap[r.id]
            val custom = o?.customAlbumId?.let { caMap[it] }
            val key = if (custom != null) "c${custom.id}" else "m${r.albumId}"
            Song(
                id = r.id,
                uri = r.uri,
                title = o?.title?.takeIf { it.isNotBlank() } ?: r.title,
                artist = o?.artist?.takeIf { it.isNotBlank() } ?: r.artist,
                album = custom?.name ?: r.album,
                albumKey = key,
                track = o?.track ?: if (custom != null) 0 else r.track,
                year = custom?.year?.takeIf { it > 0 } ?: r.year,
                durationMs = r.durationMs,
                dateAdded = r.dateAdded,
                coverPath = if (custom != null) custom.coverPath else coverMap[key],
                customAlbumId = custom?.id,
            )
        }.sortedWith(compareBy<Song, String>(collator) { it.title })

        val byId = songs.associateBy { it.id }

        val albums = ArrayList<Album>()
        songs.groupBy { it.albumKey }.forEach { (key, list) ->
            val sorted = list.sortedWith(compareBy<Song> { it.track }.thenBy<Song, String>(collator) { it.title })
            val first = sorted.first()
            val custom = first.customAlbumId?.let { caMap[it] }
            val mainArtist = custom?.artist?.takeIf { it.isNotBlank() }
                ?: list.groupingBy { it.artist }.eachCount().maxByOrNull { it.value }?.key
                ?: MediaScanner.UNKNOWN_ARTIST
            albums += Album(
                key = key,
                title = first.album,
                artist = mainArtist,
                year = list.maxOf { it.year },
                songs = sorted,
                customAlbumId = custom?.id,
                coverPath = first.coverPath,
                dateAdded = custom?.createdAt?.div(1000) ?: list.maxOf { it.dateAdded },
            )
        }
        // Álbumes propios que todavía no tienen canciones
        val usedKeys = albums.map { it.key }.toSet()
        customAlbums.filter { "c${it.id}" !in usedKeys }.forEach { c ->
            albums += Album(
                key = "c${c.id}",
                title = c.name,
                artist = c.artist.ifBlank { MediaScanner.UNKNOWN_ARTIST },
                year = c.year,
                songs = emptyList(),
                customAlbumId = c.id,
                coverPath = c.coverPath,
                dateAdded = c.createdAt / 1000,
            )
        }
        albums.sortWith(compareBy<Album, String>(collator) { it.title })

        val artists = songs.groupBy { it.artist }.map { (name, list) ->
            val keys = list.map { it.albumKey }.toSet()
            Artist(
                name = name,
                albums = albums.filter { it.key in keys },
                songs = list,
            )
        }.sortedWith(compareBy<Artist, String>(collator) { it.name })

        val songsByPlaylist = playlistSongs.groupBy { it.playlistId }
        val pls = playlists.map { p ->
            Playlist(
                id = p.id,
                name = p.name,
                coverPath = p.coverPath,
                songs = songsByPlaylist[p.id].orEmpty().mapNotNull { byId[it.songId] },
                createdAt = p.createdAt,
            )
        }

        return Library(songs, albums, artists, pls, byId, loaded = true)
    }

    private suspend fun saveCover(uri: Uri): String? =
        withContext(Dispatchers.IO) { CoverStore.save(context, uri) }

    // ---------- Álbumes propios ----------

    suspend fun createCustomAlbum(
        name: String, artist: String, year: Int, cover: Uri?, songIds: List<Long>,
    ): Long {
        val path = cover?.let { saveCover(it) }
        val id = dao.insertCustomAlbum(
            CustomAlbumEntity(
                name = name.trim(),
                artist = artist.trim(),
                year = year,
                coverPath = path,
                createdAt = System.currentTimeMillis(),
            )
        )
        addSongsToCustomAlbum(id, songIds)
        return id
    }

    suspend fun updateCustomAlbum(id: Long, name: String, artist: String, year: Int, cover: Uri?) {
        val old = dao.customAlbum(id) ?: return
        var path = old.coverPath
        if (cover != null) {
            val newPath = saveCover(cover)
            if (newPath != null) {
                CoverStore.delete(old.coverPath)
                path = newPath
            }
        }
        dao.updateCustomAlbum(old.copy(name = name.trim(), artist = artist.trim(), year = year, coverPath = path))
    }

    suspend fun addSongsToCustomAlbum(albumId: Long, songIds: List<Long>) {
        var track = dao.maxTrack(albumId)
        for (songId in songIds) {
            val o = dao.override(songId) ?: SongOverrideEntity(songId)
            if (o.customAlbumId == albumId) continue
            track += 1
            dao.upsertOverride(o.copy(customAlbumId = albumId, track = track))
        }
    }

    suspend fun removeFromCustomAlbum(songId: Long) {
        val o = dao.override(songId) ?: return
        dao.upsertOverride(o.copy(customAlbumId = null, track = null))
    }

    suspend fun deleteCustomAlbum(id: Long) {
        val album = dao.customAlbum(id)
        dao.clearAlbumFromSongs(id)
        dao.deleteCustomAlbum(id)
        CoverStore.delete(album?.coverPath)
    }

    // ---------- Carátulas ----------

    suspend fun setAlbumCover(album: Album, cover: Uri) {
        val path = saveCover(cover) ?: return
        val customId = album.customAlbumId
        if (customId != null) {
            val a = dao.customAlbum(customId) ?: return
            CoverStore.delete(a.coverPath)
            dao.updateCustomAlbum(a.copy(coverPath = path))
        } else {
            CoverStore.delete(dao.albumCover(album.key)?.coverPath)
            dao.upsertAlbumCover(AlbumCoverEntity(album.key, path))
        }
    }

    suspend fun removeAlbumCover(album: Album) {
        val customId = album.customAlbumId
        if (customId != null) {
            val a = dao.customAlbum(customId) ?: return
            CoverStore.delete(a.coverPath)
            dao.updateCustomAlbum(a.copy(coverPath = null))
        } else {
            CoverStore.delete(dao.albumCover(album.key)?.coverPath)
            dao.deleteAlbumCover(album.key)
        }
    }

    // ---------- Canciones ----------

    suspend fun editSong(songId: Long, title: String, artist: String, track: Int?) {
        val o = dao.override(songId) ?: SongOverrideEntity(songId)
        dao.upsertOverride(
            o.copy(
                title = title.trim().ifBlank { null },
                artist = artist.trim().ifBlank { null },
                track = track ?: o.track,
            )
        )
    }

    // ---------- Playlists ----------

    suspend fun createPlaylist(name: String, cover: Uri?, songIds: List<Long>): Long {
        val path = cover?.let { saveCover(it) }
        val id = dao.insertPlaylist(
            PlaylistEntity(name = name.trim(), coverPath = path, createdAt = System.currentTimeMillis())
        )
        addToPlaylist(id, songIds)
        return id
    }

    suspend fun updatePlaylist(id: Long, name: String, cover: Uri?) {
        val old = dao.playlist(id) ?: return
        var path = old.coverPath
        if (cover != null) {
            val newPath = saveCover(cover)
            if (newPath != null) {
                CoverStore.delete(old.coverPath)
                path = newPath
            }
        }
        dao.updatePlaylist(old.copy(name = name.trim(), coverPath = path))
    }

    suspend fun setPlaylistCover(id: Long, cover: Uri) {
        val old = dao.playlist(id) ?: return
        val path = saveCover(cover) ?: return
        CoverStore.delete(old.coverPath)
        dao.updatePlaylist(old.copy(coverPath = path))
    }

    suspend fun removePlaylistCover(id: Long) {
        val old = dao.playlist(id) ?: return
        CoverStore.delete(old.coverPath)
        dao.updatePlaylist(old.copy(coverPath = null))
    }

    suspend fun addToPlaylist(id: Long, songIds: List<Long>) {
        if (songIds.isEmpty()) return
        var pos = dao.maxPosition(id)
        dao.insertPlaylistSongs(songIds.map { PlaylistSongEntity(id, it, ++pos) })
    }

    suspend fun removeFromPlaylist(id: Long, songId: Long) = dao.removeFromPlaylist(id, songId)

    suspend fun deletePlaylist(id: Long) {
        val old = dao.playlist(id)
        dao.clearPlaylist(id)
        dao.deletePlaylist(id)
        CoverStore.delete(old?.coverPath)
    }
}
