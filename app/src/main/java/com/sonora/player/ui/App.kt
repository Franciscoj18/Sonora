package com.sonora.player.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sonora.player.MainViewModel
import com.sonora.player.data.Song
import com.sonora.player.playback.PlayerState

private val audioPermission =
    if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
    else Manifest.permission.READ_EXTERNAL_STORAGE

@Composable
fun SonoraRoot(vm: MainViewModel = viewModel()) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        granted = result[audioPermission] == true
    }
    LaunchedEffect(granted) { if (granted) vm.rescan() }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (granted) {
            MainContent(vm)
        } else {
            PermissionScreen {
                val perms = buildList {
                    add(audioPermission)
                    if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
                }
                launcher.launch(perms.toTypedArray())
            }
        }
    }
}

private val hiddenTabRoutes = setOf("editAlbum/{id}", "editPlaylist/{id}")

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun MainContent(vm: MainViewModel) {
    val nav = rememberNavController()
    val lib by vm.library.collectAsStateWithLifecycle()
    val state by vm.player.state.collectAsStateWithLifecycle()
    val current = state.currentId?.let { lib.byId[it] }
    val hasPlayer = state.currentId != null
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBars = route == null || route !in hiddenTabRoutes

    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomInset = navBottom + 8.dp +
        (if (showBars) 56.dp else 0.dp) +
        (if (showBars && hasPlayer) 72.dp else 0.dp)

    val push = tween<androidx.compose.ui.unit.IntOffset>(280)

    SharedTransitionLayout(Modifier.fillMaxSize()) {
    val sharedScope = this
    Box(Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalBottomInset provides bottomInset, LocalIsPlaying provides state.isPlaying) {
            NavHost(
                navController = nav,
                startDestination = "library",
                modifier = Modifier.fillMaxSize(),
                enterTransition = { slideInHorizontally(push) { it } },
                exitTransition = { slideOutHorizontally(push) { -it / 4 } + fadeOut(tween(280)) },
                popEnterTransition = { slideInHorizontally(push) { -it / 4 } + fadeIn(tween(280)) },
                popExitTransition = { slideOutHorizontally(push) { it } },
            ) {
                composable("library") { LibraryHomeScreen(vm, nav) }
                composable("search") { SearchScreen(vm, nav) }
                composable("songs") { SongsScreen(vm, nav, null, null) }
                composable(
                    "pick?album={album}&playlist={playlist}",
                    arguments = listOf(
                        navArgument("album") { type = NavType.LongType; defaultValue = -1L },
                        navArgument("playlist") { type = NavType.LongType; defaultValue = -1L },
                    ),
                ) { entry ->
                    val album = entry.arguments?.getLong("album")?.takeIf { it > 0 }
                    val playlist = entry.arguments?.getLong("playlist")?.takeIf { it > 0 }
                    SongsScreen(vm, nav, album, playlist)
                }
                composable("albums") { AlbumsScreen(vm, nav) }
                composable("album/{key}") { entry ->
                    AlbumDetailScreen(vm, nav, entry.arguments?.getString("key").orEmpty())
                }
                composable("artists") { ArtistsScreen(vm, nav) }
                composable("artist/{name}") { entry ->
                    ArtistDetailScreen(vm, nav, entry.arguments?.getString("name").orEmpty())
                }
                composable("playlists") { PlaylistsScreen(vm, nav) }
                composable(
                    "playlist/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.LongType }),
                ) { entry ->
                    PlaylistDetailScreen(vm, nav, entry.arguments?.getLong("id") ?: -1L)
                }
                composable(
                    "editAlbum/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.LongType }),
                ) { entry ->
                    EditorScreen(vm, nav, isAlbum = true, id = entry.arguments?.getLong("id") ?: -1L)
                }
                composable(
                    "editPlaylist/{id}",
                    arguments = listOf(navArgument("id") { type = NavType.LongType }),
                ) { entry ->
                    EditorScreen(vm, nav, isAlbum = false, id = entry.arguments?.getLong("id") ?: -1L)
                }
            }
        }

        if (showBars) {
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                if (hasPlayer) {
                    AnimatedVisibility(
                        visible = !vm.showNowPlaying,
                        enter = fadeIn(tween(220, delayMillis = 120)),
                        exit = fadeOut(tween(120)),
                    ) {
                        MiniPlayer(
                            song = current,
                            state = state,
                            sharedScope = sharedScope,
                            visibilityScope = this,
                            onOpen = { vm.showNowPlaying = true },
                            onToggle = { vm.player.toggle() },
                            onNext = { vm.player.next() },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                        )
                    }
                }
                TabBar(
                    searchSelected = route == "search",
                    onSelect = { target ->
                        nav.navigate(target) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        }

        AnimatedVisibility(
            visible = vm.showNowPlaying && hasPlayer,
            enter = slideInVertically(tween(330)) { it },
            exit = slideOutVertically(tween(260)) { it },
        ) {
            NowPlayingScreen(
                vm = vm,
                lib = lib,
                song = current,
                state = state,
                sharedScope = sharedScope,
                visibilityScope = this,
                onClose = { vm.showNowPlaying = false },
            )
        }

        vm.albumSheet?.let { AddToAlbumSheet(vm, nav, it) }
        vm.playlistSheet?.let { AddToPlaylistSheet(vm, nav, it) }
        vm.editingSong?.let { EditSongDialog(vm, it) }
    }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun MiniPlayer(
    song: Song?,
    state: PlayerState,
    sharedScope: SharedTransitionScope,
    visibilityScope: AnimatedVisibilityScope,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = isSystemInDarkTheme()
    val view = LocalView.current
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(16.dp),
        color = if (dark) Color(0xFF262628) else Color(0xFFF9F9FB),
        shadowElevation = 10.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
    ) {
        Row(Modifier.padding(start = 8.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            val sharedArt = with(sharedScope) {
                Modifier.sharedElement(
                    rememberSharedContentState(key = NOW_PLAYING_ART_KEY),
                    visibilityScope,
                )
            }
            Artwork(song?.art, sharedArt.size(48.dp), corner = 8.dp, seed = song?.album)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    song?.title ?: state.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    song?.artist ?: state.artist,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onToggle()
            }) {
                Icon(
                    if (state.isPlaying) SonoraIcons.Pause else SonoraIcons.Play,
                    contentDescription = if (state.isPlaying) "Pausa" else "Reproducir",
                    modifier = Modifier.size(26.dp),
                )
            }
            IconButton(onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onNext()
            }) {
                Icon(SonoraIcons.Next, contentDescription = "Siguiente", modifier = Modifier.size(26.dp))
            }
        }
    }
}

@Composable
private fun TabBar(searchSelected: Boolean, onSelect: (String) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background.copy(alpha = 0.97f)) {
        Column(Modifier.navigationBarsPadding()) {
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth().height(56.dp)) {
                TabItem(Icons.Rounded.LibraryMusic, "Biblioteca", !searchSelected, Modifier.weight(1f)) {
                    onSelect("library")
                }
                TabItem(Icons.Rounded.Search, "Buscar", searchSelected, Modifier.weight(1f)) {
                    onSelect("search")
                }
            }
        }
    }
}

@Composable
private fun TabItem(icon: ImageVector, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(26.dp))
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun PermissionScreen(onRequest: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Rounded.MusicNote,
            null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(84.dp),
        )
        Spacer(Modifier.height(20.dp))
        Text("Bienvenido a Sonora", fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(
            "Para mostrar tu música, Sonora necesita permiso para leer los archivos de audio de tu teléfono.",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onRequest,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            Text("Permitir acceso", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
