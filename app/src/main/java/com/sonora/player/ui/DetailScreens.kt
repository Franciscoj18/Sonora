package com.sonora.player.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.HideImage
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LibraryAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.sonora.player.MainViewModel

// ---------------------------------------------------------------- Detalle de álbum

@Composable
fun AlbumDetailScreen(vm: MainViewModel, nav: NavController, key: String) {
    val lib by vm.library.collectAsStateWithLifecycle()
    val state by vm.player.state.collectAsStateWithLifecycle()
    val album = lib.album(key)
    var confirmDelete by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val collapsed by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    val glow = rememberArtGlow(album?.art)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val a = lib.album(key)
        if (uri != null && a != null) vm.setAlbumCover(a, uri)
    }

    if (album == null) {
        Column(Modifier.fillMaxSize()) {
            NavBar(onBack = { nav.popBackStack() })
            if (lib.loaded) EmptyMessage("Álbum no disponible", "Este álbum ya no existe o sus canciones se movieron.")
        }
        return
    }

    Column(Modifier.fillMaxSize()) {
        NavBar(
            onBack = { nav.popBackStack() },
            title = album.title,
            showTitle = collapsed,
            showDivider = collapsed,
        ) {
            MoreButton { dismiss ->
                MenuItem("Cambiar carátula", Icons.Rounded.Image) { picker.launch("image/*"); dismiss() }
                if (album.coverPath != null) {
                    MenuItem("Quitar carátula", Icons.Rounded.HideImage) { vm.removeAlbumCover(album); dismiss() }
                }
                val customId = album.customAlbumId
                if (customId != null) {
                    MenuItem("Editar álbum", Icons.Rounded.Edit) { nav.navigate("editAlbum/$customId"); dismiss() }
                    MenuItem("Agregar canciones", Icons.Rounded.Add) { nav.navigate("pick?album=$customId"); dismiss() }
                    MenuItem("Eliminar álbum", Icons.Rounded.Delete, destructive = true) { confirmDelete = true; dismiss() }
                } else {
                    MenuItem("Crear álbum propio con estas canciones", Icons.Rounded.LibraryAdd) {
                        vm.pendingSongIds = album.songs.map { it.id }
                        nav.navigate("editAlbum/-1")
                        dismiss()
                    }
                }
            }
        }
        LazyColumn(
            Modifier.weight(1f),
            state = listState,
            contentPadding = PaddingValues(bottom = LocalBottomInset.current),
        ) {
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(8.dp))
                    Artwork(
                        album.art,
                        Modifier
                            .size(250.dp)
                            .shadow(22.dp, RoundedCornerShape(10.dp), ambientColor = glow, spotColor = glow),
                        corner = 10.dp,
                        seed = album.title,
                    )
                    Spacer(Modifier.height(18.dp))
                    Text(
                        album.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                    Text(
                        album.artist,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                    val info = listOfNotNull(
                        if (album.isCustom) "Álbum propio" else null,
                        album.year.takeIf { it > 0 }?.toString(),
                    ).joinToString(" · ")
                    if (info.isNotEmpty()) {
                        Text(info, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    PlayShuffleButtons(
                        onPlay = { vm.play(album.songs) },
                        onShuffle = { vm.play(album.songs, shuffle = true) },
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 10.dp),
                    )
                }
            }
            if (album.songs.isEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        EmptyMessage("Álbum vacío", "Agrega canciones con el botón ⋯ o desde la lista de canciones.")
                        album.customAlbumId?.let { id ->
                            TextButton(onClick = { nav.navigate("pick?album=$id") }) {
                                Text("Agregar canciones", color = MaterialTheme.colorScheme.primary, fontSize = 17.sp)
                            }
                        }
                    }
                }
            }
            itemsIndexed(album.songs, key = { _, s -> s.id }) { index, song ->
                SongRow(
                    song = song,
                    isCurrent = state.currentId == song.id,
                    trackNumber = song.track.takeIf { it > 0 } ?: (index + 1),
                    onClick = { vm.play(album.songs, index) },
                    menu = { dismiss -> SongMenuItems(vm, song, dismiss) },
                )
            }
            if (album.songs.isNotEmpty()) {
                item {
                    Text(
                        formatTotal(album.songs.size, album.totalMs),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp),
                    )
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("¿Eliminar este álbum?") },
            text = { Text("Las canciones no se borran del teléfono; solo regresan a su álbum original.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    album.customAlbumId?.let { vm.deleteCustomAlbum(it) }
                    nav.popBackStack()
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } },
        )
    }
}

// ---------------------------------------------------------------- Detalle de artista

@Composable
fun ArtistDetailScreen(vm: MainViewModel, nav: NavController, name: String) {
    val lib by vm.library.collectAsStateWithLifecycle()
    val state by vm.player.state.collectAsStateWithLifecycle()
    val artist = lib.artist(name)
    val listState = rememberLazyListState()
    val collapsed by rememberCollapsed(listState)

    Column(Modifier.fillMaxSize()) {
        NavBar(
            onBack = { nav.popBackStack() },
            title = artist?.name.orEmpty(),
            backLabel = "Artistas",
            showTitle = collapsed,
            showDivider = collapsed,
        )
        if (artist == null) return@Column
        LazyColumn(
            Modifier.weight(1f),
            state = listState,
            contentPadding = PaddingValues(bottom = LocalBottomInset.current),
        ) {
            item { LargeTitle(artist.name) }
            item {
                PlayShuffleButtons(
                    onPlay = { vm.play(artist.songs) },
                    onShuffle = { vm.play(artist.songs, shuffle = true) },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                )
            }
            if (artist.albums.isNotEmpty()) {
                item { SectionTitle("Álbumes") }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(artist.albums, key = { it.key }) { album ->
                            AlbumGridItem(album, { nav.navigate("album/${album.key}") }, Modifier.width(160.dp))
                        }
                    }
                }
            }
            item { SectionTitle("Canciones") }
            itemsIndexed(artist.songs, key = { _, s -> s.id }) { index, song ->
                SongRow(
                    song = song,
                    isCurrent = state.currentId == song.id,
                    onClick = { vm.play(artist.songs, index) },
                    menu = { dismiss -> SongMenuItems(vm, song, dismiss) },
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Detalle de playlist

@Composable
fun PlaylistDetailScreen(vm: MainViewModel, nav: NavController, id: Long) {
    val lib by vm.library.collectAsStateWithLifecycle()
    val state by vm.player.state.collectAsStateWithLifecycle()
    val playlist = lib.playlist(id)
    var confirmDelete by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) vm.setPlaylistCover(id, uri)
    }

    if (playlist == null) {
        Column(Modifier.fillMaxSize()) { NavBar(onBack = { nav.popBackStack() }, backLabel = "Playlists") }
        return
    }

    val listState = rememberLazyListState()
    val collapsed by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }
    val glow = rememberArtGlow(playlist.coverPath?.let { java.io.File(it) } ?: playlist.songs.firstOrNull()?.art)
    Column(Modifier.fillMaxSize()) {
        NavBar(
            onBack = { nav.popBackStack() },
            title = playlist.name,
            backLabel = "Playlists",
            showTitle = collapsed,
            showDivider = collapsed,
        ) {
            MoreButton { dismiss ->
                MenuItem("Editar playlist", Icons.Rounded.Edit) { nav.navigate("editPlaylist/$id"); dismiss() }
                MenuItem("Cambiar carátula", Icons.Rounded.Image) { picker.launch("image/*"); dismiss() }
                if (playlist.coverPath != null) {
                    MenuItem("Quitar carátula", Icons.Rounded.HideImage) { vm.removePlaylistCover(id); dismiss() }
                }
                MenuItem("Agregar canciones", Icons.Rounded.Add) { nav.navigate("pick?playlist=$id"); dismiss() }
                MenuItem("Eliminar playlist", Icons.Rounded.Delete, destructive = true) { confirmDelete = true; dismiss() }
            }
        }
        LazyColumn(
            Modifier.weight(1f),
            state = listState,
            contentPadding = PaddingValues(bottom = LocalBottomInset.current),
        ) {
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(8.dp))
                    PlaylistCover(
                        playlist,
                        Modifier
                            .size(250.dp)
                            .shadow(22.dp, RoundedCornerShape(10.dp), ambientColor = glow, spotColor = glow),
                        corner = 10.dp,
                    )
                    Spacer(Modifier.height(18.dp))
                    Text(
                        playlist.name,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                    Text(
                        formatTotal(playlist.songs.size, playlist.totalMs),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (playlist.songs.isNotEmpty()) {
                        PlayShuffleButtons(
                            onPlay = { vm.play(playlist.songs) },
                            onShuffle = { vm.play(playlist.songs, shuffle = true) },
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 10.dp),
                        )
                    } else {
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = { nav.navigate("pick?playlist=$id") }) {
                            Text("Agregar canciones", color = MaterialTheme.colorScheme.primary, fontSize = 17.sp)
                        }
                    }
                }
            }
            itemsIndexed(playlist.songs, key = { _, s -> s.id }) { index, song ->
                SongRow(
                    song = song,
                    isCurrent = state.currentId == song.id,
                    onClick = { vm.play(playlist.songs, index) },
                    menu = { dismiss -> SongMenuItems(vm, song, dismiss, playlistId = id) },
                )
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("¿Eliminar esta playlist?") },
            text = { Text("Las canciones no se borran del teléfono.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.deletePlaylist(id)
                    nav.popBackStack()
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } },
        )
    }
}
