package com.sonora.player.data

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.key.Keyer
import coil.request.Options
import coil.size.pxOrElse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Lee la carátula incrustada en el MP3 usando las miniaturas del sistema,
 * pidiendo solo el tamaño que se va a mostrar (rápido y con poca memoria).
 */
class SongArtFetcher(
    private val context: Context,
    private val data: SongArt,
    private val options: Options,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        val w = options.size.width.pxOrElse { 512 }
        val h = options.size.height.pxOrElse { w }
        val side = maxOf(w, h).coerceIn(96, 1024)
        val bitmap = withContext(Dispatchers.IO) {
            context.contentResolver.loadThumbnail(data.uri, android.util.Size(side, side), null)
        }
        return DrawableResult(
            drawable = BitmapDrawable(context.resources, bitmap),
            isSampled = true,
            dataSource = DataSource.DISK,
        )
    }

    class Factory(private val context: Context) : Fetcher.Factory<SongArt> {
        override fun create(data: SongArt, options: Options, imageLoader: ImageLoader): Fetcher =
            SongArtFetcher(context, data, options)
    }
}

class SongArtKeyer : Keyer<SongArt> {
    override fun key(data: SongArt, options: Options): String = data.uri.toString()
}
