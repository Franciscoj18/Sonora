package com.sonora.player.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.abs

/** Una línea de letra sincronizada. */
data class LyricLine(val timeMs: Long, val text: String)

/** Letra lista para mostrarse. */
data class Lyrics(
    val plain: String?,
    val synced: List<LyricLine>?,
    val source: String,
    val instrumental: Boolean = false,
) {
    val isSynced: Boolean get() = !synced.isNullOrEmpty()

    /** Texto para editar: la versión sincronizada si existe, si no la normal. */
    fun rawForEditing(rawSynced: String?): String = rawSynced ?: plain.orEmpty()
}

/** Resultado del buscador de letras en internet. */
data class LyricsResult(
    val id: Long,
    val track: String,
    val artist: String,
    val album: String,
    val durationSec: Double,
    val instrumental: Boolean,
    val plain: String?,
    val synced: String?,
)

/** Lee el formato LRC: líneas como "[01:23.45] texto". */
object LrcParser {
    private val tag = Regex("""\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]""")
    private val offsetTag = Regex("""\[offset:\s*([+-]?\d+)\s*]""", RegexOption.IGNORE_CASE)

    fun looksSynced(text: String): Boolean =
        text.lineSequence().count { tag.containsMatchIn(it) } >= 2

    fun parse(text: String): List<LyricLine> {
        val offset = offsetTag.find(text)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
        val out = ArrayList<LyricLine>()
        for (raw in text.lineSequence()) {
            val matches = tag.findAll(raw).toList()
            if (matches.isEmpty()) continue
            val lyric = raw.substring(matches.last().range.last + 1).trim()
            for (m in matches) {
                val min = m.groupValues[1].toLongOrNull() ?: continue
                val sec = m.groupValues[2].toLongOrNull() ?: continue
                val fracStr = m.groupValues[3]
                val frac = when (fracStr.length) {
                    0 -> 0L
                    1 -> fracStr.toLong() * 100
                    2 -> fracStr.toLong() * 10
                    else -> fracStr.take(3).toLong()
                }
                val t = (min * 60_000 + sec * 1000 + frac - offset).coerceAtLeast(0L)
                out += LyricLine(t, lyric)
            }
        }
        out.sortBy { it.timeMs }
        return out
    }

    /** Quita las marcas de tiempo para tener solo el texto. */
    fun toPlain(text: String): String =
        text.lineSequence()
            .map { tag.replace(it, "").trim() }
            .filterNot { it.startsWith("[") && it.endsWith("]") }
            .joinToString("\n")
            .trim()
}

/**
 * Lee la letra incrustada en un MP3 (etiqueta ID3v2, cuadro "USLT").
 * Solo lee el principio del archivo, así que es rápido.
 */
object Id3Lyrics {

    fun read(context: Context, uri: Uri): String? = try {
        context.contentResolver.openInputStream(uri)?.use { readFrom(it) }
    } catch (e: Exception) {
        null
    }

    private fun readFully(input: InputStream, n: Int): ByteArray? {
        val buf = ByteArray(n)
        var read = 0
        while (read < n) {
            val r = input.read(buf, read, n - read)
            if (r < 0) return null
            read += r
        }
        return buf
    }

    private fun syncSafe(b: ByteArray, o: Int): Int =
        ((b[o].toInt() and 0x7F) shl 21) or ((b[o + 1].toInt() and 0x7F) shl 14) or
            ((b[o + 2].toInt() and 0x7F) shl 7) or (b[o + 3].toInt() and 0x7F)

    private fun bigEndian(b: ByteArray, o: Int): Int =
        ((b[o].toInt() and 0xFF) shl 24) or ((b[o + 1].toInt() and 0xFF) shl 16) or
            ((b[o + 2].toInt() and 0xFF) shl 8) or (b[o + 3].toInt() and 0xFF)

    private fun readFrom(input: InputStream): String? {
        val header = readFully(input, 10) ?: return null
        if (header[0] != 'I'.code.toByte() || header[1] != 'D'.code.toByte() || header[2] != '3'.code.toByte()) return null
        val major = header[3].toInt()
        if (major != 3 && major != 4) return null
        val flags = header[5].toInt()
        val size = syncSafe(header, 6)
        if (size <= 0 || size > 20_000_000) return null
        val tag = readFully(input, size) ?: return null

        var pos = 0
        if (flags and 0x40 != 0) { // encabezado extendido
            val ext = if (major == 4) syncSafe(tag, 0) else bigEndian(tag, 0) + 4
            pos += ext
        }
        while (pos + 10 <= tag.size) {
            val id = String(tag, pos, 4, Charsets.ISO_8859_1)
            if (id[0] == '\u0000') break
            val frameSize = if (major == 4) syncSafe(tag, pos + 4) else bigEndian(tag, pos + 4)
            val start = pos + 10
            if (frameSize <= 0 || start + frameSize > tag.size) break
            if (id == "USLT") {
                return decodeUslt(tag, start, frameSize)
            }
            pos = start + frameSize
        }
        return null
    }

    private fun decodeUslt(b: ByteArray, start: Int, len: Int): String? {
        if (len < 5) return null
        val enc = b[start].toInt()
        var p = start + 4 // codificación (1) + idioma (3)
        val end = start + len
        // Saltar la descripción (termina en 0 o en 00 00 según la codificación)
        val wide = enc == 1 || enc == 2
        if (wide) {
            while (p + 1 < end && !(b[p].toInt() == 0 && b[p + 1].toInt() == 0)) p += 2
            p += 2
        } else {
            while (p < end && b[p].toInt() != 0) p++
            p += 1
        }
        if (p >= end) return null
        val charset = when (enc) {
            0 -> Charsets.ISO_8859_1
            1 -> Charsets.UTF_16
            2 -> Charsets.UTF_16BE
            else -> Charsets.UTF_8
        }
        return String(b, p, end - p, charset).trim('\u0000', ' ', '\n', '\r').takeIf { it.isNotBlank() }
    }
}

/** Cliente de LRCLIB: base de datos abierta y gratuita de letras (muchas sincronizadas). */
object LrclibClient {
    private const val BASE = "https://lrclib.net/api"
    private const val AGENT = "Sonora/1.6 (https://github.com/Franciscoj18/Sonora)"

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    /** Devuelve el cuerpo de la respuesta, null si no existe (404). Lanza excepción si no hay conexión. */
    private fun httpGet(url: String): String? {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 8000
            conn.readTimeout = 10000
            conn.setRequestProperty("User-Agent", AGENT)
            conn.setRequestProperty("Accept", "application/json")
            val code = conn.responseCode
            if (code == 404 || code == 400) return null
            if (code !in 200..299) throw java.io.IOException("HTTP $code")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun JSONObject.str(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

    private fun parse(o: JSONObject) = LyricsResult(
        id = o.optLong("id"),
        track = o.str("trackName").orEmpty(),
        artist = o.str("artistName").orEmpty(),
        album = o.str("albumName").orEmpty(),
        durationSec = o.optDouble("duration", 0.0),
        instrumental = o.optBoolean("instrumental", false),
        plain = o.str("plainLyrics"),
        synced = o.str("syncedLyrics"),
    )

    /** Búsqueda exacta por canción y artista (la más precisa). */
    suspend fun get(track: String, artist: String, durationSec: Int): LyricsResult? = withContext(Dispatchers.IO) {
        val url = "$BASE/get?track_name=${enc(track)}&artist_name=${enc(artist)}&duration=$durationSec"
        httpGet(url)?.let { parse(JSONObject(it)) }
    }

    /** Búsqueda libre: devuelve varias opciones. */
    suspend fun search(query: String): List<LyricsResult> = withContext(Dispatchers.IO) {
        val body = httpGet("$BASE/search?q=${enc(query)}") ?: return@withContext emptyList()
        val arr = JSONArray(body)
        (0 until arr.length()).map { parse(arr.getJSONObject(it)) }
    }

    suspend fun searchTrack(track: String, artist: String?): List<LyricsResult> = withContext(Dispatchers.IO) {
        val url = buildString {
            append("$BASE/search?track_name=").append(enc(track))
            if (!artist.isNullOrBlank()) append("&artist_name=").append(enc(artist))
        }
        val body = httpGet(url) ?: return@withContext emptyList()
        val arr = JSONArray(body)
        (0 until arr.length()).map { parse(arr.getJSONObject(it)) }
    }
}

/** Estado de la letra de una canción. */
sealed interface LyricsState {
    data object Loading : LyricsState
    data object NotFound : LyricsState
    data object Offline : LyricsState
    data class Ready(val lyrics: Lyrics, val rawSynced: String?) : LyricsState
}

/**
 * Encuentra la letra de una canción en este orden:
 * 1) la que ya guardaste o encontraste antes, 2) la que viene dentro del MP3,
 * 3) LRCLIB en internet (y la guarda para que después funcione sin conexión).
 */
class LyricsRepository(private val context: Context, private val dao: MusicDao) {

    private fun LyricsEntity.toState(): LyricsState {
        if (source == "removed") return LyricsState.NotFound
        val synced = synced?.takeIf { LrcParser.looksSynced(it) }
        val lines = synced?.let { LrcParser.parse(it) }
        val plainText = plain ?: synced?.let { LrcParser.toPlain(it) }
        if (lines.isNullOrEmpty() && plainText.isNullOrBlank() && !instrumental) return LyricsState.NotFound
        return LyricsState.Ready(Lyrics(plainText, lines, source, instrumental), synced)
    }

    private suspend fun save(songId: Long, plain: String?, synced: String?, source: String, instrumental: Boolean = false) {
        dao.upsertLyrics(LyricsEntity(songId, plain, synced, source, instrumental, System.currentTimeMillis()))
    }

    suspend fun load(song: Song): LyricsState {
        dao.lyrics(song.id)?.let { return it.toState() }

        // Letra dentro del archivo MP3
        val embedded = withContext(Dispatchers.IO) { Id3Lyrics.read(context, song.uri) }
        if (embedded != null) {
            val isSynced = LrcParser.looksSynced(embedded)
            save(song.id, if (isSynced) LrcParser.toPlain(embedded) else embedded, if (isSynced) embedded else null, "file")
            return dao.lyrics(song.id)?.toState() ?: LyricsState.NotFound
        }

        // Internet
        val found = try {
            findOnline(song)
        } catch (e: Exception) {
            return LyricsState.Offline
        }
        if (found == null) return LyricsState.NotFound
        save(song.id, found.plain, found.synced, "lrclib", found.instrumental)
        return dao.lyrics(song.id)?.toState() ?: LyricsState.NotFound
    }

    private suspend fun findOnline(song: Song): LyricsResult? {
        val durationSec = (song.durationMs / 1000).toInt()
        val artist = song.artist.takeIf { it != MediaScanner.UNKNOWN_ARTIST }
        if (artist != null) {
            LrclibClient.get(song.title, artist, durationSec)
                ?.takeIf { it.synced != null || it.plain != null || it.instrumental }
                ?.let { return it }
        }
        val candidates = LrclibClient.searchTrack(song.title, artist)
            .filter { it.synced != null || it.plain != null }
        return candidates
            .filter { abs(it.durationSec - durationSec) <= 4 }
            .maxByOrNull { if (it.synced != null) 1 else 0 }
            ?: candidates.firstOrNull { abs(it.durationSec - durationSec) <= 12 }
    }

    suspend fun search(query: String): List<LyricsResult> =
        LrclibClient.search(query).filter { it.synced != null || it.plain != null || it.instrumental }

    /** Usa un resultado del buscador para esta canción. */
    suspend fun choose(songId: Long, result: LyricsResult) =
        save(songId, result.plain, result.synced, "lrclib", result.instrumental)

    /** Guarda una letra pegada o importada (detecta sola si trae tiempos). */
    suspend fun saveManual(songId: Long, text: String) {
        val t = text.trim()
        if (t.isEmpty()) return remove(songId)
        if (LrcParser.looksSynced(t)) save(songId, LrcParser.toPlain(t), t, "user")
        else save(songId, t, null, "user")
    }

    suspend fun remove(songId: Long) = save(songId, null, null, "removed")

    /** Lee un archivo .lrc o .txt elegido por el usuario. */
    suspend fun readTextFile(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { s ->
                val bytes = s.readBytes()
                // UTF-8 con o sin BOM; si falla, Latin-1
                val text = String(bytes, Charsets.UTF_8)
                if (text.contains('�')) String(bytes, Charsets.ISO_8859_1) else text.removePrefix("﻿")
            }
        } catch (e: Exception) {
            null
        }
    }
}
