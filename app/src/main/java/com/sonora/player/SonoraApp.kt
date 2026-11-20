package com.sonora.player

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.sonora.player.data.LyricsRepository
import com.sonora.player.data.MusicRepository
import com.sonora.player.data.SongArtFetcher
import com.sonora.player.data.SongArtKeyer
import com.sonora.player.data.SonoraDatabase

class SonoraApp : Application(), ImageLoaderFactory {

    val repository: MusicRepository by lazy {
        MusicRepository(this, SonoraDatabase.get(this).dao())
    }

    val lyrics: LyricsRepository by lazy {
        LyricsRepository(this, SonoraDatabase.get(this).dao())
    }

    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components {
            add(SongArtKeyer())
            add(SongArtFetcher.Factory(this@SonoraApp))
        }
        .crossfade(150)
        .build()
}
