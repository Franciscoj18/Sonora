package com.sonora.player.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import java.io.File

/**
 * Guarda las carátulas elegidas por el usuario: las recorta en cuadrado (centrado),
 * las reduce a 800 px y las comprime, para que pesen poco y carguen rápido.
 */
object CoverStore {
    private const val MAX_SIZE = 800

    fun save(context: Context, uri: Uri): String? = try {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val minSide = minOf(info.size.width, info.size.height)
            var sample = 1
            while (minSide / (sample * 2) >= MAX_SIZE) sample *= 2
            decoder.setTargetSampleSize(sample)
        }
        val side = minOf(bitmap.width, bitmap.height)
        val square = Bitmap.createBitmap(
            bitmap,
            (bitmap.width - side) / 2,
            (bitmap.height - side) / 2,
            side,
            side
        )
        val scaled = if (side > MAX_SIZE) {
            Bitmap.createScaledBitmap(square, MAX_SIZE, MAX_SIZE, true)
        } else {
            square
        }
        val dir = File(context.filesDir, "covers").apply { mkdirs() }
        val file = File(dir, "cover_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        file.absolutePath
    } catch (e: Exception) {
        null
    }

    fun delete(path: String?) {
        if (path != null) runCatching { File(path).delete() }
    }
}
