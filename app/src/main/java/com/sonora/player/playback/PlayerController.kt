package com.sonora.player.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.sonora.player.data.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import kotlin.random.Random

data class QueueEntry(val index: Int, val songId: Long)

data class PlayerState(
    val currentId: Long? = null,
    val title: String = "",
    val artist: String = "",
    val isPlaying: Boolean = false,
    val durationMs: Long = 0,
    val shuffle: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val upcoming: List<QueueEntry> = emptyList(),
)

/** Conecta la interfaz con el servicio de reproducción. */
class PlayerController(context: Context) {

    private val appContext = context.applicationContext
    private val future: ListenableFuture<MediaController>
    private var controller: MediaController? = null
    private val pending = ArrayList<(MediaController) -> Unit>()

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = update()
    }

    init {
        val token = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        future = MediaController.Builder(appContext, token).buildAsync()
        future.addListener({
            val c = runCatching { future.get() }.getOrNull() ?: return@addListener
            controller = c
            c.addListener(listener)
            update()
            pending.forEach { it(c) }
            pending.clear()
        }, ContextCompat.getMainExecutor(appContext))
    }

    private fun withController(block: (MediaController) -> Unit) {
        val c = controller
        if (c != null) block(c) else pending.add(block)
    }

    private fun update() {
        val c = controller ?: return
        val upcoming = ArrayList<QueueEntry>()
        val timeline = c.currentTimeline
        if (!timeline.isEmpty) {
            var i = c.currentMediaItemIndex
            while (upcoming.size < 60) {
                i = timeline.getNextWindowIndex(i, Player.REPEAT_MODE_OFF, c.shuffleModeEnabled)
                if (i == C.INDEX_UNSET) break
                val id = c.getMediaItemAt(i).mediaId.toLongOrNull() ?: continue
                upcoming += QueueEntry(i, id)
            }
        }
        val meta = c.mediaMetadata
        _state.value = PlayerState(
            currentId = c.currentMediaItem?.mediaId?.toLongOrNull(),
            title = meta.title?.toString().orEmpty(),
            artist = meta.artist?.toString().orEmpty(),
            isPlaying = c.isPlaying,
            durationMs = c.duration.takeIf { it != C.TIME_UNSET && it > 0 } ?: 0L,
            shuffle = c.shuffleModeEnabled,
            repeatMode = c.repeatMode,
            upcoming = upcoming,
        )
    }

    val positionMs: Long get() = controller?.currentPosition ?: 0L

    fun playItems(items: List<MediaItem>, startIndex: Int, shuffle: Boolean) {
        if (items.isEmpty()) return
        withController { c ->
            c.shuffleModeEnabled = shuffle
            val start = if (shuffle) Random.nextInt(items.size) else startIndex.coerceIn(0, items.lastIndex)
            c.setMediaItems(items, start, 0L)
            c.prepare()
            c.play()
        }
    }

    fun toggle() = withController { c ->
        if (c.isPlaying) {
            c.pause()
        } else {
            if (c.playbackState == Player.STATE_IDLE) c.prepare()
            if (c.playbackState == Player.STATE_ENDED) c.seekTo(0, 0L)
            c.play()
        }
    }

    fun next() = withController { it.seekToNext() }
    fun previous() = withController { it.seekToPrevious() }
    fun seekTo(ms: Long) = withController { it.seekTo(ms) }
    fun skipTo(index: Int) = withController { it.seekTo(index, 0L); it.play() }
    fun toggleShuffle() = withController { it.shuffleModeEnabled = !it.shuffleModeEnabled }

    fun cycleRepeat() = withController { c ->
        c.repeatMode = when (c.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun playNext(item: MediaItem) = withController { c ->
        if (c.mediaItemCount == 0) {
            playItems(listOf(item), 0, false)
        } else {
            c.addMediaItem(c.currentMediaItemIndex + 1, item)
        }
    }

    fun addToQueue(item: MediaItem) = withController { c ->
        if (c.mediaItemCount == 0) playItems(listOf(item), 0, false) else c.addMediaItem(item)
    }

    fun release() {
        controller?.removeListener(listener)
        MediaController.releaseFuture(future)
        controller = null
    }

    companion object {
        /**
         * Convierte canciones en elementos del reproductor. Si una canción tiene carátula
         * propia, se incluye para que también aparezca en la notificación y la pantalla de bloqueo.
         * Llamar fuera del hilo principal.
         */
        fun buildItems(songs: List<Song>): List<MediaItem> {
            val artCache = HashMap<String, ByteArray?>()
            return songs.map { song ->
                val meta = MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                song.coverPath?.let { path ->
                    val bytes = artCache.getOrPut(path) { runCatching { File(path).readBytes() }.getOrNull() }
                    if (bytes != null) {
                        meta.setArtworkData(bytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                    } else {
                        meta.setArtworkUri(Uri.fromFile(File(path)))
                    }
                }
                MediaItem.Builder()
                    .setMediaId(song.id.toString())
                    .setUri(song.uri)
                    .setMediaMetadata(meta.build())
                    .build()
            }
        }
    }
}
