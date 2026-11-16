package com.sonora.player.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sonora.player.MainViewModel
import com.sonora.player.data.MediaScanner
import java.io.File

/**
 * Pantalla para crear o editar un álbum propio o una playlist:
 * nombre, artista, año y carátula elegida desde la galería.
 */
@Composable
fun EditorScreen(vm: MainViewModel, nav: androidx.navigation.NavController, isAlbum: Boolean, id: Long) {
    val lib = remember { vm.library.value }
    val existingAlbum = remember { if (isAlbum && id > 0) lib.albums.firstOrNull { it.customAlbumId == id } else null }
    val existingPlaylist = remember { if (!isAlbum && id > 0) lib.playlist(id) else null }
    val pending = remember { vm.pendingSongIds.mapNotNull { lib.byId[it] } }
    val isNew = id <= 0

    val suggestedArtist = remember {
        pending.map { it.artist }
            .filter { it != MediaScanner.UNKNOWN_ARTIST }
            .groupingBy { it }.eachCount().maxByOrNull { it.value }?.key.orEmpty()
    }

    var name by rememberSaveable { mutableStateOf(existingAlbum?.title ?: existingPlaylist?.name ?: "") }
    var artist by rememberSaveable { mutableStateOf(existingAlbum?.artist ?: suggestedArtist) }
    var year by rememberSaveable {
        mutableStateOf(existingAlbum?.year?.takeIf { it > 0 }?.toString() ?: "")
    }
    var newCover by remember { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) newCover = uri
    }
    val existingCover = existingAlbum?.coverPath ?: existingPlaylist?.coverPath
    val preview: Any? = newCover ?: existingCover?.let { File(it) }

    val title = when {
        isAlbum && isNew -> "Nuevo álbum"
        isAlbum -> "Editar álbum"
        isNew -> "Nueva playlist"
        else -> "Editar playlist"
    }

    fun save() {
        val y = year.trim().toIntOrNull() ?: 0
        when {
            isAlbum && isNew -> vm.createAlbum(name, artist, y, newCover) { newId ->
                nav.navigate("album/c$newId") { popUpTo("editAlbum/{id}") { inclusive = true } }
            }
            isAlbum -> {
                vm.updateAlbum(id, name, artist, y, newCover)
                nav.popBackStack()
            }
            isNew -> vm.createPlaylist(name, newCover) { newId ->
                nav.navigate("playlist/$newId") { popUpTo("editPlaylist/{id}") { inclusive = true } }
            }
            else -> {
                vm.updatePlaylist(id, name, newCover)
                nav.popBackStack()
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(52.dp)
        ) {
            TextButton(onClick = { nav.popBackStack() }, modifier = Modifier.align(Alignment.CenterStart)) {
                Text("Cancelar", color = MaterialTheme.colorScheme.primary, fontSize = 17.sp)
            }
            Text(
                title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Center),
            )
            TextButton(
                onClick = { save() },
                enabled = name.isNotBlank(),
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                Text(
                    "Listo",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = if (name.isNotBlank()) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier
                    .size(230.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { picker.launch("image/*") },
                contentAlignment = Alignment.Center,
            ) {
                if (preview != null) {
                    AsyncImage(
                        model = preview,
                        contentDescription = "Carátula",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.AddPhotoAlternate,
                            null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(52.dp),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Elegir carátula", color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                    }
                }
            }
            if (preview != null) {
                TextButton(onClick = { picker.launch("image/*") }) {
                    Text("Cambiar carátula", color = MaterialTheme.colorScheme.primary)
                }
            }
            Text(
                "La imagen se recorta en cuadrado automáticamente.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            IosField(
                value = name,
                onChange = { name = it },
                placeholder = if (isAlbum) "Nombre del álbum" else "Nombre de la playlist",
            )
            if (isAlbum) {
                IosField(value = artist, onChange = { artist = it }, placeholder = "Artista")
                IosField(
                    value = year,
                    onChange = { v -> year = v.filter { it.isDigit() }.take(4) },
                    placeholder = "Año (opcional)",
                    keyboardType = KeyboardType.Number,
                )
            }
            if (isNew && pending.isNotEmpty()) {
                Text(
                    if (pending.size == 1) "Se agregará 1 canción." else "Se agregarán ${pending.size} canciones.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun IosField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(placeholder) },
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        keyboardOptions = KeyboardOptions(
            capitalization = if (keyboardType == KeyboardType.Text) KeyboardCapitalization.Sentences
            else KeyboardCapitalization.None,
            keyboardType = keyboardType,
        ),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}
