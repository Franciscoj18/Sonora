package com.sonora.player

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sonora.player.data.Album
import com.sonora.player.data.Library
import com.sonora.player.data.LyricsResult
import com.sonora.player.data.LyricsState
import com.sonora.player.data.Song
import com.sonora.player.playback.PlaybackStore
import com.sonora.player.playback.PlayerController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as SonoraApp).repository
    private val lyricsRepo = (app as SonoraApp).lyrics
    val library: StateFlow<Library> = repo.library
    val player = PlayerController(app)

    // Estado de la interfaz compartido entre pantallas
    var showNowPlaying by mutableStateOf(false)
    var editingSong by mutableStateOf<Song?>(null)
    var playlistSheet by mutableStateOf<List<Long>?>(null)
    var albumSheet by mutableStateOf<List<Long>?>(null)

    /** Canciones que se agregarán al álbum o playlist que se está creando. */
    var pendingSongIds: List<Long> = emptyList()

    fun rescan() = repo.requestScan()

    init {
        // Continuar donde te quedaste: cuando la biblioteca esté lista, vuelve a cargar
        // la última cola en el mismo segundo (en pausa).
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) { PlaybackStore.load(app) } ?: return@launch
            val lib = library.first { it.loaded && it.songs.isNotEmpty() }
            val songs = saved.queue.mapNotNull { lib.byId[it] }
            if (songs.isEmpty()) return@launch
            val index = songs.indexOfFirst { it.id == saved.currentId }
            val items = withContext(Dispatchers.IO) { PlayerController.buildItems(songs) }
            player.restoreIfEmpty(
                items = items,
                index = if (index >= 0) index else 0,
                positionMs = if (index >= 0) saved.positionMs else 0L,
                shuffle = saved.shuffle,
                repeatMode = saved.repeatMode,
            )
        }
    }

    // ---------- Reproducción ----------

    fun play(songs: List<Song>, startIndex: Int = 0, shuffle: Boolean = false) {
        if (songs.isEmpty()) return
        viewModelScope.launch {
            val items = withContext(Dispatchers.IO) { PlayerController.buildItems(songs) }
            player.playItems(items, startIndex, shuffle)
        }
    }

    fun playNext(song: Song) = viewModelScope.launch {
        val item = withContext(Dispatchers.IO) { PlayerController.buildItems(listOf(song)).first() }
        player.playNext(item)
    }

    fun addToQueue(song: Song) = viewModelScope.launch {
        val item = withContext(Dispatchers.IO) { PlayerController.buildItems(listOf(song)).first() }
        player.addToQueue(item)
    }

    /** Ordena las canciones seleccionadas como aparecen en sus álbumes. */
    fun orderIds(ids: Collection<Long>): List<Long> {
        val lib = library.value
        return ids.mapNotNull { lib.byId[it] }
            .sortedWith(compareBy<Song>({ it.albumKey }, { it.track }, { it.title.lowercase() }))
            .map { it.id }
    }

    // ---------- Álbumes ----------

    fun createAlbum(name: String, artist: String, year: Int, cover: Uri?, onDone: (Long) -> Unit) {
        val ids = pendingSongIds
        pendingSongIds = emptyList()
        viewModelScope.launch {
            val id = repo.createCustomAlbum(name, artist, year, cover, ids)
            onDone(id)
        }
    }

    fun updateAlbum(id: Long, name: String, artist: String, year: Int, cover: Uri?) =
        viewModelScope.launch { repo.updateCustomAlbum(id, name, artist, year, cover) }

    fun addToCustomAlbum(albumId: Long, songIds: List<Long>) =
        viewModelScope.launch { repo.addSongsToCustomAlbum(albumId, orderIds(songIds)) }

    fun removeFromCustomAlbum(songId: Long) =
        viewModelScope.launch { repo.removeFromCustomAlbum(songId) }

    fun deleteCustomAlbum(id: Long) = viewModelScope.launch { repo.deleteCustomAlbum(id) }

    fun setAlbumCover(album: Album, uri: Uri) = viewModelScope.launch { repo.setAlbumCover(album, uri) }

    fun removeAlbumCover(album: Album) = viewModelScope.launch { repo.removeAlbumCover(album) }

    // ---------- Canciones ----------

    fun editSong(songId: Long, title: String, artist: String, track: Int?) =
        viewModelScope.launch { repo.editSong(songId, title, artist, track) }

    // ---------- Playlists ----------

    fun createPlaylist(name: String, cover: Uri?, onDone: (Long) -> Unit) {
        val ids = pendingSongIds
        pendingSongIds = emptyList()
        viewModelScope.launch {
            val id = repo.createPlaylist(name, cover, ids)
            onDone(id)
        }
    }

    fun updatePlaylist(id: Long, name: String, cover: Uri?) =
        viewModelScope.launch { repo.updatePlaylist(id, name, cover) }

    fun setPlaylistCover(id: Long, uri: Uri) = viewModelScope.launch { repo.setPlaylistCover(id, uri) }

    fun removePlaylistCover(id: Long) = viewModelScope.launch { repo.removePlaylistCover(id) }

    fun addToPlaylist(id: Long, songIds: List<Long>) =
        viewModelScope.launch { repo.addToPlaylist(id, songIds) }

    fun removeFromPlaylist(id: Long, songId: Long) =
        viewModelScope.launch { repo.removeFromPlaylist(id, songId) }

    fun deletePlaylist(id: Long) = viewModelScope.launch { repo.deletePlaylist(id) }

    // ---------- Letras ----------

    suspend fun lyricsFor(song: Song): LyricsState = lyricsRepo.load(song)

    /** Busca letras en internet; lanza excepción si no hay conexión. */
    suspend fun searchLyrics(query: String): List<LyricsResult> = lyricsRepo.search(query)

    suspend fun chooseLyrics(songId: Long, result: LyricsResult) = lyricsRepo.choose(songId, result)

    suspend fun saveLyricsText(songId: Long, text: String) = lyricsRepo.saveManual(songId, text)

    suspend fun removeLyrics(songId: Long) = lyricsRepo.remove(songId)

    suspend fun readLyricsFile(uri: Uri): String? = lyricsRepo.readTextFile(uri)

    override fun onCleared() {
        player.release()
    }
}
