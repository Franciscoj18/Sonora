package com.sonora.player.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.sonora.player.MainViewModel
import com.sonora.player.data.MediaScanner
import com.sonora.player.data.Song

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToAlbumSheet(vm: MainViewModel, nav: NavController, songIds: List<Long>) {
    val lib by vm.library.collectAsStateWithLifecycle()
    val albums = lib.customAlbums
    ModalBottomSheet(
        onDismissRequest = { vm.albumSheet = null },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        SheetTitle(if (songIds.size == 1) "Agregar a un álbum" else "Agregar ${songIds.size} canciones a un álbum")
        LazyColumn {
            item {
                NewRow("Nuevo álbum…") {
                    vm.pendingSongIds = vm.orderIds(songIds)
                    vm.albumSheet = null
                    nav.navigate("editAlbum/-1")
                }
            }
            items(albums, key = { it.key }) { album ->
                SheetRow(album.art, album.title, "${album.songs.size} canciones") {
                    album.customAlbumId?.let { vm.addToCustomAlbum(it, songIds) }
                    vm.albumSheet = null
                }
            }
            if (albums.isEmpty()) {
                item {
                    Text(
                        "Aún no tienes álbumes propios. Crea uno para agrupar canciones descargadas.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(vm: MainViewModel, nav: NavController, songIds: List<Long>) {
    val lib by vm.library.collectAsStateWithLifecycle()
    ModalBottomSheet(
        onDismissRequest = { vm.playlistSheet = null },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        SheetTitle("Agregar a una playlist")
        LazyColumn {
            item {
                NewRow("Nueva playlist…") {
                    vm.pendingSongIds = vm.orderIds(songIds)
                    vm.playlistSheet = null
                    nav.navigate("editPlaylist/-1")
                }
            }
            items(lib.playlists, key = { it.id }) { pl ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            vm.addToPlaylist(pl.id, vm.orderIds(songIds))
                            vm.playlistSheet = null
                        }
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlaylistCover(pl, Modifier.size(52.dp), corner = 6.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(pl.name, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${pl.songs.size} canciones",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun SheetTitle(text: String) {
    Text(
        text,
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun NewRow(text: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(text, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
    }
}

@Composable
private fun SheetRow(art: Any?, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(art, Modifier.size(52.dp), corner = 6.dp, seed = title)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ---------------------------------------------------------------- Editar canción

private val junkInTitle = Regex(
    """\s*[(\[][^)\]]*(official|oficial|video|vídeo|audio|lyric|letra|hd|hq|4k|visualizer|remaster)[^)\]]*[)\]]""",
    RegexOption.IGNORE_CASE,
)

/** Quita "(Official Video)", "[HD]" y similares, y separa "Artista - Canción". */
private fun cleanTitle(title: String, artist: String): Pair<String, String> {
    var t = junkInTitle.replace(title, "").trim()
    var a = artist
    val unknownArtist = artist.isBlank() || artist == MediaScanner.UNKNOWN_ARTIST
    if (unknownArtist && t.contains(" - ")) {
        a = t.substringBefore(" - ").trim()
        t = t.substringAfter(" - ").trim()
    }
    return t to a
}

@Composable
fun EditSongDialog(vm: MainViewModel, song: Song) {
    var title by remember(song.id) { mutableStateOf(song.title) }
    var artist by remember(song.id) { mutableStateOf(song.artist) }
    var track by remember(song.id) { mutableStateOf(if (song.track > 0) song.track.toString() else "") }

    AlertDialog(
        onDismissRequest = { vm.editingSong = null },
        title = { Text("Editar canción") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Artista") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = track,
                    onValueChange = { v -> track = v.filter { it.isDigit() }.take(3) },
                    label = { Text("Número de pista") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                TextButton(onClick = {
                    val (t, a) = cleanTitle(title, artist)
                    title = t
                    artist = a
                }) {
                    Icon(Icons.Rounded.AutoFixHigh, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Limpiar título de YouTube")
                }
                Text(
                    "Los cambios se guardan en la app; el archivo original no se modifica.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                vm.editSong(song.id, title, artist, track.toIntOrNull())
                vm.editingSong = null
            }) { Text("Guardar", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = { vm.editingSong = null }) { Text("Cancelar") }
        },
    )
}
