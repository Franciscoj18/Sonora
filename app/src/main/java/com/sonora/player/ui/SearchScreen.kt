package com.sonora.player.ui

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.sonora.player.MainViewModel
import java.text.Normalizer

private val accents = Regex("\\p{Mn}+")

private fun normalize(s: String): String =
    accents.replace(Normalizer.normalize(s, Normalizer.Form.NFD), "").lowercase()

@Composable
fun SearchScreen(vm: MainViewModel, nav: NavController) {
    val lib by vm.library.collectAsStateWithLifecycle()
    val state by vm.player.state.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }

    // Textos normalizados (sin acentos) calculados una sola vez por biblioteca
    val index = remember(lib) {
        Triple(
            lib.songs.map { normalize(it.title + " " + it.artist + " " + it.album) },
            lib.albums.map { normalize(it.title + " " + it.artist) },
            lib.artists.map { normalize(it.name) },
        )
    }
    val q = normalize(query.trim())
    val songs = remember(q, index) {
        if (q.isEmpty()) emptyList() else lib.songs.filterIndexed { i, _ -> index.first[i].contains(q) }.take(50)
    }
    val albums = remember(q, index) {
        if (q.isEmpty()) emptyList() else lib.albums.filterIndexed { i, _ -> index.second[i].contains(q) }.take(20)
    }
    val artists = remember(q, index) {
        if (q.isEmpty()) emptyList() else lib.artists.filterIndexed { i, _ -> index.third[i].contains(q) }.take(10)
    }

    Column(Modifier.fillMaxSize()) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        Spacer(Modifier.height(12.dp))
        LargeTitle("Buscar")
        TextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Artistas, canciones, álbumes") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Cancel, "Borrar") }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(top = 8.dp, bottom = LocalBottomInset.current),
        ) {
            if (q.isNotEmpty() && songs.isEmpty() && albums.isEmpty() && artists.isEmpty()) {
                item { EmptyMessage("Sin resultados", "Prueba con otra palabra.") }
            }
            if (artists.isNotEmpty()) {
                item { SectionTitle("Artistas") }
                items(artists, key = { "a" + it.name }) { artist ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { nav.navigate("artist/${Uri.encode(artist.name)}") }
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Artwork(artist.art, Modifier.size(48.dp), shape = CircleShape)
                        Spacer(Modifier.width(14.dp))
                        Text(artist.name, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            if (albums.isNotEmpty()) {
                item { SectionTitle("Álbumes") }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(albums, key = { it.key }) { album ->
                            AlbumGridItem(album, { nav.navigate("album/${album.key}") }, Modifier.width(150.dp))
                        }
                    }
                }
            }
            if (songs.isNotEmpty()) {
                item { SectionTitle("Canciones") }
                itemsIndexed(songs, key = { _, s -> "s" + s.id }) { i, song ->
                    SongRow(
                        song = song,
                        isCurrent = state.currentId == song.id,
                        onClick = { vm.play(songs, i) },
                        menu = { dismiss -> SongMenuItems(vm, song, dismiss) },
                    )
                }
            }
        }
    }
}
