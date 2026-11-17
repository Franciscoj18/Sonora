package com.sonora.player.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Íconos propios de Sonora para los controles de reproducción: formas limpias con
 * esquinas suaves (relleno + trazo redondeado), más cercanas al estilo del iPhone que
 * los íconos estándar de Android.
 */
object SonoraIcons {

    private fun icon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply(block).build()

    /** Figura rellena con esquinas redondeadas (el trazo redondo suaviza las puntas). */
    private fun ImageVector.Builder.soft(corner: Float = 1.6f, pathBuilder: PathBuilder.() -> Unit) {
        path(
            fill = SolidColor(Color.Black),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = corner,
            strokeLineJoin = StrokeJoin.Round,
            strokeLineCap = StrokeCap.Round,
            pathBuilder = pathBuilder,
        )
    }

    /** Línea fina con puntas redondas. */
    private fun ImageVector.Builder.line(width: Float = 1.9f, pathBuilder: PathBuilder.() -> Unit) {
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = width,
            strokeLineJoin = StrokeJoin.Round,
            strokeLineCap = StrokeCap.Round,
            pathBuilder = pathBuilder,
        )
    }

    val Play: ImageVector by lazy {
        icon("Play") {
            soft(2.2f) {
                moveTo(7.2f, 4.6f)
                lineTo(19.4f, 12f)
                lineTo(7.2f, 19.4f)
                close()
            }
        }
    }

    val Pause: ImageVector by lazy {
        icon("Pause") {
            soft(1.8f) {
                moveTo(6.4f, 4.8f); lineTo(9.6f, 4.8f); lineTo(9.6f, 19.2f); lineTo(6.4f, 19.2f); close()
            }
            soft(1.8f) {
                moveTo(14.4f, 4.8f); lineTo(17.6f, 4.8f); lineTo(17.6f, 19.2f); lineTo(14.4f, 19.2f); close()
            }
        }
    }

    /** Siguiente: dos triángulos, sin barra. */
    val Next: ImageVector by lazy {
        icon("Next") {
            soft(1.6f) {
                moveTo(2.8f, 6.4f); lineTo(11.6f, 12f); lineTo(2.8f, 17.6f); close()
            }
            soft(1.6f) {
                moveTo(12.4f, 6.4f); lineTo(21.2f, 12f); lineTo(12.4f, 17.6f); close()
            }
        }
    }

    /** Anterior: dos triángulos, sin barra. */
    val Previous: ImageVector by lazy {
        icon("Previous") {
            soft(1.6f) {
                moveTo(21.2f, 6.4f); lineTo(12.4f, 12f); lineTo(21.2f, 17.6f); close()
            }
            soft(1.6f) {
                moveTo(11.6f, 6.4f); lineTo(2.8f, 12f); lineTo(11.6f, 17.6f); close()
            }
        }
    }

    val Shuffle: ImageVector by lazy {
        icon("Shuffle") {
            line {
                // Línea que cruza hacia arriba
                moveTo(3.5f, 17f)
                lineTo(7f, 17f)
                curveTo(10.5f, 17f, 12f, 7f, 16f, 7f)
                lineTo(20f, 7f)
            }
            line {
                // Línea que cruza hacia abajo
                moveTo(3.5f, 7f)
                lineTo(7f, 7f)
                curveTo(8.7f, 7f, 9.8f, 9.2f, 10.8f, 11.2f)
                moveTo(13.2f, 12.8f)
                curveTo(14.2f, 14.8f, 15.2f, 17f, 16f, 17f)
                lineTo(20f, 17f)
            }
            line {
                moveTo(17.6f, 4.6f); lineTo(20f, 7f); lineTo(17.6f, 9.4f)
            }
            line {
                moveTo(17.6f, 14.6f); lineTo(20f, 17f); lineTo(17.6f, 19.4f)
            }
        }
    }

    val Repeat: ImageVector by lazy {
        icon("Repeat") { repeatShape() }
    }

    val RepeatOne: ImageVector by lazy {
        icon("RepeatOne") {
            repeatShape()
            line(1.7f) {
                moveTo(11.1f, 10.4f); lineTo(12.3f, 9.6f); lineTo(12.3f, 14.4f)
            }
        }
    }

    private fun ImageVector.Builder.repeatShape() {
        line {
            moveTo(4f, 11.5f)
            lineTo(4f, 10f)
            curveTo(4f, 8.3f, 5.3f, 7f, 7f, 7f)
            lineTo(19.5f, 7f)
        }
        line { moveTo(17f, 4.5f); lineTo(19.5f, 7f); lineTo(17f, 9.5f) }
        line {
            moveTo(20f, 12.5f)
            lineTo(20f, 14f)
            curveTo(20f, 15.7f, 18.7f, 17f, 17f, 17f)
            lineTo(4.5f, 17f)
        }
        line { moveTo(7f, 14.5f); lineTo(4.5f, 17f); lineTo(7f, 19.5f) }
    }
}
