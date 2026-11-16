package com.sonora.player.playback

import android.content.Context
import androidx.media3.common.Player

/** Lo que se necesita para continuar donde te quedaste. */
data class SavedPlayback(
    val queue: List<Long>,
    val currentId: Long,
    val positionMs: Long,
    val shuffle: Boolean,
    val repeatMode: Int,
)

/**
 * Guarda la cola, la canción actual, el segundo exacto, el modo aleatorio y el de repetir,
 * para que al volver a abrir Sonora todo siga igual aunque Android haya cerrado la app.
 */
object PlaybackStore {
    private const val PREFS = "playback_state"
    private const val KEY_QUEUE = "queue"
    private const val KEY_CURRENT = "current"
    private const val KEY_POSITION = "position"
    private const val KEY_SHUFFLE = "shuffle"
    private const val KEY_REPEAT = "repeat"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Guarda la lista de canciones (solo cuando la cola cambia). */
    fun saveQueue(context: Context, player: Player) {
        val count = player.mediaItemCount
        if (count == 0) return
        val ids = StringBuilder()
        for (i in 0 until count) {
            val id = player.getMediaItemAt(i).mediaId
            if (id.isEmpty()) continue
            if (ids.isNotEmpty()) ids.append(',')
            ids.append(id)
        }
        prefs(context).edit().putString(KEY_QUEUE, ids.toString()).apply()
    }

    /** Guarda la canción actual y el segundo (se llama seguido, es muy barato). */
    fun saveState(context: Context, player: Player) {
        val id = player.currentMediaItem?.mediaId?.toLongOrNull() ?: return
        prefs(context).edit()
            .putLong(KEY_CURRENT, id)
            .putLong(KEY_POSITION, player.currentPosition.coerceAtLeast(0L))
            .putBoolean(KEY_SHUFFLE, player.shuffleModeEnabled)
            .putInt(KEY_REPEAT, player.repeatMode)
            .apply()
    }

    fun load(context: Context): SavedPlayback? {
        val p = prefs(context)
        val queue = p.getString(KEY_QUEUE, null)
            ?.split(',')
            ?.mapNotNull { it.toLongOrNull() }
            .orEmpty()
        if (queue.isEmpty()) return null
        return SavedPlayback(
            queue = queue,
            currentId = p.getLong(KEY_CURRENT, -1L),
            positionMs = p.getLong(KEY_POSITION, 0L),
            shuffle = p.getBoolean(KEY_SHUFFLE, false),
            repeatMode = p.getInt(KEY_REPEAT, Player.REPEAT_MODE_OFF),
        )
    }
}
