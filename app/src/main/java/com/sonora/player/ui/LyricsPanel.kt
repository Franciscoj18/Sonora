package com.sonora.player.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sonora.player.MainViewModel
import com.sonora.player.data.LrcParser
import com.sonora.player.data.LyricLine
import com.sonora.player.data.LyricsResult
import com.sonora.player.data.LyricsState
import com.sonora.player.data.MediaScanner
import com.sonora.player.data.Song
import com.sonora.player.playback.PlayerState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

private val Dim = Color.White.copy(alpha = 0.6f)

/** Desvanece suavemente la parte de arriba y de abajo de la letra. */
private fun Modifier.fadingEdges(): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        drawRect(
            Brush.verticalGradient(
                0f to Color.Transparent,
                0.07f to Color.Black,
                0.85f to Color.Black,
                1f to Color.Transparent,
            ),
            blendMode = BlendMode.DstIn,
        )
    }

private fun sourceLabel(source: String) = when (source) {
    "lrclib" -> "Letra de LRCLIB"
    "file" -> "Letra incluida en el archivo"
    else -> "Letra agregada por ti"
}

/**
 * Letra de la canción dentro del reproductor. Si es sincronizada, avanza sola con la
 * música y resalta la línea que se está cantando; toca una línea para saltar a esa parte.
 */
@Composable
fun LyricsPanel(vm: MainViewModel, song: Song?, state: PlayerState, modifier: Modifier = Modifier) {
    var lyricsState by remember(song?.id) { mutableStateOf<LyricsState>(LyricsState.Loading) }
    var version by remember { mutableIntStateOf(0) }
    var showSearch by remember { mutableStateOf(false) }
    var showEditor by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(song?.id, version) {
        lyricsState = LyricsState.Loading
        lyricsState = if (song != null) vm.lyricsFor(song) else LyricsState.NotFound
    }

    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && song != null) {
            scope.launch {
                val text = vm.readLyricsFile(uri)
                if (!text.isNullOrBlank()) {
                    vm.saveLyricsText(song.id, text)
                    version++
                }
            }
        }
    }
    val openSearch = { showSearch = true }
    val openEditor = { showEditor = true }
    val openImport = { importer.launch(arrayOf("*/*")) }

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Letra", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            if (song != null) {
                var open by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { open = true }) {
                        Box(
                            Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(15.dp))
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.MoreHoriz, "Opciones de letra", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                        MenuItem("Buscar letra", Icons.Rounded.Search) { open = false; openSearch() }
                        MenuItem("Pegar o editar letra", Icons.Rounded.Edit) { open = false; openEditor() }
                        MenuItem("Importar archivo .lrc", Icons.Rounded.Description) { open = false; openImport() }
                        if (lyricsState is LyricsState.Ready) {
                            MenuItem("Quitar letra", Icons.Rounded.Delete, destructive = true) {
                                open = false
                                scope.launch { vm.removeLyrics(song.id); version++ }
                            }
                        }
                    }
                }
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (val st = lyricsState) {
                LyricsState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                }
                LyricsState.NotFound -> EmptyLyrics(
                    title = "No encontramos la letra",
                    message = "Búscala con otras palabras, pégala o importa un archivo .lrc.",
                    onSearch = openSearch, onPaste = openEditor, onImport = openImport,
                )
                LyricsState.Offline -> EmptyLyrics(
                    title = "Sin conexión a internet",
                    message = "Conéctate para buscar la letra, o pégala tú mismo.",
                    onSearch = { version++ }, searchLabel = "Reintentar",
                    onPaste = openEditor, onImport = openImport,
                )
                is LyricsState.Ready -> {
                    val lyrics = st.lyrics
                    val lines = lyrics.synced
                    when {
                        !lines.isNullOrEmpty() -> SyncedLyrics(vm, state, lines, sourceLabel(lyrics.source))
                        !lyrics.plain.isNullOrBlank() -> PlainLyrics(lyrics.plain, sourceLabel(lyrics.source))
                        else -> EmptyLyrics(
                            title = "Instrumental",
                            message = "Esta canción no tiene letra.",
                            onSearch = openSearch, onPaste = openEditor, onImport = openImport,
                        )
                    }
                }
            }
        }
    }

    if (showSearch && song != null) {
        LyricsSearchSheet(
            vm = vm,
            song = song,
            onDismiss = { showSearch = false },
            onChosen = { showSearch = false; version++ },
        )
    }
    if (showEditor && song != null) {
        val initial = (lyricsState as? LyricsState.Ready)?.let { it.rawSynced ?: it.lyrics.plain }.orEmpty()
        LyricsEditorDialog(
            initial = initial,
            onDismiss = { showEditor = false },
            onSave = { text ->
                showEditor = false
                scope.launch { vm.saveLyricsText(song.id, text); version++ }
            },
        )
    }
}

@Composable
private fun SyncedLyrics(vm: MainViewModel, state: PlayerState, lines: List<LyricLine>, source: String) {
    var position by remember { mutableLongStateOf(vm.player.positionMs) }
    LaunchedEffect(state.isPlaying, state.currentId) {
        while (true) {
            position = vm.player.positionMs
            if (!state.isPlaying) break
            delay(200)
        }
    }
    val current = remember(position, lines) { lines.indexOfLast { it.timeMs <= position + 150 } }
    val listState = rememberLazyListState()

    // Si deslizas la letra con el dedo, deja de seguir la canción durante unos segundos
    var userScrollAt by remember { mutableLongStateOf(0L) }
    LaunchedEffect(listState) {
        listState.interactionSource.interactions.collect {
            if (it is DragInteraction.Start || it is DragInteraction.Stop) userScrollAt = System.currentTimeMillis()
        }
    }
    LaunchedEffect(current) {
        if (current >= 0 && System.currentTimeMillis() - userScrollAt > 3500) {
            listState.animateScrollToItem(current)
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(top = maxHeight * 0.26f, bottom = maxHeight * 0.62f),
            modifier = Modifier
                .fillMaxSize()
                .fadingEdges(),
        ) {
            itemsIndexed(lines) { i, line ->
                val active = i == current
                val lineAlpha by animateFloatAsState(
                    when {
                        active -> 1f
                        i < current -> 0.3f
                        else -> 0.42f
                    },
                    tween(350),
                    label = "lineAlpha",
                )
                val lineScale by animateFloatAsState(if (active) 1f else 0.94f, tween(350), label = "lineScale")
                Text(
                    line.text.ifBlank { "♪" },
                    color = Color.White,
                    fontSize = 26.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = lineAlpha
                            scaleX = lineScale
                            scaleY = lineScale
                            transformOrigin = TransformOrigin(0f, 0.5f)
                        }
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            vm.player.seekTo(line.timeMs)
                            position = line.timeMs
                            userScrollAt = 0L
                        }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                )
            }
            item {
                Text(source, color = Dim, fontSize = 12.sp, modifier = Modifier.padding(top = 24.dp, start = 4.dp))
            }
        }
    }
}

@Composable
private fun PlainLyrics(text: String, source: String) {
    Column(
        Modifier
            .fillMaxSize()
            .fadingEdges()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 18.dp, horizontal = 4.dp)
    ) {
        Text(
            text,
            color = Color.White.copy(alpha = 0.92f),
            fontSize = 22.sp,
            lineHeight = 31.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(20.dp))
        Text("$source · sin sincronizar", color = Dim, fontSize = 12.sp)
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun EmptyLyrics(
    title: String,
    message: String,
    onSearch: () -> Unit,
    onPaste: () -> Unit,
    onImport: () -> Unit,
    searchLabel: String = "Buscar letra",
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(message, color = Dim, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        GlassButton(searchLabel, Icons.Rounded.Search, onSearch)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassButton("Pegar letra", Icons.Rounded.ContentPaste, onPaste)
            GlassButton("Importar .lrc", Icons.Rounded.Description, onImport)
        }
    }
}

/** Botón translúcido que combina con el fondo del reproductor. */
@Composable
private fun GlassButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.16f))
            .border(0.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

// ---------------------------------------------------------------- Buscador

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LyricsSearchSheet(vm: MainViewModel, song: Song, onDismiss: () -> Unit, onChosen: () -> Unit) {
    val artist = song.artist.takeIf { it != MediaScanner.UNKNOWN_ARTIST }
    var query by remember { mutableStateOf(listOfNotNull(song.title, artist).joinToString(" ")) }
    var results by remember { mutableStateOf<List<LyricsResult>?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<LyricsResult?>(null) }
    val scope = rememberCoroutineScope()
    val songSec = song.durationMs / 1000.0

    fun runSearch() {
        val q = query.trim()
        if (q.isEmpty()) return
        scope.launch {
            loading = true
            error = null
            try {
                results = vm.searchLyrics(q)
            } catch (e: Exception) {
                results = null
                error = "No hay conexión a internet."
            }
            loading = false
        }
    }
    LaunchedEffect(Unit) { runSearch() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 420.dp)
                .padding(horizontal = 16.dp)
        ) {
            Text("Buscar letra", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text("Canción y artista") },
                trailingIcon = {
                    IconButton(onClick = { runSearch() }) { Icon(Icons.Rounded.Search, "Buscar") }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { runSearch() }),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            val list = results
            when {
                loading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                }
                error != null -> Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 16.dp))
                list != null && list.isEmpty() -> Text(
                    "Sin resultados. Prueba solo con el título, o revisa que esté bien escrito.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }
            if (!loading && !list.isNullOrEmpty()) {
                LazyColumn(Modifier.heightIn(max = 520.dp)) {
                    items(list, key = { it.id }) { r ->
                        ResultRow(r, matches = abs(r.durationSec - songSec) <= 3) { preview = r }
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    preview?.let { r ->
        AlertDialog(
            onDismissRequest = { preview = null },
            title = { Text(r.track, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            text = {
                Column(
                    Modifier
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        r.plain ?: r.synced?.let { LrcParser.toPlain(it) } ?: "Instrumental",
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        vm.chooseLyrics(song.id, r)
                        preview = null
                        onChosen()
                    }
                }) { Text("Usar esta letra", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { preview = null }) { Text("Volver") } },
        )
    }
}

@Composable
private fun ResultRow(r: LyricsResult, matches: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(r.track, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOf(r.artist, r.album).filter { it.isNotBlank() }.joinToString(" · "),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (r.synced != null) Badge("Sincronizada", MaterialTheme.colorScheme.primary)
                if (matches) Badge("Misma duración", Color(0xFF30A46C))
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            formatTime((r.durationSec * 1000).toLong()),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Badge(text: String, color: Color) {
    Text(
        text,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

// ---------------------------------------------------------------- Pegar / editar

@Composable
private fun LyricsEditorDialog(initial: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    val clipboard = LocalClipboardManager.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pegar o editar letra") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Pega aquí la letra…") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                )
                TextButton(onClick = {
                    clipboard.getText()?.text?.let { pasted ->
                        text = if (text.isBlank()) pasted else text + "\n" + pasted
                    }
                }) {
                    Icon(Icons.Rounded.ContentPaste, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Pegar del portapapeles")
                }
                Text(
                    "Si la letra trae tiempos como [01:23.45], se sincroniza sola con la canción.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }) { Text("Guardar", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
