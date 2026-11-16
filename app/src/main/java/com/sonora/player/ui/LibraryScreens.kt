package com.sonora.player.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.MicExternalOn
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.sonora.player.MainViewModel

// ---------------------------------------------------------------- Biblioteca

@Composable
fun LibraryHomeScreen(vm: MainViewModel, nav: NavController) {
    val lib by vm.library.collectAsStateWithLifecycle()
    val recent = remember(lib.albums) {
        lib.albums.filter { it.songs.isNotEmpty() }.sortedByDescending { it.dateAdded }.take(10)
    }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = LocalBottomInset.current),
    ) {
        item {
            Column {
                Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
                Spacer(Modifier.height(12.dp))
                LargeTitle("Biblioteca")
            }
        }
        item { LibraryLink(Icons.AutoMirrored.Rounded.QueueMusic, "Playlists") { nav.navigate("playlists") } }
        item { LibraryLink(Icons.Rounded.MicExternalOn, "Artistas") { nav.navigate("artists") } }
        item { LibraryLink(Icons.Rounded.Album, "Álbumes") { nav.navigate("albums") } }
        item { LibraryLink(Icons.Rounded.MusicNote, "Canciones") { nav.navigate("songs") } }

        if (lib.loaded && lib.songs.isEmpty()) {
            item {
                EmptyMessage(
                    "No hay música",
                    "Guarda canciones MP3 en tu teléfono y aparecerán aquí automáticamente.",
                )
            }
        }
        if (recent.isNotEmpty()) {
            item { SectionTitle("Agregado recientemente") }
            items(recent.chunked(2)) { row ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    row.forEach { album ->
                        AlbumGridItem(album, { nav.navigate("album/${album.key}") }, Modifier.weight(1f))
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun LibraryLink(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(16.dp))
            Text(label, fontSize = 20.sp, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider(
            Modifier.padding(start = 62.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

// ---------------------------------------------------------------- Canciones

/**
 * Lista de canciones. Mantén presionada una canción para seleccionar varias y
 * crear un álbum o una playlist con ellas. En modo "agregar" (pickAlbumId / pickPlaylistId)
 * sirve para elegir canciones para un álbum o playlist existente.
 */
@Composable
fun SongsScreen(vm: MainViewModel, nav: NavController, pickAlbumId: Long?, pickPlaylistId: Long?) {
    val lib by vm.library.collectAsStateWithLifecycle()
    val state by vm.player.state.collectAsStateWithLifecycle()
    val pickMode = pickAlbumId != null || pickPlaylistId != null
    var selection by remember { mutableStateOf(if (pickMode) emptySet<Long>() else null) }
    val sel = selection

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            NavBar(
                onBack = { nav.popBackStack() },
                title = when {
                    sel == null -> ""
                    sel.isEmpty() -> "Elige canciones"
                    else -> "${sel.size} seleccionadas"
                },
                backLabel = if (pickMode) "Cancelar" else "Biblioteca",
            ) {
                if (!pickMode) {
                    TextButton(onClick = { selection = if (sel == null) emptySet() else null }) {
                        Text(if (sel == null) "Seleccionar" else "Listo", color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                    }
                }
            }
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = LocalBottomInset.current + if (sel != null) 72.dp else 0.dp),
            ) {
                if (sel == null) {
                    item { LargeTitle("Canciones") }
                    item {
                        PlayShuffleButtons(
                            onPlay = { vm.play(lib.songs) },
                            onShuffle = { vm.play(lib.songs, shuffle = true) },
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        )
                    }
                    if (sel == null && !pickMode && lib.songs.isNotEmpty()) {
                        item {
                            Text(
                                "Consejo: mantén presionada una canción para seleccionar varias y crear un álbum.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
                itemsIndexed(lib.songs, key = { _, s -> s.id }) { index, song ->
                    SongRow(
                        song = song,
                        isCurrent = state.currentId == song.id,
                        selected = sel?.contains(song.id),
                        onClick = {
                            if (sel != null) {
                                selection = if (song.id in sel) sel - song.id else sel + song.id
                            } else {
                                vm.play(lib.songs, index)
                            }
                        },
                        onLongClick = if (pickMode) null else {
                            { if (sel == null) selection = setOf(song.id) }
                        },
                        menu = { dismiss -> SongMenuItems(vm, song, dismiss) },
                    )
                }
            }
        }

        if (sel != null && sel.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 10.dp,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 12.dp, end = 12.dp, bottom = LocalBottomInset.current)
                    .fillMaxWidth(),
            ) {
                Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (pickMode) {
                        Button(
                            onClick = {
                                val ids = sel.toList()
                                if (pickAlbumId != null) vm.addToCustomAlbum(pickAlbumId, ids)
                                if (pickPlaylistId != null) vm.addToPlaylist(pickPlaylistId, vm.orderIds(ids))
                                nav.popBackStack()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(48.dp),
                        ) { Text("Agregar ${sel.size}", fontWeight = FontWeight.SemiBold) }
                    } else {
                        ActionButton("Crear álbum", Icons.Rounded.Album, Modifier.weight(1f)) {
                            vm.albumSheet = sel.toList(); selection = null
                        }
                        ActionButton("A playlist", Icons.AutoMirrored.Rounded.PlaylistAdd, Modifier.weight(1f)) {
                            vm.playlistSheet = sel.toList(); selection = null
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionButton(text: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary,
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.height(48.dp),
    ) {
        Icon(icon, null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

// ---------------------------------------------------------------- Álbumes

@Composable
fun AlbumsScreen(vm: MainViewModel, nav: NavController) {
    val lib by vm.library.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        NavBar(onBack = { nav.popBackStack() }, backLabel = "Biblioteca") {
            IconButton(onClick = {
                vm.pendingSongIds = emptyList()
                nav.navigate("editAlbum/-1")
            }) {
                Icon(Icons.Rounded.Add, contentDescription = "Nuevo álbum", tint = MaterialTheme.colorScheme.primary)
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = LocalBottomInset.current),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) { LargeTitle("Álbumes", horizontalPadding = 0.dp) }
            items(lib.albums, key = { it.key }) { album ->
                AlbumGridItem(album, { nav.navigate("album/${album.key}") })
            }
        }
    }
}

// ---------------------------------------------------------------- Artistas

@Composable
fun ArtistsScreen(vm: MainViewModel, nav: NavController) {
    val lib by vm.library.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        NavBar(onBack = { nav.popBackStack() }, backLabel = "Biblioteca")
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = LocalBottomInset.current)) {
            item { LargeTitle("Artistas") }
            items(lib.artists, key = { it.name }) { artist ->
                Column(Modifier.clickable { nav.navigate("artist/${Uri.encode(artist.name)}") }) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Artwork(artist.art, Modifier.size(48.dp), shape = CircleShape)
                        Spacer(Modifier.width(14.dp))
                        Text(
                            artist.name,
                            fontSize = 17.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider(
                        Modifier.padding(start = 78.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Playlists

@Composable
fun PlaylistsScreen(vm: MainViewModel, nav: NavController) {
    val lib by vm.library.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        NavBar(onBack = { nav.popBackStack() }, backLabel = "Biblioteca")
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = LocalBottomInset.current)) {
            item { LargeTitle("Playlists") }
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            vm.pendingSongIds = emptyList()
                            nav.navigate("editPlaylist/-1")
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Text("Nueva playlist…", color = MaterialTheme.colorScheme.primary, fontSize = 17.sp)
                }
            }
            items(lib.playlists, key = { it.id }) { pl ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { nav.navigate("playlist/${pl.id}") }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlaylistCover(pl, Modifier.size(64.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            pl.name,
                            fontSize = 17.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            if (pl.songs.size == 1) "1 canción" else "${pl.songs.size} canciones",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
