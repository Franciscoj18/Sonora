package com.sonora.player.ui

import android.app.Activity
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import androidx.media3.common.Player
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.sonora.player.MainViewModel
import com.sonora.player.data.Library
import com.sonora.player.data.Song
import com.sonora.player.playback.PlayerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val DefaultTop = Color(0xFF55555A)
private val DefaultBottom = Color(0xFF1C1C1E)
private val White60 = Color.White.copy(alpha = 0.6f)

/**
 * Saca dos colores de la carátula para el fondo degradado (en lugar de un desenfoque
 * en tiempo real, que sería pesado en Android 10/11). Se oscurecen para que el texto
 * blanco siempre se lea bien.
 */
private suspend fun extractColors(context: Context, model: Any?): Pair<Color, Color> {
    if (model == null) return DefaultTop to DefaultBottom
    val request = ImageRequest.Builder(context)
        .data(model)
        .size(96)
        .allowHardware(false)
        .build()
    val result = context.imageLoader.execute(request)
    val drawable = (result as? SuccessResult)?.drawable ?: return DefaultTop to DefaultBottom
    return withContext(Dispatchers.Default) {
        val bitmap = drawable.toBitmap()
        val palette = Palette.from(bitmap).maximumColorCount(12).generate()
        val base = palette.getDominantColor(0xFF444444.toInt())
        val second = palette.getDarkVibrantColor(palette.getDarkMutedColor(base))
        var k = 0.2f
        var top = ColorUtils.blendARGB(base, android.graphics.Color.BLACK, k)
        while (ColorUtils.calculateLuminance(top) > 0.28 && k < 0.9f) {
            k += 0.1f
            top = ColorUtils.blendARGB(base, android.graphics.Color.BLACK, k)
        }
        val bottom = ColorUtils.blendARGB(second, android.graphics.Color.BLACK, 0.62f)
        Color(top) to Color(bottom)
    }
}

@Composable
fun NowPlayingScreen(vm: MainViewModel, lib: Library, song: Song?, state: PlayerState, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val context = LocalContext.current
    val art = song?.art
    var colors by remember { mutableStateOf(DefaultTop to DefaultBottom) }
    LaunchedEffect(art) { colors = extractColors(context, art) }
    val top by animateColorAsState(colors.first, tween(700), label = "top")
    val bottom by animateColorAsState(colors.second, tween(700), label = "bottom")

    // Íconos de la barra de estado en blanco mientras se ve el reproductor
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val prevStatus = controller?.isAppearanceLightStatusBars
        val prevNav = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            if (controller != null && prevStatus != null && prevNav != null) {
                controller.isAppearanceLightStatusBars = prevStatus
                controller.isAppearanceLightNavigationBars = prevNav
            }
        }
    }

    val scope = rememberCoroutineScope()
    val drag = remember { Animatable(0f) }
    val close by rememberUpdatedState(onClose)
    var showQueue by remember { mutableStateOf(false) }

    // Deslizar hacia abajo para cerrar, como en el iPhone
    val dragToClose = Modifier.pointerInput(Unit) {
        detectVerticalDragGestures(
            onDragEnd = {
                scope.launch {
                    if (drag.value > size.height * 0.15f) close() else drag.animateTo(0f, spring())
                }
            },
            onDragCancel = { scope.launch { drag.animateTo(0f, spring()) } },
            onVerticalDrag = { change, amount ->
                change.consume()
                scope.launch { drag.snapTo((drag.value + amount).coerceAtLeast(0f)) }
            },
        )
    }

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { translationY = drag.value }
            .background(Brush.verticalGradient(listOf(top, bottom)))
            .pointerInput(Unit) { detectTapGestures { } } // evita toques a la pantalla de atrás
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp),
        ) {
            // Asa superior
            Box(
                dragToClose
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { close() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(width = 40.dp, height = 5.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.45f))
                )
            }

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .then(if (showQueue) Modifier else dragToClose),
                contentAlignment = Alignment.Center,
            ) {
                if (showQueue) {
                    QueuePanel(vm, lib, state)
                } else {
                    val scale by animateFloatAsState(
                        if (state.isPlaying) 1f else 0.8f,
                        spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessLow),
                        label = "artScale",
                    )
                    Artwork(
                        art,
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .shadow(if (state.isPlaying) 28.dp else 12.dp, RoundedCornerShape(12.dp)),
                        corner = 12.dp,
                    )
                }
            }

            Spacer(Modifier.height(26.dp))
            TitleRow(vm, song, state)
            Spacer(Modifier.height(18.dp))
            ProgressSection(vm, state)
            Spacer(Modifier.height(10.dp))
            Controls(vm, state)
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = { showQueue = !showQueue }) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (showQueue) Color.White.copy(alpha = 0.25f) else Color.Transparent),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.QueueMusic,
                            contentDescription = "A continuación",
                            tint = Color.White,
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TitleRow(vm: MainViewModel, song: Song?, state: PlayerState) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                song?.title ?: state.title,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.basicMarquee(initialDelayMillis = 2500),
            )
            Text(
                song?.artist ?: state.artist,
                color = White60,
                fontSize = 20.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (song != null) {
            var open by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { open = true }) {
                    Box(
                        Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.MoreHoriz, "Opciones", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
                DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                    SongMenuItems(vm, song, { open = false })
                }
            }
        }
    }
}

@Composable
private fun ProgressSection(vm: MainViewModel, state: PlayerState) {
    var position by remember { mutableLongStateOf(vm.player.positionMs) }
    var dragFraction by remember { mutableStateOf<Float?>(null) }

    LaunchedEffect(state.isPlaying, state.currentId) {
        while (true) {
            position = vm.player.positionMs
            if (!state.isPlaying) break
            delay(250)
        }
    }

    val duration = state.durationMs.coerceAtLeast(1L)
    val fraction = dragFraction ?: (position.toFloat() / duration).coerceIn(0f, 1f)
    val shown = (fraction * duration).toLong()

    ThinSlider(
        fraction = fraction,
        onDrag = { dragFraction = it },
        onRelease = { f ->
            val target = (f * duration).toLong()
            vm.player.seekTo(target)
            position = target
            dragFraction = null
        },
    )
    Row(Modifier.fillMaxWidth()) {
        Text(formatTime(shown), color = White60, fontSize = 12.sp)
        Spacer(Modifier.weight(1f))
        Text("-" + formatTime(state.durationMs - shown), color = White60, fontSize = 12.sp)
    }
}

/** Barra de progreso delgada, sin "bolita", que se ensancha al arrastrarla. */
@Composable
private fun ThinSlider(fraction: Float, onDrag: (Float) -> Unit, onRelease: (Float) -> Unit) {
    var active by remember { mutableStateOf(false) }
    var last by remember { mutableFloatStateOf(0f) }
    val height by animateDpAsState(if (active) 11.dp else 6.dp, label = "barHeight")
    val drag by rememberUpdatedState(onDrag)
    val release by rememberUpdatedState(onRelease)

    Box(
        Modifier
            .fillMaxWidth()
            .height(30.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { o ->
                        active = true
                        last = (o.x / size.width).coerceIn(0f, 1f)
                        drag(last)
                    },
                    onDragEnd = { active = false; release(last) },
                    onDragCancel = { active = false; release(last) },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        last = (change.position.x / size.width).coerceIn(0f, 1f)
                        drag(last)
                    },
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { o -> release((o.x / size.width).coerceIn(0f, 1f)) }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(height)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.22f))
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .background(Color.White.copy(alpha = if (active) 1f else 0.8f))
            )
        }
    }
}

@Composable
private fun Controls(vm: MainViewModel, state: PlayerState) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SmallToggle(Icons.Rounded.Shuffle, state.shuffle, "Aleatorio") { vm.player.toggleShuffle() }
        BigControl(Icons.Rounded.SkipPrevious, 46.dp, "Anterior") { vm.player.previous() }
        BigControl(
            if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            66.dp,
            if (state.isPlaying) "Pausa" else "Reproducir",
        ) { vm.player.toggle() }
        BigControl(Icons.Rounded.SkipNext, 46.dp, "Siguiente") { vm.player.next() }
        SmallToggle(
            if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
            state.repeatMode != Player.REPEAT_MODE_OFF,
            "Repetir",
        ) { vm.player.cycleRepeat() }
    }
}

@Composable
private fun BigControl(icon: ImageVector, size: Dp, label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(size + 20.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(size))
    }
}

@Composable
private fun SmallToggle(icon: ImageVector, on: Boolean, label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (on) Color.White.copy(alpha = 0.25f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (on) Color.White else White60,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun QueuePanel(vm: MainViewModel, lib: Library, state: PlayerState) {
    Column(Modifier.fillMaxSize()) {
        Text(
            "A continuación",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        if (state.upcoming.isEmpty()) {
            Text("No hay más canciones en la cola.", color = White60, fontSize = 15.sp)
        }
        LazyColumn(Modifier.weight(1f)) {
            items(state.upcoming, key = { it.index }) { entry ->
                val s = lib.byId[entry.songId] ?: return@items
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { vm.player.skipTo(entry.index) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Artwork(s.art, Modifier.size(44.dp), corner = 5.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.title, color = Color.White, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(s.artist, color = White60, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
