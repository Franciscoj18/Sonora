package com.sonora.player.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sonora.player.MainViewModel
import com.sonora.player.data.Album
import com.sonora.player.data.Playlist
import com.sonora.player.data.Song

/** Espacio inferior que ocupan el mini reproductor y la barra de pestañas. */
val LocalBottomInset = compositionLocalOf { 0.dp }

@Composable
fun Artwork(
    model: Any?,
    modifier: Modifier = Modifier,
    corner: Dp = 6.dp,
    shape: Shape = RoundedCornerShape(corner),
) {
    Box(
        modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier.fillMaxSize(0.42f),
        )
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun LargeTitle(
    text: String,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 20.dp,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = horizontalPadding, end = (horizontalPadding - 8.dp).coerceAtLeast(0.dp), top = 6.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            style = TextStyle(
                fontFamily = InterDisplay,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        actions()
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 10.dp),
    )
}

/** Barra superior estilo iOS: "‹ Atrás" en color de acento. */
@Composable
fun NavBar(
    onBack: (() -> Unit)?,
    title: String = "",
    backLabel: String = "Atrás",
    actions: @Composable RowScope.() -> Unit = {},
) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .height(48.dp)
    ) {
        if (onBack != null) {
            Row(
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onBack)
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.ArrowBackIosNew,
                    contentDescription = "Atrás",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(backLabel, color = MaterialTheme.colorScheme.primary, fontSize = 17.sp)
            }
        }
        Text(
            title,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 96.dp),
        )
        Row(
            Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = actions,
        )
    }
}

@Composable
fun MoreButton(content: @Composable (dismiss: () -> Unit) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Rounded.MoreHoriz, contentDescription = "Más", tint = MaterialTheme.colorScheme.primary)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            content { open = false }
        }
    }
}

@Composable
fun MenuItem(text: String, icon: ImageVector? = null, destructive: Boolean = false, onClick: () -> Unit) {
    val color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    DropdownMenuItem(
        text = { Text(text, color = color) },
        leadingIcon = if (icon != null) {
            { Icon(icon, contentDescription = null, tint = color) }
        } else {
            null
        },
        onClick = onClick,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongRow(
    song: Song,
    isCurrent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trackNumber: Int? = null,
    selected: Boolean? = null,
    onLongClick: (() -> Unit)? = null,
    menu: (@Composable (dismiss: () -> Unit) -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .height(60.dp)
                .padding(start = 16.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected != null) {
                Icon(
                    if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (selected) colors.primary else colors.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(12.dp))
            }
            if (trackNumber != null) {
                Box(Modifier.width(28.dp), contentAlignment = Alignment.CenterStart) {
                    if (isCurrent) {
                        Icon(Icons.Rounded.GraphicEq, null, tint = colors.primary, modifier = Modifier.size(18.dp))
                    } else {
                        Text("$trackNumber", color = colors.onSurfaceVariant, fontSize = 15.sp)
                    }
                }
            } else {
                Artwork(song.art, Modifier.size(48.dp), corner = 6.dp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    song.title,
                    color = if (isCurrent) colors.primary else colors.onBackground,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (trackNumber == null) {
                    Text(
                        song.artist,
                        color = colors.onSurfaceVariant,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (menu != null && selected == null) {
                var open by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { open = true }) {
                        Icon(Icons.Rounded.MoreHoriz, contentDescription = "Opciones", tint = colors.onBackground)
                    }
                    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                        menu { open = false }
                    }
                }
            } else {
                Spacer(Modifier.width(12.dp))
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = if (selected != null) 116.dp else if (trackNumber != null) 56.dp else 76.dp),
            thickness = 0.5.dp,
            color = colors.outlineVariant,
        )
    }
}

/** Opciones del menú "⋯" de una canción. */
@Composable
fun SongMenuItems(vm: MainViewModel, song: Song, dismiss: () -> Unit, playlistId: Long? = null) {
    MenuItem("Reproducir a continuación", Icons.Rounded.SkipNext) { vm.playNext(song); dismiss() }
    MenuItem("Agregar a la cola", Icons.AutoMirrored.Rounded.QueueMusic) { vm.addToQueue(song); dismiss() }
    MenuItem("Agregar a una playlist…", Icons.AutoMirrored.Rounded.PlaylistAdd) {
        vm.playlistSheet = listOf(song.id); dismiss()
    }
    MenuItem("Agregar a un álbum…", Icons.Rounded.Album) { vm.albumSheet = listOf(song.id); dismiss() }
    MenuItem("Editar información", Icons.Rounded.Edit) { vm.editingSong = song; dismiss() }
    if (song.customAlbumId != null) {
        MenuItem("Quitar del álbum", Icons.Rounded.RemoveCircleOutline) {
            vm.removeFromCustomAlbum(song.id); dismiss()
        }
    }
    if (playlistId != null) {
        MenuItem("Quitar de la playlist", Icons.Rounded.RemoveCircleOutline, destructive = true) {
            vm.removeFromPlaylist(playlistId, song.id); dismiss()
        }
    }
}

@Composable
fun PlayShuffleButtons(onPlay: () -> Unit, onShuffle: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        PillButton("Reproducir", Icons.Rounded.PlayArrow, onPlay, Modifier.weight(1f))
        PillButton("Aleatorio", Icons.Rounded.Shuffle, onShuffle, Modifier.weight(1f))
    }
}

@Composable
fun PillButton(text: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.height(48.dp),
    ) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(6.dp))
            Text(text, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}

@Composable
fun AlbumGridItem(album: Album, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Artwork(album.art, Modifier.fillMaxWidth().aspectRatio(1f), corner = 8.dp)
        Spacer(Modifier.height(6.dp))
        Text(
            album.title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            album.artist,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
    }
}

/** Carátula de playlist: la elegida por el usuario o un mosaico con las primeras portadas. */
@Composable
fun PlaylistCover(playlist: Playlist, modifier: Modifier = Modifier, corner: Dp = 8.dp) {
    val cover = playlist.coverPath
    if (cover != null) {
        Artwork(java.io.File(cover), modifier, corner = corner)
        return
    }
    val arts = remember(playlist.songs) { playlist.songs.map { it.art }.distinct().take(4) }
    if (arts.size < 4) {
        Artwork(arts.firstOrNull(), modifier, corner = corner)
    } else {
        Column(modifier.clip(RoundedCornerShape(corner))) {
            Row(Modifier.weight(1f)) {
                Artwork(arts[0], Modifier.weight(1f).fillMaxSize(), corner = 0.dp)
                Artwork(arts[1], Modifier.weight(1f).fillMaxSize(), corner = 0.dp)
            }
            Row(Modifier.weight(1f)) {
                Artwork(arts[2], Modifier.weight(1f).fillMaxSize(), corner = 0.dp)
                Artwork(arts[3], Modifier.weight(1f).fillMaxSize(), corner = 0.dp)
            }
        }
    }
}

@Composable
fun EmptyMessage(title: String, message: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Rounded.MusicNote,
            null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(6.dp))
        Text(
            message,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

fun formatTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

fun formatTotal(count: Int, ms: Long): String {
    val songs = if (count == 1) "1 canción" else "$count canciones"
    val minutes = ms / 60000
    val time = if (minutes >= 60) {
        val h = minutes / 60
        val m = minutes % 60
        (if (h == 1L) "1 hora" else "$h horas") + if (m > 0) " $m min" else ""
    } else {
        "$minutes minutos"
    }
    return "$songs, $time"
}
