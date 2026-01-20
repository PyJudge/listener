// Experimental API opt-ins required for:
// - Material3 Carousel APIs (HorizontalCenteredHeroCarousel, HorizontalUncontainedCarousel)
// - ModalBottomSheet, rememberModalBottomSheetState
// - Window Size Class APIs
// These will become stable in future Material3 releases
@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)

package chromahub.rhythm.app.features.local.presentation.screens

import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.carousel.HorizontalCenteredHeroCarousel
import androidx.compose.material3.carousel.HorizontalUncontainedCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.carousel.CarouselDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.LibraryAdd
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material.icons.rounded.PlayArrow // Or your specific icon
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import chromahub.rhythm.app.shared.presentation.components.common.CollapsibleHeaderScreen
import chromahub.rhythm.app.ui.theme.festive.FestiveConfig
import chromahub.rhythm.app.ui.theme.festive.FestiveThemeEngine
import chromahub.rhythm.app.ui.theme.festive.FestiveThemeType
import chromahub.rhythm.app.shared.data.model.AppSettings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import chromahub.rhythm.app.util.performIfEnabled
import chromahub.rhythm.app.util.HapticUtils
import chromahub.rhythm.app.R
import chromahub.rhythm.app.shared.data.model.Album
import chromahub.rhythm.app.shared.data.model.Artist
import chromahub.rhythm.app.shared.data.model.Song
import chromahub.rhythm.app.features.local.presentation.components.player.MiniPlayer
import chromahub.rhythm.app.shared.presentation.components.icons.RhythmIcons
import chromahub.rhythm.app.features.local.presentation.components.settings.HomeSectionOrderBottomSheet
import chromahub.rhythm.app.shared.presentation.components.common.M3PlaceholderType
import chromahub.rhythm.app.features.local.presentation.components.bottomsheets.ArtistBottomSheet
import chromahub.rhythm.app.features.local.presentation.components.bottomsheets.AlbumBottomSheet
import chromahub.rhythm.app.features.local.presentation.components.bottomsheets.AddToPlaylistBottomSheet
import chromahub.rhythm.app.features.local.presentation.components.bottomsheets.SongInfoBottomSheet
import chromahub.rhythm.app.util.ImageUtils
import chromahub.rhythm.app.shared.presentation.viewmodel.AppUpdaterViewModel
import chromahub.rhythm.app.shared.presentation.viewmodel.AppVersion
import chromahub.rhythm.app.features.local.presentation.viewmodel.MusicViewModel
import chromahub.rhythm.app.features.local.presentation.components.dialogs.CreatePlaylistDialog
import chromahub.rhythm.app.features.local.presentation.components.bottomsheets.AddToPlaylistBottomSheet
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import java.util.Calendar
import kotlin.random.Random
import androidx.core.text.HtmlCompat

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    songs: List<Song>,
    albums: List<Album>,
    artists: List<Artist>,
    recentlyPlayed: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    progress: Float,
    onSongClick: (Song) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onArtistClick: (Artist) -> Unit,
    onPlayPause: () -> Unit,
    onPlayerClick: () -> Unit,
    onViewAllSongs: () -> Unit,
    onViewAllAlbums: () -> Unit,
    onViewAllArtists: () -> Unit,
    onSkipNext: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onAppUpdateClick: (autoDownload: Boolean) -> Unit = { onSettingsClick() },
    onNavigateToLibrary: () -> Unit = {},
    onAddToQueue: (Song) -> Unit = {},
    onAddSongToPlaylist: (Song, String) -> Unit = { _, _ -> },
    onNavigateToPlaylist: (String) -> Unit = {},
    onCreatePlaylist: (String) -> Unit = { _ -> },
    onNavigateToStats: () -> Unit = {},
    updaterViewModel: AppUpdaterViewModel = viewModel()
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val coroutineScope = rememberCoroutineScope()
    val musicViewModel = viewModel<chromahub.rhythm.app.viewmodel.MusicViewModel>()
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val appSettings = remember { AppSettings.getInstance(context) }
    
    // Home header customization
    val headerDisplayMode by appSettings.homeHeaderDisplayMode.collectAsState()
    val showAppIcon by appSettings.homeShowAppIcon.collectAsState()
    val iconVisibilityMode by appSettings.homeAppIconVisibility.collectAsState()
    
    // State for artist bottom sheet
    var showArtistSheet by remember { mutableStateOf(false) }
    var selectedArtist by remember { mutableStateOf<Artist?>(null) }
    val artistSheetState = rememberModalBottomSheetState()

    // State for album bottom sheet
    var showAlbumBottomSheet by remember { mutableStateOf(false) }
    var selectedAlbum by remember { mutableStateOf<Album?>(null) }
    val albumSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    // State for AddToPlaylist bottom sheet
    var showAddToPlaylistSheet by remember { mutableStateOf(false) }
    var selectedSongForPlaylist by remember { mutableStateOf<Song?>(null) }
    val addToPlaylistSheetState = rememberModalBottomSheetState()
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    
    // Song info bottom sheet state
    var showSongInfoSheet by remember { mutableStateOf(false) }
    
    // Home section order bottom sheet state
    var showHomeSectionOrderSheet by remember { mutableStateOf(false) }
    
    // Select featured content from all albums (enhanced selection)
    // Note: Don't limit here, let the carousel use discoverItemCount setting
    val featuredContent = remember(albums) {
        albums.shuffled()
    }
    
    // Get all unique artists (already handled by repository based on user preference)
    val availableArtists = remember(artists) {
        artists.sortedBy { it.name }
    }
    
    val quickPicks = songs.take(8) // Increased for better variety
    val topArtists = availableArtists
    
    // Enhanced filtering for new releases
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val currentYearReleases = remember(albums, currentYear) {
        albums.filter { it.year == currentYear }
            .ifEmpty {
                albums.sortedByDescending { it.year }.take(6) // Increased count
            }
    }

    // Enhanced mood-based content
    val moodBasedSongs = songs.takeLast(15) // Increased for variety
    val energeticSongs = songs.take(12)
    val relaxingSongs = songs.drop(12).take(12)

    // Enhanced recently added songs
    val recentlyAddedSongs = remember(songs) {
        val oneMonthAgo = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }.timeInMillis
        songs.filter { it.dateAdded >= oneMonthAgo }
            .sortedByDescending { it.dateAdded }
    }

    // Enhanced recently added albums (for unified styling with new releases)
    val recentlyAddedAlbums = remember(albums, songs) {
        val oneMonthAgo = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }.timeInMillis
        val recentSongIds = songs.filter { it.dateAdded >= oneMonthAgo }.map { it.id }.toSet()
        albums.filter { album ->
            album.songs.any { song -> song.id in recentSongIds }
        }.sortedByDescending { album ->
            album.songs.mapNotNull { song ->
                if (song.id in recentSongIds) song.dateAdded else null
            }.maxOfOrNull { it } ?: 0L
        }
    }

    // Bottom sheet handlers (keeping same logic as original)
    if (showArtistSheet && selectedArtist != null) {
        ArtistBottomSheet(
            artist = selectedArtist!!,
            onDismiss = { showArtistSheet = false },
            onSongClick = { song: Song ->
                showArtistSheet = false
                onSongClick(song)
            },
            onAlbumClick = { album: Album ->
                showArtistSheet = false
                onAlbumClick(album)
            },
            onPlayAll = { songs ->
                if (songs.isNotEmpty()) {
                    musicViewModel.playQueue(songs)
                }
            },
            onShufflePlay = { songs ->
                if (songs.isNotEmpty()) {
                    musicViewModel.playShuffled(songs)
                }
            },
            onAddToQueue = { song ->
                onAddToQueue(song)
            },
            onAddSongToPlaylist = { song ->
                selectedSongForPlaylist = song
                coroutineScope.launch {
                    artistSheetState.hide()
                }.invokeOnCompletion {
                    if (!artistSheetState.isVisible) {
                        showArtistSheet = false
                        showAddToPlaylistSheet = true
                    }
                }
            },
            onPlayerClick = onPlayerClick,
            sheetState = artistSheetState,
            haptics = haptics,
            onPlayNext = { song -> musicViewModel.playNext(song) },
            onToggleFavorite = { song -> musicViewModel.toggleFavorite(song) },
            favoriteSongs = musicViewModel.favoriteSongs.collectAsState().value,
            onShowSongInfo = { song ->
                selectedSongForPlaylist = song
                coroutineScope.launch {
                    artistSheetState.hide()
                }.invokeOnCompletion {
                    if (!artistSheetState.isVisible) {
                        showArtistSheet = false
                        showSongInfoSheet = true
                    }
                }
            },
            onAddToBlacklist = { song ->
                val appSettings = AppSettings.getInstance(context)
                appSettings.addToBlacklist(song.id)
                Toast.makeText(context, "${song.title} added to blacklist", Toast.LENGTH_SHORT).show()
            },
            currentSong = currentSong,
            isPlaying = isPlaying
        )
    }

    if (showAlbumBottomSheet && selectedAlbum != null) {
        AlbumBottomSheet(
            album = selectedAlbum!!,
            onDismiss = { showAlbumBottomSheet = false },
            onSongClick = onSongClick,
            onPlayAll = { songsToPlay ->
                if (songsToPlay.isNotEmpty()) {
                    musicViewModel.playQueue(songsToPlay)
                }
                coroutineScope.launch {
                    albumSheetState.hide()
                }.invokeOnCompletion {
                    if (!albumSheetState.isVisible) {
                        showAlbumBottomSheet = false
                    }
                }
            },
            onShufflePlay = { songsToPlay ->
                if (songsToPlay.isNotEmpty()) {
                    musicViewModel.playShuffled(songsToPlay)
                }
                coroutineScope.launch {
                    albumSheetState.hide()
                }.invokeOnCompletion {
                    if (!albumSheetState.isVisible) {
                        showAlbumBottomSheet = false
                    }
                }
            },
            onAddToQueue = onAddToQueue,
            onAddSongToPlaylist = { song ->
                selectedSongForPlaylist = song
                coroutineScope.launch {
                    albumSheetState.hide()
                }.invokeOnCompletion {
                    if (!albumSheetState.isVisible) {
                        showAlbumBottomSheet = false
                        showAddToPlaylistSheet = true
                    }
                }
            },
            onPlayerClick = onPlayerClick,
            haptics = LocalHapticFeedback.current,
            sheetState = albumSheetState,
            onPlayNext = { song -> musicViewModel.playNext(song) },
            onToggleFavorite = { song -> musicViewModel.toggleFavorite(song) },
            favoriteSongs = musicViewModel.favoriteSongs.collectAsState().value,
            onShowSongInfo = { song ->
                selectedSongForPlaylist = song
                coroutineScope.launch {
                    albumSheetState.hide()
                }.invokeOnCompletion {
                    if (!albumSheetState.isVisible) {
                        showAlbumBottomSheet = false
                        showSongInfoSheet = true
                    }
                }
            },
            onAddToBlacklist = { song ->
                val appSettings = AppSettings.getInstance(context)
                appSettings.addToBlacklist(song.id)
                Toast.makeText(context, "${song.title} added to blacklist", Toast.LENGTH_SHORT).show()
            },
            currentSong = currentSong,
            isPlaying = isPlaying
        )
    }

    // Song info bottom sheet
    if (showSongInfoSheet && selectedSongForPlaylist != null) {
        SongInfoBottomSheet(
            song = selectedSongForPlaylist,
            onDismiss = { showSongInfoSheet = false },
            appSettings = AppSettings.getInstance(context),
            onEditSong = { title, artist, album, genre, year, trackNumber ->
                musicViewModel.saveMetadataChanges(
                    song = selectedSongForPlaylist!!,
                    title = title,
                    artist = artist,
                    album = album,
                    genre = genre,
                    year = year,
                    trackNumber = trackNumber,
                    onSuccess = { fileWriteSucceeded ->
                        if (fileWriteSucceeded) {
                            Toast.makeText(context, "Metadata saved successfully to file!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onError = { errorMessage ->
                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                    },
                    onPermissionRequired = { pendingRequest ->
                        Toast.makeText(context, "Permission required to modify file metadata", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        )
    }

    if (showAddToPlaylistSheet && selectedSongForPlaylist != null) {
        val musicViewModel = viewModel<chromahub.rhythm.app.viewmodel.MusicViewModel>()
        val playlists by musicViewModel.playlists.collectAsState()

        AddToPlaylistBottomSheet(
            song = selectedSongForPlaylist!!,
            playlists = playlists,
            onDismissRequest = { showAddToPlaylistSheet = false },
            onAddToPlaylist = { playlist ->
                onAddSongToPlaylist(selectedSongForPlaylist!!, playlist.id)
                coroutineScope.launch {
                    addToPlaylistSheetState.hide()
                }.invokeOnCompletion {
                    if (!addToPlaylistSheetState.isVisible) {
                        showAddToPlaylistSheet = false
                    }
                }
            },
            onCreateNewPlaylist = {
                coroutineScope.launch {
                    addToPlaylistSheetState.hide()
                }.invokeOnCompletion {
                    if (!addToPlaylistSheetState.isVisible) {
                        showAddToPlaylistSheet = false
                        showCreatePlaylistDialog = true
                    }
                }
            },
            sheetState = addToPlaylistSheetState
        )
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onConfirm = { name ->
                musicViewModel.createPlaylist(name)
                showCreatePlaylistDialog = false
            }
        )
    }
    
    if (showHomeSectionOrderSheet) {
        HomeSectionOrderBottomSheet(
            onDismiss = { showHomeSectionOrderSheet = false },
            appSettings = AppSettings.getInstance(context)
        )
    }

    CollapsibleHeaderScreen(
        title = context.getString(R.string.home_title),
        headerDisplayMode = headerDisplayMode,
        showAppIcon = showAppIcon,
        iconVisibilityMode = iconVisibilityMode,
        actions = {
            FilledIconButton(
                onClick = {
                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                    showHomeSectionOrderSheet = true
                },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Reorder,
                    contentDescription = context.getString(R.string.cd_reorder_home_sections)
                )
            }
            FilledIconButton(
                onClick = {
                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                    onSettingsClick()
                },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.padding(end = 16.dp)
            ) {
                Icon(
                    imageVector = RhythmIcons.Settings,
                    contentDescription = context.getString(R.string.home_settings_cd)
                )
            }
        }
    ) { modifier ->
        ModernScrollableContent(
            modifier = modifier
                .fillMaxSize()
                .padding(bottom = if (currentSong != null) 0.dp else 0.dp),
            featuredContent = featuredContent,
            albums = albums,
            topArtists = topArtists,
            newReleases = currentYearReleases,
            recentlyAddedSongs = recentlyAddedSongs,
            recentlyAddedAlbums = recentlyAddedAlbums,
            recentlyPlayed = recentlyPlayed,
            moodBasedSongs = moodBasedSongs,
            energeticSongs = energeticSongs,
            relaxingSongs = relaxingSongs,
            songs = songs,
            onSongClick = onSongClick,
            onAlbumClick = { album: Album ->
                selectedAlbum = album
                showAlbumBottomSheet = true
            },
            onArtistClick = { artist: Artist ->
                selectedArtist = artist
                showArtistSheet = true
            },
            onViewAllSongs = onViewAllSongs,
            onViewAllAlbums = onViewAllAlbums,
            onViewAllArtists = onViewAllArtists,
            onSearchClick = onSearchClick,
            onSettingsClick = onSettingsClick,
            onAppUpdateClick = onAppUpdateClick,
            onNavigateToLibrary = onNavigateToLibrary,
            onNavigateToPlaylist = onNavigateToPlaylist,
            onNavigateToStats = onNavigateToStats,
            updaterViewModel = updaterViewModel,
            musicViewModel = musicViewModel,
            coroutineScope = coroutineScope
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ModernScrollableContent(
    modifier: Modifier = Modifier,
    featuredContent: List<Album>,
    albums: List<Album>,
    topArtists: List<Artist>,
    newReleases: List<Album>,
    recentlyAddedSongs: List<Song>,
    recentlyAddedAlbums: List<Album>,
    recentlyPlayed: List<Song>,
    moodBasedSongs: List<Song>,
    energeticSongs: List<Song>,
    relaxingSongs: List<Song>,
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onArtistClick: (Artist) -> Unit,
    onViewAllSongs: () -> Unit,
    onViewAllAlbums: () -> Unit,
    onViewAllArtists: () -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
    onAppUpdateClick: (Boolean) -> Unit = { onSettingsClick() },
    onNavigateToLibrary: () -> Unit = {},
    onNavigateToPlaylist: (String) -> Unit = {},
    onNavigateToStats: () -> Unit = {},
    updaterViewModel: AppUpdaterViewModel = viewModel(),
    musicViewModel: chromahub.rhythm.app.viewmodel.MusicViewModel,
    coroutineScope: CoroutineScope
) {
    val context = LocalContext.current
    val windowSizeClass = calculateWindowSizeClass(context as android.app.Activity)
    val widthSizeClass = windowSizeClass.widthSizeClass
    val heightSizeClass = windowSizeClass.heightSizeClass
    val isTablet = widthSizeClass == WindowWidthSizeClass.Medium || 
                   widthSizeClass == WindowWidthSizeClass.Expanded
    val scrollState = rememberScrollState()
    val allSongs by musicViewModel.filteredSongs.collectAsState()
    
    // Home Screen Customization Settings
    val appSettings = AppSettings.getInstance(context)
    val sectionOrder by appSettings.homeSectionOrder.collectAsState()
    val showGreeting by appSettings.homeShowGreeting.collectAsState()
    val showRecentlyPlayed by appSettings.homeShowRecentlyPlayed.collectAsState()
    val showDiscoverCarousel by appSettings.homeShowDiscoverCarousel.collectAsState()
    val showArtists by appSettings.homeShowArtists.collectAsState()
    val showNewReleases by appSettings.homeShowNewReleases.collectAsState()
    val showRecentlyAdded by appSettings.homeShowRecentlyAdded.collectAsState()
    val showRecommended by appSettings.homeShowRecommended.collectAsState()
    val showListeningStats by appSettings.homeShowListeningStats.collectAsState()
    val showMoodSections by appSettings.homeShowMoodSections.collectAsState()
    val discoverAutoScroll by appSettings.homeDiscoverAutoScroll.collectAsState()
    val discoverAutoScrollInterval by appSettings.homeDiscoverAutoScrollInterval.collectAsState()
    val discoverItemCount by appSettings.homeDiscoverItemCount.collectAsState()
    val recentlyPlayedCount by appSettings.homeRecentlyPlayedCount.collectAsState()
    val artistsCount by appSettings.homeArtistsCount.collectAsState()
    val newReleasesCount by appSettings.homeNewReleasesCount.collectAsState()
    val recentlyAddedCount by appSettings.homeRecentlyAddedCount.collectAsState()
    val recommendedCount by appSettings.homeRecommendedCount.collectAsState()
    val carouselHeight by appSettings.homeCarouselHeight.collectAsState()
    val discoverCarouselStyle by appSettings.homeDiscoverCarouselStyle.collectAsState()
    
    // Discover widget card content visibility settings
    val discoverShowAlbumName by appSettings.homeDiscoverShowAlbumName.collectAsState()
    val discoverShowArtistName by appSettings.homeDiscoverShowArtistName.collectAsState()
    val discoverShowYear by appSettings.homeDiscoverShowYear.collectAsState()
    val discoverShowPlayButton by appSettings.homeDiscoverShowPlayButton.collectAsState()
    val discoverShowGradient by appSettings.homeDiscoverShowGradient.collectAsState()
    
    // Enhanced artist computation
    val availableArtists = remember(allSongs, topArtists) {
        val collaborationSeparators = listOf(
            ", ", ",", " & ", " and ", "&", " feat. ", " featuring ", " ft. ", 
            " with ", " x ", " X ", " + ", " vs ", " VS ", " / ", ";", " · "
        )

        val collaborationRegex = collaborationSeparators
            .map { Regex.escape(it) }
            .joinToString("|")
            .toRegex(RegexOption.IGNORE_CASE)

        val filteredTopArtists = topArtists.filter { artist ->
            !artist.name.contains(collaborationRegex)
        }

        val extractedArtistNames = allSongs.asSequence()
            .flatMap { song -> 
                var artistString = song.artist
                collaborationSeparators.forEach { separator ->
                    artistString = artistString.replace(separator, "||")
                }
                
                artistString.split("||")
                    .map { it.trim() }
                    .map { name ->
                        if (name.contains("(") && (name.contains("feat") || name.contains("ft") || name.contains("featuring"))) {
                            name.substringBefore("(").trim()
                        } else {
                            name
                        }
                    }
            }
            .filter { it.length > 1 }
            .distinct()
        
        extractedArtistNames
            .mapNotNull { artistName ->
                filteredTopArtists.find { it.name.equals(artistName, ignoreCase = true) }
                    ?: filteredTopArtists.find { 
                        it.name.equals(artistName, ignoreCase = true) || 
                        (artistName.length > 3 && it.name.contains(artistName, ignoreCase = true))
                    }
            }
            .distinct()
            .sortedBy { it.name }
            .toList()
    }
    
    // Featured albums with auto-refresh - ensure proper count handling
    var currentFeaturedAlbums by remember(featuredContent, discoverItemCount) { 
        mutableStateOf(
            if (featuredContent.isEmpty()) listOf() 
            else featuredContent.take(discoverItemCount)
        )
    }
    
    // Calculate safe item count for carousel
    val carouselItemCount = maxOf(1, minOf(currentFeaturedAlbums.size, discoverItemCount))
    
    // Safe carousel state initialization with key to force recreation on count changes
    val featuredCarouselState = key(carouselItemCount) {
        rememberCarouselState(
            initialItem = 0,
            itemCount = { carouselItemCount }
        )
    }
    
    // Reset carousel when item count changes to prevent state restoration crash
    LaunchedEffect(currentFeaturedAlbums.size, discoverItemCount) {
        val newItemCount = maxOf(1, minOf(currentFeaturedAlbums.size, discoverItemCount))
        // Only scroll if current position is invalid
        if (featuredCarouselState.currentItem >= newItemCount) {
            try {
                featuredCarouselState.scrollToItem(0)
            } catch (e: Exception) {
                android.util.Log.w("HomeScreen", "Carousel reset error: ${e.message}")
            }
        }
    }
    
    // Auto-refresh featured content periodically - respect discoverItemCount setting
    LaunchedEffect(albums, discoverItemCount) {
        while (true) {
            delay(45000) // 45 seconds for better user experience
            if (albums.size > discoverItemCount) {
                // Shuffle and take only the configured count
                currentFeaturedAlbums = albums.shuffled().take(discoverItemCount)
            } else if (albums.isNotEmpty()) {
                // If we have fewer albums than the count, use all available
                currentFeaturedAlbums = albums.shuffled()
            }
        }
    }
    
    // Enhanced mood content
    val enhancedMoodContent = remember(allSongs) {
        val focusSongs = allSongs
            .filter { it.duration > 3 * 60 * 1000 }
            .take(15) // Increased count
            .ifEmpty { moodBasedSongs }
        
        val energeticKeywords = listOf("rock", "dance", "pop", "party", "beat", "energy", "fast", "upbeat", "electronic")
        val betterEnergeticSongs = allSongs
            .filter { song ->
                energeticKeywords.any { keyword ->
                    song.title.contains(keyword, ignoreCase = true) || 
                    song.artist.contains(keyword, ignoreCase = true)
                }
            }
            .take(12)
            .ifEmpty { energeticSongs }
        
        val relaxingKeywords = listOf("chill", "relax", "ambient", "piano", "sleep", "calm", "soft", "peaceful", "acoustic")
        val betterRelaxingSongs = allSongs
            .filter { song ->
                relaxingKeywords.any { keyword ->
                    song.title.contains(keyword, ignoreCase = true) || 
                    song.artist.contains(keyword, ignoreCase = true)
                }
            }
            .take(12)
            .ifEmpty { relaxingSongs }
        
        Triple(focusSongs, betterEnergeticSongs, betterRelaxingSongs)
    }
    
    // Get festive theme settings (using appSettings already declared above)
    val festiveEnabled by appSettings.festiveThemeEnabled.collectAsState()
    val festiveTypeString by appSettings.festiveThemeType.collectAsState()
    val festiveAutoDetect by appSettings.festiveThemeAutoDetect.collectAsState()
    
    // Determine active festive theme
    val activeFestiveTheme = remember(festiveEnabled, festiveTypeString, festiveAutoDetect) {
        if (festiveEnabled) {
            val festiveConfig = FestiveConfig(
                enabled = festiveEnabled,
                type = try {
                    FestiveThemeType.valueOf(festiveTypeString)
                } catch (e: IllegalArgumentException) {
                    FestiveThemeType.NONE
                },
                autoDetect = festiveAutoDetect
            )
            FestiveThemeEngine.getActiveFestiveTheme(festiveConfig)
        } else {
            FestiveThemeType.NONE
        }
    }
    
    // Time-based greeting with festive override
    val greeting = remember(activeFestiveTheme) {
        when (activeFestiveTheme) {
            FestiveThemeType.CHRISTMAS -> context.getString(R.string.home_greeting_christmas)
            FestiveThemeType.NEW_YEAR -> context.getString(R.string.home_greeting_new_year)
            FestiveThemeType.HALLOWEEN -> context.getString(R.string.home_greeting_halloween)
            FestiveThemeType.VALENTINES -> context.getString(R.string.home_greeting_valentines)
            else -> {
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                when {
                    hour in 0..4 -> context.getString(R.string.home_greeting_night)      // Late night: 12 AM - 4:59 AM
                    hour in 5..11 -> context.getString(R.string.home_greeting_morning)   // Morning: 5 AM - 11:59 AM
                    hour in 12..16 -> context.getString(R.string.home_greeting_afternoon) // Afternoon: 12 PM - 4:59 PM
                    hour in 17..20 -> context.getString(R.string.home_greeting_evening)   // Evening: 5 PM - 8:59 PM
                    else -> context.getString(R.string.home_greeting_night)              // Night: 9 PM - 11:59 PM
                }
            }
        }
    }
    
    // Update information
    val updateAvailable by updaterViewModel.updateAvailable.collectAsState()
    val latestVersion by updaterViewModel.latestVersion.collectAsState()
    val error by updaterViewModel.error.collectAsState()
    val updatesEnabled by updaterViewModel.appSettings.updatesEnabled.collectAsState(initial = false)
    
    // Enhanced auto-scroll for featured carousel with smooth animations
    LaunchedEffect(carouselItemCount, discoverAutoScroll, discoverAutoScrollInterval) {
        if (discoverAutoScroll && carouselItemCount > 1) {
            while (true) {
                delay(discoverAutoScrollInterval * 1000L) // Use settings interval
                try {
                    // Calculate next item safely within the carousel bounds
                    val currentItem = featuredCarouselState.currentItem
                    val nextItem = (currentItem + 1) % carouselItemCount
                    
                    // Only scroll if we have valid items and within bounds
                    if (nextItem < carouselItemCount && currentItem < carouselItemCount) {
                        featuredCarouselState.animateScrollToItem(nextItem)
                    }
                } catch (e: Exception) {
                    // Handle any scroll exceptions gracefully
                    android.util.Log.w("HomeScreen", "Carousel autoscroll error: ${e.message}")
                }
            }
        }
    }
    
    // Remember lazy list state for performance optimization (Compose 1.10)
    val lazyListState = rememberLazyListState()
    
    // Visibility tracking for performance optimization (Compose 1.8+)
    // Track first visible item for analytics and prefetching optimization
    val firstVisibleItemIndex by remember { derivedStateOf { lazyListState.firstVisibleItemIndex } }
    val isScrollInProgress by remember { derivedStateOf { lazyListState.isScrollInProgress } }
    
    // Prefetch optimization: when user stops scrolling near end, prepare next items
    LaunchedEffect(firstVisibleItemIndex, isScrollInProgress) {
        if (!isScrollInProgress && firstVisibleItemIndex > 0) {
            // User has scrolled and stopped - good time for analytics or prefetching
            // This can be extended to log visible sections for personalization
        }
    }
    
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = when (widthSizeClass) {
                    WindowWidthSizeClass.Compact -> 20.dp
                    WindowWidthSizeClass.Medium -> 48.dp  // Increased for tablets
                    WindowWidthSizeClass.Expanded -> 64.dp  // More padding for large tablets
                    else -> 20.dp
                }),
            verticalArrangement = Arrangement.spacedBy(when (widthSizeClass) {
                WindowWidthSizeClass.Compact -> when (heightSizeClass) {
                    WindowHeightSizeClass.Compact -> 32.dp // Landscape phone - tighter spacing
                    else -> 40.dp // Portrait phone
                }
                WindowWidthSizeClass.Medium -> when (heightSizeClass) {
                    WindowHeightSizeClass.Compact -> 48.dp // Landscape tablet - more spacing
                    else -> 56.dp // Portrait tablet - more spacing
                }
                WindowWidthSizeClass.Expanded -> when (heightSizeClass) {
                    WindowHeightSizeClass.Compact -> 52.dp // Landscape large tablet
                    else -> 64.dp // Portrait large tablet - generous spacing
                }
                else -> 40.dp
            }),
            contentPadding = PaddingValues(bottom = 24.dp) // No top padding to connect with topbar
        ) {
            // Update section (preserved as requested)
            item(key = "section_update", contentType = "update") {
                AnimatedVisibility(
                    visible = updateAvailable && latestVersion != null && error == null && updatesEnabled,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    latestVersion?.let { version ->
                        ModernUpdateSection(
                            latestVersion = version,
                            onUpdateClick = onAppUpdateClick
                        )
                    }
                }
            }

            // Render sections dynamically based on sectionOrder
            sectionOrder.forEach { sectionId ->
                when (sectionId) {
                    "GREETING" -> {
                        if (showGreeting) {
                            item(key = "section_greeting") {
                                if (!updateAvailable || latestVersion == null || error != null || !updatesEnabled) {
                                    ModernWelcomeSection(
                                        greeting = greeting,
                                        festiveTheme = activeFestiveTheme,
                                        onSearchClick = onSearchClick
                                    )
                                }
                            }
                        }
                    }
                    "RECENTLY_PLAYED" -> {
                        if (showRecentlyPlayed) {
                            item(key = "section_recently_played") {
                                ModernRecentlyPlayedSection(
                                    recentlyPlayed = recentlyPlayed.take(recentlyPlayedCount),
                                    onSongClick = onSongClick,
                                    musicViewModel = musicViewModel,
                                    coroutineScope = coroutineScope,
                                    widthSizeClass = widthSizeClass,
                                    heightSizeClass = heightSizeClass
                                )
                            }
                        }
                    }
                    "DISCOVER" -> {
                        if (showDiscoverCarousel) {
                            item(key = "section_discover") {
                                Column {
                                    ModernSectionTitle(
                                        title = context.getString(R.string.home_discover_albums),
                                        subtitle = context.getString(R.string.home_explore_music),
                                        viewAllAction = onViewAllAlbums
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                    if (currentFeaturedAlbums.isNotEmpty()) {
                                        // Use larger carousel for tablets
                                        val tabletCarouselHeight = when (widthSizeClass) {
                                            WindowWidthSizeClass.Medium -> carouselHeight + 60  // +60dp for medium tablets
                                            WindowWidthSizeClass.Expanded -> carouselHeight + 100  // +100dp for large tablets
                                            else -> carouselHeight
                                        }
                                        ModernFeaturedSection(
                                            albums = currentFeaturedAlbums,
                                            carouselState = featuredCarouselState,
                                            onAlbumClick = onAlbumClick,
                                            showAlbumName = discoverShowAlbumName,
                                            showArtistName = discoverShowArtistName,
                                            showYear = discoverShowYear,
                                            showPlayButton = discoverShowPlayButton,
                                            showGradient = discoverShowGradient,
                                            carouselHeight = tabletCarouselHeight,
                                            carouselStyle = discoverCarouselStyle,
                                            widthSizeClass = widthSizeClass,
                                            heightSizeClass = heightSizeClass
                                        )
                                    } else {
                                        ModernEmptyState(
                                            icon = Icons.Rounded.Album,
                                            title = context.getString(R.string.home_no_featured_albums),
                                            subtitle = context.getString(R.string.home_no_featured_albums_desc),
                                            iconSize = 48.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                    "ARTISTS" -> {
                        if (showArtists) {
                            item(key = "section_artists") {
                                if (availableArtists.isNotEmpty()) {
                                    ModernArtistsSection(
                                        artists = availableArtists.take(artistsCount),
                                        songs = allSongs,
                                        onArtistClick = onArtistClick,
                                        onViewAllArtists = onViewAllArtists,
                                        widthSizeClass = widthSizeClass,
                                        heightSizeClass = heightSizeClass
                                    )
                                } else {
                                    Column {
                                        ModernSectionTitle(
                                            title = context.getString(R.string.home_artists),
                                            subtitle = context.getString(R.string.home_explore_musicians),
                                            viewAllAction = onViewAllArtists
                                        )
                                        Spacer(modifier = Modifier.height(20.dp))
                                        ModernEmptyState(
                                            icon = Icons.Rounded.Person,
                                            title = context.getString(R.string.home_no_artists),
                                            subtitle = context.getString(R.string.home_no_artists_desc),
                                            iconSize = 48.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                    "NEW_RELEASES" -> {
                        if (showNewReleases) {
                            item(key = "section_new_releases") {
                                Column {
                                    ModernSectionTitle(
                                        title = context.getString(R.string.home_new_releases),
                                        subtitle = context.getString(R.string.home_fresh_music),
                                        onPlayAll = {
                                            coroutineScope.launch {
                                                val allNewReleaseSongs = newReleases.flatMap { album ->
                                                    musicViewModel.getMusicRepository().getSongsForAlbumLocal(album.id)
                                                }
                                                if (allNewReleaseSongs.isNotEmpty()) {
                                                    musicViewModel.playQueue(allNewReleaseSongs)
                                                }
                                            }
                                        },
                                        onShufflePlay = {
                                            coroutineScope.launch {
                                                val allNewReleaseSongs = newReleases.flatMap { album ->
                                                    musicViewModel.getMusicRepository().getSongsForAlbumLocal(album.id)
                                                }
                                                if (allNewReleaseSongs.isNotEmpty()) {
                                                    musicViewModel.playShuffled(allNewReleaseSongs)
                                                }
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                    if (newReleases.isNotEmpty()) {
                                        val isTablet = widthSizeClass != WindowWidthSizeClass.Compact
                                        if (isTablet) {
                                            // Enhanced grid layout for tablets with better column distribution
                                            val gridColumns = when (widthSizeClass) {
                                                WindowWidthSizeClass.Medium -> 3  // 3 columns for medium tablets
                                                WindowWidthSizeClass.Expanded -> 4  // 4 columns for large tablets
                                                else -> 2
                                            }
                                            val gridState = rememberLazyGridState()
                                            // Calculate dynamic height based on content and columns
                                            val estimatedRows = (newReleases.take(newReleasesCount).size + gridColumns - 1) / gridColumns
                                            val cardHeight = when (widthSizeClass) {
                                                WindowWidthSizeClass.Medium -> 300.dp
                                                WindowWidthSizeClass.Expanded -> 330.dp
                                                else -> 240.dp
                                            }
                                            val gridHeight = (cardHeight.value * minOf(estimatedRows, 2) + 20f * (minOf(estimatedRows, 2) - 1)).dp

                                            LazyVerticalGrid(
                                                columns = GridCells.Fixed(gridColumns),
                                                state = gridState,
                                                horizontalArrangement = Arrangement.spacedBy(24.dp), // More spacing for tablets
                                                verticalArrangement = Arrangement.spacedBy(24.dp),   // More spacing for tablets
                                                modifier = Modifier.height(gridHeight),
                                                contentPadding = PaddingValues(horizontal = 8.dp)
                                            ) {
                                                items(
                                                    items = newReleases.take(newReleasesCount),
                                                    key = { "newrelease_${it.id}" },
                                                    contentType = { "album" }
                                                ) { album ->
                                                    ModernAlbumCard(
                                                        album = album,
                                                        onClick = { onAlbumClick(album) },
                                                        widthSizeClass = widthSizeClass,
                                                        heightSizeClass = heightSizeClass
                                                    )
                                                }
                                            }
                                        } else {
                                            // Horizontal scroll for phones
                                            val newReleasesListState = rememberLazyListState()
                                            LazyRow(
                                                state = newReleasesListState,
                                                contentPadding = PaddingValues(horizontal = 8.dp),
                                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                                            ) {
                                                items(
                                                    items = newReleases.take(newReleasesCount),
                                                    key = { "newrelease_${it.id}" },
                                                    contentType = { "album" }
                                                ) { album ->
                                                    ModernAlbumCard(
                                                        album = album,
                                                        onClick = { onAlbumClick(album) },
                                                        widthSizeClass = widthSizeClass,
                                                        heightSizeClass = heightSizeClass
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        ModernEmptyState(
                                            icon = Icons.Rounded.NewReleases,
                                            title = context.getString(R.string.home_no_new_releases),
                                            subtitle = context.getString(R.string.home_no_new_releases_desc),
                                            iconSize = 48.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                    "RECENTLY_ADDED" -> {
                        if (showRecentlyAdded) {
                            item(key = "section_recently_added") {
                                Column {
                                    ModernSectionTitle(
                                        title = context.getString(R.string.home_recently_added),
                                        subtitle = context.getString(R.string.home_latest_additions),
                                        onPlayAll = {
                                            if (recentlyAddedAlbums.isNotEmpty()) {
                                                coroutineScope.launch(Dispatchers.Default) {
                                                    val allSongs = recentlyAddedAlbums.flatMap { album ->
                                                        album.songs
                                                    }
                                                    withContext(Dispatchers.Main) {
                                                        musicViewModel.playQueue(allSongs)
                                                    }
                                                }
                                            }
                                        },
                                        onShufflePlay = {
                                            if (recentlyAddedAlbums.isNotEmpty()) {
                                                coroutineScope.launch(Dispatchers.Default) {
                                                    val allSongs = recentlyAddedAlbums.flatMap { album ->
                                                        album.songs
                                                    }
                                                    withContext(Dispatchers.Main) {
                                                        musicViewModel.playShuffled(allSongs)
                                                    }
                                                }
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                    if (recentlyAddedAlbums.isNotEmpty()) {
                                        val isTablet = widthSizeClass != WindowWidthSizeClass.Compact
                                        if (isTablet) {
                                            // Enhanced grid layout for tablets with better column distribution
                                            val gridColumns = when (widthSizeClass) {
                                                WindowWidthSizeClass.Medium -> 3  // 3 columns for medium tablets
                                                WindowWidthSizeClass.Expanded -> 4  // 4 columns for large tablets
                                                else -> 2
                                            }
                                            val gridState = rememberLazyGridState()
                                            // Calculate dynamic height based on content and columns
                                            val estimatedRows = (recentlyAddedAlbums.take(recentlyAddedCount).size + gridColumns - 1) / gridColumns
                                            val cardHeight = when (widthSizeClass) {
                                                WindowWidthSizeClass.Medium -> 300.dp
                                                WindowWidthSizeClass.Expanded -> 330.dp
                                                else -> 240.dp
                                            }
                                            val gridHeight = (cardHeight.value * minOf(estimatedRows, 2) + 24f * (minOf(estimatedRows, 2) - 1)).dp

                                            LazyVerticalGrid(
                                                columns = GridCells.Fixed(gridColumns),
                                                state = gridState,
                                                horizontalArrangement = Arrangement.spacedBy(24.dp), // More spacing for tablets
                                                verticalArrangement = Arrangement.spacedBy(24.dp),   // More spacing for tablets
                                                modifier = Modifier.height(gridHeight),
                                                contentPadding = PaddingValues(horizontal = 8.dp)
                                            ) {
                                                items(
                                                    items = recentlyAddedAlbums.take(recentlyAddedCount),
                                                    key = { "recentalbum_${it.id}" },
                                                    contentType = { "album" }
                                                ) { album ->
                                                    ModernAlbumCard(
                                                        album = album,
                                                        onClick = { onAlbumClick(album) },
                                                        widthSizeClass = widthSizeClass,
                                                        heightSizeClass = heightSizeClass
                                                    )
                                                }
                                            }
                                        } else {
                                            // Horizontal scroll for phones
                                            val recentlyAddedListState = rememberLazyListState()
                                            LazyRow(
                                                state = recentlyAddedListState,
                                                contentPadding = PaddingValues(horizontal = 8.dp),
                                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                                            ) {
                                                items(
                                                    items = recentlyAddedAlbums.take(recentlyAddedCount),
                                                    key = { "recentalbum_${it.id}" },
                                                    contentType = { "album" }
                                                ) { album ->
                                                    ModernAlbumCard(
                                                        album = album,
                                                        onClick = { onAlbumClick(album) },
                                                        widthSizeClass = widthSizeClass,
                                                        heightSizeClass = heightSizeClass
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        ModernEmptyState(
                                            icon = Icons.Rounded.LibraryAdd,
                                            title = context.getString(R.string.home_no_recently_added),
                                            subtitle = context.getString(R.string.home_no_recently_added_desc),
                                            iconSize = 48.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                    "RECOMMENDED" -> {
                        if (showRecommended) {
                            item(key = "section_recommended") {
                                val recommendedSongs = remember(recentlyPlayed, songs, recommendedCount) {
                                    // Generate recommendations based on recently played songs
                                    if (recentlyPlayed.isNotEmpty()) {
                                        val playedArtists = recentlyPlayed.map { it.artist }.distinct()
                                        val playedAlbums = recentlyPlayed.map { it.album }.distinct()
                                        
                                        // Find songs from similar artists or albums
                                        songs.filter { song ->
                                            (song.artist in playedArtists || song.album in playedAlbums) &&
                                            !recentlyPlayed.contains(song)
                                        }.shuffled().take(recommendedCount)
                                    } else {
                                        // Fallback to random popular songs if no history
                                        songs.shuffled().take(recommendedCount)
                                    }
                                }
                                
                                ModernRecommendedSection(
                                    recommendedSongs = recommendedSongs,
                                    onSongClick = onSongClick
                                )
                            }
                        }
                    }
                    "STATS" -> {
                        if (showListeningStats) {
                            item(key = "section_stats") {
                                ModernListeningStatsSection(onClick = onNavigateToStats)
                            }
                        }
                    }
                    "MOOD" -> {
                        if (showMoodSections) {
                            item(key = "section_mood") {
                                ModernMoodSection(
                                    moodBasedSongs = enhancedMoodContent.first,
                                    energeticSongs = enhancedMoodContent.second,
                                    relaxingSongs = enhancedMoodContent.third,
                                    onSongClick = onSongClick
                                )
                            }
                        }
                    }
                }
            }

            // Add some bottom padding for mini player
            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

// Modern Component Functions - Part 2

@Composable
private fun ModernWelcomeSection(
    greeting: String,
    festiveTheme: FestiveThemeType = FestiveThemeType.NONE,
    onSearchClick: () -> Unit
) {
    val context = LocalContext.current
    val viewModel = viewModel<chromahub.rhythm.app.viewmodel.MusicViewModel>()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
    val haptic = LocalHapticFeedback.current
    
    // Responsive font sizes
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isCompactWidth = screenWidthDp < 400
    val isTablet = screenWidthDp >= 600
    
    val greetingFontSize = when {
        isCompactWidth -> 28.sp
        isTablet -> 36.sp
        else -> 32.sp
    }
    val messageFontSize = when {
        isCompactWidth -> 12.sp
        isTablet -> 16.sp
        else -> 14.sp
    }
    val quoteFontSize = when {
        isCompactWidth -> 11.sp
        isTablet -> 14.sp
        else -> 12.sp
    }
    
    // Enhanced time-based quotes with proper time ranges
    val timeBasedQuote = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour in 0..4 -> listOf(
                context.getString(R.string.home_quote_late_night_1),
                context.getString(R.string.home_quote_late_night_2),
                context.getString(R.string.home_quote_late_night_3),
                context.getString(R.string.home_quote_late_night_4)
            )
            hour in 5..11 -> listOf(
                context.getString(R.string.home_quote_morning_1),
                context.getString(R.string.home_quote_morning_2),
                context.getString(R.string.home_quote_morning_3),
                context.getString(R.string.home_quote_morning_4)
            )
            hour in 12..16 -> listOf(
                context.getString(R.string.home_quote_afternoon_1),
                context.getString(R.string.home_quote_afternoon_2),
                context.getString(R.string.home_quote_afternoon_3),
                context.getString(R.string.home_quote_afternoon_4)
            )
            hour in 17..20 -> listOf(
                context.getString(R.string.home_quote_evening_1),
                context.getString(R.string.home_quote_evening_2),
                context.getString(R.string.home_quote_evening_3),
                context.getString(R.string.home_quote_evening_4)
            )
            else -> listOf(
                context.getString(R.string.home_quote_night_1),
                context.getString(R.string.home_quote_night_2),
                context.getString(R.string.home_quote_night_3),
                context.getString(R.string.home_quote_night_4)
            )
        }.random()
    }
    
    val personalizedMessage = remember(recentlyPlayed) {
        if (recentlyPlayed.isNotEmpty()) {
            val recentSong = recentlyPlayed.firstOrNull()?.title
            if (!recentSong.isNullOrBlank()) {
                context.getString(R.string.home_continue_song, recentSong)
            } else {
                context.getString(R.string.home_adventure_continues)
            }
        } else {
            context.getString(R.string.home_discover_next_favorite)
        }
    }
    
    val timeBasedTheme = remember(festiveTheme) {
        when (festiveTheme) {
            FestiveThemeType.CHRISTMAS -> Triple("🎄", "christmas", "🎅")
            FestiveThemeType.NEW_YEAR -> Triple("🎉", "new_year", "🥳")
            FestiveThemeType.HALLOWEEN -> Triple("🎃", "halloween", "👻")
            FestiveThemeType.VALENTINES -> Triple("💝", "valentines", "💕")
            else -> {
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                when {
                    hour in 0..4 -> Triple("🌙", "late_night", "⭐")  // Late night
                    hour in 5..11 -> Triple("☀️", "morning", "🌻")      // Morning
                    hour in 12..16 -> Triple("🌤️", "afternoon", "⚡") // Afternoon
                    hour in 17..20 -> Triple("🌅", "evening", "✨")    // Evening
                    else -> Triple("🌙", "night", "🌟")                // Night
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                onSearchClick() 
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(28.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Decorative elements in background
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(18.dp), // Reduced padding
                horizontalArrangement = Arrangement.spacedBy(8.dp) // Reduced spacing
            ) {
                repeat(3) { // Reduced from 3 to 2 decorative elements
                    Text(
                        text = timeBasedTheme.third,
                        style = MaterialTheme.typography.titleLarge, // Reduced from headlineSmall
                        modifier = Modifier.alpha(0.12f) // Slightly more transparent
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp) // Reduced padding for more compact design
            ) {
                // Main greeting
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 0.dp) // Reduced spacing
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "emoji_pulse")
                    val emojiScale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2000),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "emoji_scale"
                    )
                    
                    Text(
                        text = timeBasedTheme.first,
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .graphicsLayer {
                                scaleX = emojiScale
                                scaleY = emojiScale
                            }
                    )
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = greeting,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Text(
                            text = timeBasedQuote,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 5.dp) // Reduced spacing
                        )
                    }

                    // Modern search button with expressive design
                    FilledIconButton(
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                            onSearchClick()
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.size(46.dp) // Larger, more prominent
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = context.getString(R.string.cd_search),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
//
//                // Quote section with Material 3 Expressive design
//                Surface(
//                    color = MaterialTheme.colorScheme.primary,
//                    shape = RoundedCornerShape(24.dp), // More rounded
//                    modifier = Modifier.fillMaxWidth()
//                ) {
//                    Row(
//                        modifier = Modifier.padding(16.dp), // Reduced padding
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Surface(
//                            shape = CircleShape,
//                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
//                            modifier = Modifier.size(32.dp) // Reduced size
//                        ) {
//                            Box(
//                                contentAlignment = Alignment.Center,
//                                modifier = Modifier.fillMaxSize()
//                            ) {
//                                Text(
//                                    text = "💭",
//                                    style = MaterialTheme.typography.bodyLarge // Reduced size
//                                )
//                            }
//                        }
//
//                        Spacer(modifier = Modifier.width(12.dp)) // Reduced spacing
//
//                        Text(
//                            text = timeBasedQuote,
//                            style = MaterialTheme.typography.bodyMedium, // Larger text for readability
//                            fontWeight = FontWeight.Medium,
//                            color = MaterialTheme.colorScheme.inverseOnSurface,
//                            lineHeight = 22.sp, // Better line height
//                            modifier = Modifier.weight(1f)
//                        )
//                    }
//                }
            }
        }
    }
}



@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ModernRecentlyPlayedSection(
    recentlyPlayed: List<Song>,
    onSongClick: (Song) -> Unit,
    musicViewModel: chromahub.rhythm.app.viewmodel.MusicViewModel,
    coroutineScope: CoroutineScope,
    widthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
    heightSizeClass: WindowHeightSizeClass = WindowHeightSizeClass.Medium
) {
    val context = LocalContext.current
    Column {
        ModernSectionTitle(
            title = context.getString(R.string.home_recently_played),
            subtitle = context.getString(R.string.home_recently_played_subtitle),
            onPlayAll = {
                coroutineScope.launch {
                    if (recentlyPlayed.isNotEmpty()) {
                        musicViewModel.playQueue(recentlyPlayed)
                    }
                }
            },
            onShufflePlay = {
                coroutineScope.launch {
                    if (recentlyPlayed.isNotEmpty()) {
                        musicViewModel.playShuffled(recentlyPlayed)
                    }
                }
            }
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        if (recentlyPlayed.isNotEmpty()) {
            val isTablet = widthSizeClass != WindowWidthSizeClass.Compact
            if (isTablet) {
                // Grid layout for tablets - better use of space
                val gridColumns = when (widthSizeClass) {
                    WindowWidthSizeClass.Medium -> 2  // 2 columns for medium tablets
                    WindowWidthSizeClass.Expanded -> 3  // 3 columns for large tablets
                    else -> 2
                }
                val gridState = rememberLazyGridState()
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumns),
                    state = gridState,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.height(300.dp)  // Fixed height for grid
                ) {
                    items(
                        items = recentlyPlayed,
                        key = { "recentplay_${it.id}" },
                        contentType = { "song" }
                    ) { song ->
                        ModernRecentSongCard(
                            song = song,
                            onClick = { onSongClick(song) },
                            widthSizeClass = widthSizeClass,
                            heightSizeClass = heightSizeClass
                        )
                    }
                }
            } else {
                // Horizontal scroll for phones
                val recentlyPlayedListState = rememberLazyListState()
                LazyRow(
                    state = recentlyPlayedListState,
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(
                        items = recentlyPlayed,
                        key = { "recentplay_${it.id}" },
                        contentType = { "song" }
                    ) { song ->
                        ModernRecentSongCard(
                            song = song,
                            onClick = { onSongClick(song) },
                            widthSizeClass = widthSizeClass,
                            heightSizeClass = heightSizeClass
                        )
                    }
                }
            }
        } else {
            // Empty state for recently played
            ModernEmptyState(
                icon = Icons.Rounded.History,
                title = context.getString(R.string.home_no_recent_activity),
                subtitle = context.getString(R.string.home_no_recent_activity_desc),
                iconSize = 48.dp
            )
        }
    }
}

@Composable
private fun ModernRecentSongCard(
    song: Song,
    onClick: () -> Unit,
    widthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
    heightSizeClass: WindowHeightSizeClass = WindowHeightSizeClass.Medium
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val (cardWidth, cardHeight) = when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> when (heightSizeClass) {
            WindowHeightSizeClass.Compact -> 160.dp to 70.dp // Landscape phone - smaller
            else -> 180.dp to 80.dp // Portrait phone
        }
        WindowWidthSizeClass.Medium -> when (heightSizeClass) {
            WindowHeightSizeClass.Compact -> Dp.Unspecified to 90.dp // Full width for grid
            else -> Dp.Unspecified to 95.dp // Full width for grid
        }
        WindowWidthSizeClass.Expanded -> when (heightSizeClass) {
            WindowHeightSizeClass.Compact -> Dp.Unspecified to 100.dp // Full width for grid
            else -> Dp.Unspecified to 100.dp // Full width for grid
        }
        else -> 180.dp to 80.dp
    }
    
    val cardModifier = if (cardWidth == Dp.Unspecified) {
        // Full width for grid layout on tablets
        Modifier
            .fillMaxWidth()
            .height(cardHeight)
    } else {
        // Fixed width for horizontal scroll on phones
        Modifier
            .width(cardWidth)
            .height(cardHeight)
    }

    Card(
        onClick = {
            HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
            onClick()
        },
        modifier = cardModifier
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp), // More padding
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Enhanced artwork with more rounded corners
            Surface(
                shape = RoundedCornerShape(16.dp), // More rounded
                modifier = Modifier.size(52.dp), // Slightly larger
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .apply(ImageUtils.buildImageRequest(
                            song.artworkUri,
                            song.title,
                            context.cacheDir,
                            M3PlaceholderType.TRACK
                        ))
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Play indicator
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = context.getString(R.string.cd_play),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun ModernSectionTitle(
    title: String,
    subtitle: String? = null,
    viewAllAction: (() -> Unit)? = null,
    onPlayAll: (() -> Unit)? = null,
    onShufflePlay: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        
        // Compact Material 3 Split Button Group
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play/Shuffle Split Button (Primary Action)
            if (onPlayAll != null || onShufflePlay != null) {
                var isPlayPressed by remember { mutableStateOf(false) }
                var isShufflePressed by remember { mutableStateOf(false) }
                
                val playScale by animateFloatAsState(
                    targetValue = if (isPlayPressed) 0.94f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "playScale"
                )
                val shuffleScale by animateFloatAsState(
                    targetValue = if (isShufflePressed) 0.94f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "shuffleScale"
                )
                
                // Split button container
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primary,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(0.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Play button (left side)
                        onPlayAll?.let {
                            Surface(
                                onClick = {
                                    HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.LongPress)
                                    it()
                                },
                                shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
                                color = Color.Transparent,
                                modifier = Modifier
                                    .scale(playScale)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                isPlayPressed = true
                                                tryAwaitRelease()
                                                isPlayPressed = false
                                            }
                                        )
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 14.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = context.getString(R.string.cd_play),
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = context.getString(R.string.action_play),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                        
                        // Divider
                        if (onPlayAll != null && onShufflePlay != null) {
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(24.dp)
                                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
                            )
                        }
                        
                        // Shuffle button (right side)
                        onShufflePlay?.let {
                            Surface(
                                onClick = {
                                    HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.LongPress)
                                    it()
                                },
                                shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                                color = Color.Transparent,
                                modifier = Modifier
                                    .scale(shuffleScale)
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                isShufflePressed = true
                                                tryAwaitRelease()
                                                isShufflePressed = false
                                            }
                                        )
                                    }
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Shuffle,
                                        contentDescription = context.getString(R.string.cd_shuffle),
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // View All Button (Secondary Action) - Prominent size
            viewAllAction?.let {
                var isPressed by remember { mutableStateOf(false) }
                val scale by animateFloatAsState(
                    targetValue = if (isPressed) 0.94f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "viewAllScale"
                )
                
                OutlinedButton(
                    onClick = {
                        HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                        it()
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    modifier = Modifier
                        .scale(scale)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    isPressed = true
                                    tryAwaitRelease()
                                    isPressed = false
                                }
                            )
                        }
                ) {
                    Text(
                        text = context.getString(R.string.ui_view_all),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernFeaturedSection(
    albums: List<Album>,
    carouselState: androidx.compose.material3.carousel.CarouselState,
    onAlbumClick: (Album) -> Unit,
    showAlbumName: Boolean = true,
    showArtistName: Boolean = true,
    showYear: Boolean = true,
    showPlayButton: Boolean = true,
    showGradient: Boolean = true,
    carouselHeight: Int = 260,
    carouselStyle: Int = 0, // 0=Hero, 1=MultiBrowse, 2=Uncontained
    widthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
    heightSizeClass: WindowHeightSizeClass = WindowHeightSizeClass.Medium
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    
    Column(modifier = Modifier.fillMaxWidth()) {
        when (carouselStyle) {
            0 -> {
                // Default - Centered Hero Carousel with 2 side peeks
                HorizontalCenteredHeroCarousel(
                    state = carouselState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(when (widthSizeClass) {
                            WindowWidthSizeClass.Compact -> carouselHeight.dp
                            WindowWidthSizeClass.Medium -> (carouselHeight + 40).dp
                            WindowWidthSizeClass.Expanded -> when (heightSizeClass) {
                                WindowHeightSizeClass.Compact -> (carouselHeight + 20).dp // Landscape tablet
                                else -> (carouselHeight + 40).dp // Portrait tablet
                            }
                            else -> carouselHeight.dp
                        })
                        .padding(vertical = 8.dp),
                    itemSpacing = 16.dp,
                    contentPadding = PaddingValues(horizontal = 32.dp),
                    flingBehavior = CarouselDefaults.singleAdvanceFlingBehavior(state = carouselState)
                ) { itemIndex ->
                    val album = albums[itemIndex]
                    HeroCarouselCard(
                        album = album,
                        onClick = { onAlbumClick(album) },
                        modifier = Modifier.maskClip(MaterialTheme.shapes.extraLarge),
                        showAlbumName = showAlbumName,
                        showArtistName = showArtistName,
                        showYear = showYear,
                        showPlayButton = showPlayButton,
                        showGradient = showGradient,
                        itemIndex = itemIndex,
                        currentItem = carouselState.currentItem
                    )
                }
            }
            else -> {
                // Hero - Uncontained Carousel with 1 peek (larger centered item)
                HorizontalUncontainedCarousel(
                    state = carouselState,
                    itemWidth = 300.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(when (widthSizeClass) {
                            WindowWidthSizeClass.Compact -> carouselHeight.dp
                            WindowWidthSizeClass.Medium -> (carouselHeight + 40).dp
                            WindowWidthSizeClass.Expanded -> when (heightSizeClass) {
                                WindowHeightSizeClass.Compact -> (carouselHeight + 20).dp // Landscape tablet
                                else -> (carouselHeight + 40).dp // Portrait tablet
                            }
                            else -> carouselHeight.dp
                        })
                        .padding(vertical = 8.dp),
                    itemSpacing = 12.dp,
                    contentPadding = PaddingValues(horizontal = 40.dp),
                    flingBehavior = CarouselDefaults.singleAdvanceFlingBehavior(state = carouselState)
                ) { itemIndex ->
                    val album = albums[itemIndex]
                    HeroCarouselCard(
                        album = album,
                        onClick = { onAlbumClick(album) },
                        modifier = Modifier.maskClip(MaterialTheme.shapes.extraLarge),
                        showAlbumName = showAlbumName,
                        showArtistName = showArtistName,
                        showYear = showYear,
                        showPlayButton = showPlayButton,
                        showGradient = showGradient,
                        itemIndex = itemIndex,
                        currentItem = carouselState.currentItem
                    )
                }
            }
        }
        
        // Modern page indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(albums.size) { index ->
                val isSelected = index == carouselState.currentItem
                
                val width by animateFloatAsState(
                    targetValue = if (isSelected) 24f else 8f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "indicator_width"
                )
                
                val color = if (isSelected) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .width(width.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(color)
                        .clickable { 
                            HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                            scope.launch {
                                carouselState.animateScrollToItem(index)
                            }
                        }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun androidx.compose.material3.carousel.CarouselItemScope.HeroCarouselCard(
    album: Album,
    onClick: (Album) -> Unit,
    modifier: Modifier = Modifier,
    showAlbumName: Boolean = true,
    showArtistName: Boolean = true,
    showYear: Boolean = true,
    showPlayButton: Boolean = true,
    showGradient: Boolean = true,
    itemIndex: Int = 0,
    currentItem: Int = 0
) {
    val context = LocalContext.current
    val viewModel = viewModel<chromahub.rhythm.app.viewmodel.MusicViewModel>()
    val haptic = LocalHapticFeedback.current
    
    // Hide text content on peeked items (not the current centered item)
    val isPeeked = itemIndex != currentItem
    
    Card(
        onClick = {
            HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
            onClick(album)
        },
        modifier = modifier
            .fillMaxSize(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Album artwork background
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .apply(ImageUtils.buildImageRequest(
                        album.artworkUri,
                        album.title,
                        context.cacheDir,
                        M3PlaceholderType.ALBUM
                    ))
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            
            // Enhanced gradient overlays for better text readability
            if (showGradient) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.3f),
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                                ),
                                startY = 0f,
                                endY = Float.POSITIVE_INFINITY
                            )
                        )
                )
            }
            
            // Content - only show if at least one element is visible and not in peek mode
            if (!isPeeked && (showAlbumName || showArtistName || showYear || showPlayButton)) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    if (showAlbumName) {
                        Text(
                            text = album.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    if (showArtistName) {
                        Text(
                            text = album.artist,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = if (showAlbumName) 4.dp else 0.dp)
                        )
                    }
                    
                    if (showYear || showPlayButton) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (showYear && album.year > 0) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Text(
                                        text = album.year.toString(),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            // Enhanced play button
                            if (showPlayButton) {
                                FilledIconButton(
                                    onClick = {
                                        HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.LongPress)
                                        viewModel.playAlbum(album)
                                        onClick(album)
                                    },
                                    modifier = Modifier.size(48.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = context.getString(R.string.cd_play_album),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Additional Modern Components

@Composable
private fun ModernArtistsSection(
    artists: List<Artist>,
    songs: List<Song>,
    onArtistClick: (Artist) -> Unit,
    onViewAllArtists: () -> Unit,
    widthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
    heightSizeClass: WindowHeightSizeClass = WindowHeightSizeClass.Medium
) {
    val context = LocalContext.current
    Column {
        ModernSectionTitle(
            title = context.getString(R.string.home_top_artists),
            subtitle = context.getString(R.string.home_top_artists_subtitle),
            viewAllAction = onViewAllArtists
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        val isTablet = widthSizeClass != WindowWidthSizeClass.Compact
        if (isTablet) {
            // Grid layout for tablets - better display of artists
            val gridColumns = when (widthSizeClass) {
                WindowWidthSizeClass.Medium -> 4  // 4 columns for medium tablets
                WindowWidthSizeClass.Expanded -> 6  // 6 columns for large tablets
                else -> 3
            }
            val gridState = rememberLazyGridState()
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns),
                state = gridState,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.height(320.dp)  // Fixed height for artist grid
            ) {
                items(
                    items = artists,
                    key = { "artist_${it.id}" },
                    contentType = { "artist" }
                ) { artist ->
                    ModernArtistCard(
                        artist = artist,
                        songs = songs,
                        onClick = { onArtistClick(artist) },
                        widthSizeClass = widthSizeClass,
                        heightSizeClass = heightSizeClass
                    )
                }
            }
        } else {
            // Horizontal scroll for phones
            val artistsListState = rememberLazyListState()
            LazyRow(
                state = artistsListState,
                contentPadding = PaddingValues(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(
                    items = artists,
                    key = { "artist_${it.id}" },
                    contentType = { "artist" }
                ) { artist ->
                    ModernArtistCard(
                        artist = artist,
                        songs = songs,
                        onClick = { onArtistClick(artist) },
                        widthSizeClass = widthSizeClass,
                        heightSizeClass = heightSizeClass
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernArtistCard(
    artist: Artist,
    songs: List<Song>,
    onClick: () -> Unit,
    widthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
    heightSizeClass: WindowHeightSizeClass = WindowHeightSizeClass.Medium
) {
    val context = LocalContext.current
    val viewModel = viewModel<chromahub.rhythm.app.viewmodel.MusicViewModel>()
    val haptic = LocalHapticFeedback.current
    
    val cardSize = when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> when (heightSizeClass) {
            WindowHeightSizeClass.Compact -> 100.dp // Landscape phone
            else -> 120.dp // Portrait phone
        }
        WindowWidthSizeClass.Medium -> when (heightSizeClass) {
            WindowHeightSizeClass.Compact -> 110.dp // Landscape tablet - larger for grid
            else -> 120.dp // Portrait tablet - larger for grid
        }
        WindowWidthSizeClass.Expanded -> when (heightSizeClass) {
            WindowHeightSizeClass.Compact -> 115.dp // Landscape large tablet
            else -> 125.dp // Portrait large tablet
        }
        else -> 120.dp
    }
    
    val isTablet = widthSizeClass != WindowWidthSizeClass.Compact
    val columnModifier = if (isTablet) {
        // Full width for grid layout on tablets
        Modifier
            .fillMaxWidth()
            .clickable { 
                HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                onClick() 
            }
    } else {
        // Fixed width for horizontal scroll on phones
        Modifier
            .width(cardSize)
            .clickable { 
                HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                onClick() 
            }
    }

    Column(
        modifier = columnModifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(cardSize)) {
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = CircleShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                if (artist.artworkUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .apply(
                                ImageUtils.buildImageRequest(
                                    artist.artworkUri,
                                    artist.name,
                                    context.cacheDir,
                                    M3PlaceholderType.ARTIST
                                )
                            )
                            .build(),
                        contentDescription = context.getString(R.string.cd_artist, artist.name),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = RhythmIcons.Artist,
                            contentDescription = context.getString(R.string.cd_artist, artist.name),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
            
            FilledIconButton(
                onClick = {
                    HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.LongPress)
                    viewModel.playArtist(artist)
                    onClick()
                },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier
                    .size(40.dp)
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = context.getString(R.string.cd_play_artist, artist.name),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        Text(
            text = artist.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun ModernAlbumCard(
    album: Album,
    onClick: (Album) -> Unit,
    widthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
    heightSizeClass: WindowHeightSizeClass = WindowHeightSizeClass.Medium
) {
    val context = LocalContext.current
    val viewModel = viewModel<chromahub.rhythm.app.viewmodel.MusicViewModel>()
    val haptic = LocalHapticFeedback.current
    
    val (cardWidth, cardHeight) = when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> when (heightSizeClass) {
            WindowHeightSizeClass.Compact -> 140.dp to 210.dp // Landscape phone
            else -> 160.dp to 240.dp // Portrait phone
        }
        WindowWidthSizeClass.Medium -> when (heightSizeClass) {
            WindowHeightSizeClass.Compact -> 180.dp to 270.dp // Landscape tablet - larger cards
            else -> 200.dp to 300.dp // Portrait tablet - larger cards
        }
        WindowWidthSizeClass.Expanded -> when (heightSizeClass) {
            WindowHeightSizeClass.Compact -> 200.dp to 300.dp // Landscape large tablet - even larger
            else -> 220.dp to 330.dp // Portrait large tablet - even larger
        }
        else -> 160.dp to 240.dp
    }

    Card(
        onClick = {
            HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
            onClick(album)
        },
        modifier = Modifier
            .width(cardWidth)
            .height(cardHeight) // Fixed height to prevent layout issues
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            ),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(
            when (widthSizeClass) {
                WindowWidthSizeClass.Compact -> 12.dp
                WindowWidthSizeClass.Medium -> 16.dp  // More padding for tablets
                WindowWidthSizeClass.Expanded -> 20.dp // Even more padding for large tablets
                else -> 12.dp
            }
        )) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(24.dp)) // More rounded artwork
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .apply(ImageUtils.buildImageRequest(
                            album.artworkUri,
                            album.title,
                            context.cacheDir,
                            M3PlaceholderType.ALBUM
                        ))
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(
                            when (widthSizeClass) {
                                WindowWidthSizeClass.Compact -> 12.dp
                                WindowWidthSizeClass.Medium -> 16.dp  // More padding for tablets
                                WindowWidthSizeClass.Expanded -> 20.dp // Even more padding for large tablets
                                else -> 12.dp
                            }
                        )
                ) {
                    FilledIconButton(
                        onClick = {
                            HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.LongPress)
                            viewModel.playAlbum(album)
                        },
                        modifier = Modifier.size(
                            when (widthSizeClass) {
                                WindowWidthSizeClass.Compact -> 40.dp
                                WindowWidthSizeClass.Medium -> 48.dp  // Larger button for tablets
                                WindowWidthSizeClass.Expanded -> 52.dp // Even larger button for large tablets
                                else -> 40.dp
                            }
                        ),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = context.getString(R.string.cd_play_album),
                            modifier = Modifier.size(
                                when (widthSizeClass) {
                                    WindowWidthSizeClass.Compact -> 20.dp
                                    WindowWidthSizeClass.Medium -> 24.dp  // Larger icon for tablets
                                    WindowWidthSizeClass.Expanded -> 26.dp // Even larger icon for large tablets
                                    else -> 20.dp
                                }
                            )
                        )
                    }
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Take remaining space
                    .padding(
                        when (widthSizeClass) {
                            WindowWidthSizeClass.Compact -> 12.dp
                            WindowWidthSizeClass.Medium -> 16.dp  // More padding for tablets
                            WindowWidthSizeClass.Expanded -> 20.dp // Even more padding for large tablets
                            else -> 12.dp
                        }
                    ),
                verticalArrangement = Arrangement.spacedBy(
                    when (widthSizeClass) {
                        WindowWidthSizeClass.Compact -> 4.dp
                        WindowWidthSizeClass.Medium -> 6.dp  // More spacing for tablets
                        WindowWidthSizeClass.Expanded -> 8.dp // Even more spacing for large tablets
                        else -> 4.dp
                    }
                )
            ) {
                Text(
                    text = album.title,
                    style = when (widthSizeClass) {
                        WindowWidthSizeClass.Compact -> MaterialTheme.typography.titleSmall
                        WindowWidthSizeClass.Medium -> MaterialTheme.typography.titleMedium  // Larger text for tablets
                        WindowWidthSizeClass.Expanded -> MaterialTheme.typography.titleMedium // Larger text for large tablets
                        else -> MaterialTheme.typography.titleSmall
                    },
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = when (widthSizeClass) {
                        WindowWidthSizeClass.Compact -> 18.sp
                        WindowWidthSizeClass.Medium -> 22.sp  // Better line height for tablets
                        WindowWidthSizeClass.Expanded -> 24.sp // Better line height for large tablets
                        else -> 18.sp
                    }
                )
                
                Text(
                    text = album.artist,
                    style = when (widthSizeClass) {
                        WindowWidthSizeClass.Compact -> MaterialTheme.typography.bodySmall
                        WindowWidthSizeClass.Medium -> MaterialTheme.typography.bodyMedium  // Larger text for tablets
                        WindowWidthSizeClass.Expanded -> MaterialTheme.typography.bodyMedium // Larger text for large tablets
                        else -> MaterialTheme.typography.bodySmall
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ModernSongCard(
    song: Song,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    Card(
        onClick = {
            HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
            onClick()
        },
        modifier = Modifier
            .width(180.dp)
            .height(260.dp) // Fixed height to prevent layout issues
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f),
                spotColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), // Take most of the space
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .apply(ImageUtils.buildImageRequest(
                            song.artworkUri,
                            song.title,
                            context.cacheDir,
                            M3PlaceholderType.TRACK
                        ))
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Text section with fixed height
            Column(
                modifier = Modifier.height(60.dp), // Fixed height for text
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleSmall, // Smaller text
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 16.sp // Compact line height
                )
                
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall, // Smaller text
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ModernListeningStatsSection(
    onClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel = viewModel<chromahub.rhythm.app.viewmodel.MusicViewModel>()
    val songs by viewModel.songs.collectAsState()

    // Load stats from PlaybackStatsRepository (matching ListeningStatsScreen)
    var statsSummary by remember { mutableStateOf<chromahub.rhythm.app.shared.data.repository.PlaybackStatsRepository.PlaybackStatsSummary?>(null) }
    
    LaunchedEffect(songs) {
        statsSummary = viewModel.loadPlaybackStats(chromahub.rhythm.app.shared.data.repository.StatsTimeRange.ALL_TIME)
    }

    val listeningTimeHours = remember(statsSummary) {
        val totalMillis = statsSummary?.totalDurationMs ?: 0L
        val hours = totalMillis / (1000 * 60 * 60)
        if (hours < 1) "< 1h" else "${hours}h"
    }

    val songsPlayed = remember(statsSummary) {
        (statsSummary?.totalPlayCount ?: 0).toString()
    }

    val uniqueArtistsCount = remember(statsSummary) {
        (statsSummary?.uniqueArtists ?: 0).toString()
    }

    // Enhanced stats card with better design
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            // Header with icon and title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BarChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = context.getString(R.string.home_listening_stats),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = context.getString(R.string.home_listening_stats_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Arrow indicator for navigation
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = context.getString(R.string.cd_view_detailed_stats),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Stats grid with better layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                EnhancedStatItem(
                    modifier = Modifier.weight(1f),
                    value = listeningTimeHours,
                    label = context.getString(R.string.home_stat_listening_time),
                    icon = RhythmIcons.Player.Timer,
                    accentColor = MaterialTheme.colorScheme.primary
                )
                
                EnhancedStatItem(
                    modifier = Modifier.weight(1f),
                    value = songsPlayed,
                    label = context.getString(R.string.home_stat_songs_played),
                    icon = RhythmIcons.Music.MusicNote,
                    accentColor = MaterialTheme.colorScheme.secondary
                )
                
                EnhancedStatItem(
                    modifier = Modifier.weight(1f),
                    value = uniqueArtistsCount,
                    label = context.getString(R.string.home_stat_artists),
                    icon = RhythmIcons.Artist,
                    accentColor = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun EnhancedStatItem(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    icon: ImageVector,
    accentColor: Color
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Icon with accent color background
        Surface(
            shape = CircleShape,
            color = accentColor.copy(alpha = 0.12f),
            modifier = Modifier.size(56.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        
        // Value with emphasis
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        // Label
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun ModernStatCard(
    value: String,
    label: String,
    icon: ImageVector,
    backgroundColor: Color,
    contentColor: Color
) {
    Card(
        modifier = Modifier.width(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = contentColor.copy(alpha = 0.2f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                textAlign = TextAlign.Center
            )

            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ModernMoodSection(
    moodBasedSongs: List<Song>,
    energeticSongs: List<Song>,
    relaxingSongs: List<Song>,
    onSongClick: (Song) -> Unit
) {
    val context = LocalContext.current
    val viewModel = viewModel<chromahub.rhythm.app.viewmodel.MusicViewModel>()
    val haptic = LocalHapticFeedback.current
    
    Column {
        ModernSectionTitle(
            title = context.getString(R.string.home_mood_title),
            subtitle = context.getString(R.string.home_mood_subtitle)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Remember lazy row state for scroll position preservation (Compose 1.10)
        val moodListState = rememberLazyListState()
        LazyRow(
            state = moodListState,
            contentPadding = PaddingValues(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "mood_energize", contentType = "mood_card") {
                ModernMoodCard(
                    title = context.getString(R.string.home_mood_energize),
                    description = context.getString(R.string.home_mood_energize_desc),
                    songs = energeticSongs,
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    icon = RhythmIcons.Energy,
                    songCount = energeticSongs.size,
                    onPlayClick = {
                        HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.LongPress)
                        if (energeticSongs.isNotEmpty()) {
                            // Play all energetic songs shuffled
                            viewModel.playQueue(energeticSongs.shuffled())
                            onSongClick(energeticSongs.first())
                        }
                    }
                )
            }
            
            item(key = "mood_relax", contentType = "mood_card") {
                ModernMoodCard(
                    title = context.getString(R.string.home_mood_relax),
                    description = context.getString(R.string.home_mood_relax_desc),
                    songs = relaxingSongs,
                    backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    icon = RhythmIcons.Relax,
                    songCount = relaxingSongs.size,
                    onPlayClick = {
                        HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.LongPress)
                        if (relaxingSongs.isNotEmpty()) {
                            // Play all relaxing songs in order
                            viewModel.playQueue(relaxingSongs)
                            onSongClick(relaxingSongs.first())
                        }
                    }
                )
            }
            
            item(key = "mood_focus", contentType = "mood_card") {
                ModernMoodCard(
                    title = context.getString(R.string.home_mood_focus),
                    description = context.getString(R.string.home_mood_focus_desc),
                    songs = moodBasedSongs,
                    backgroundColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    icon = RhythmIcons.Focus,
                    songCount = moodBasedSongs.size,
                    onPlayClick = {
                        HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.LongPress)
                        if (moodBasedSongs.isNotEmpty()) {
                            // Play all focus songs in order
                            viewModel.playQueue(moodBasedSongs)
                            onSongClick(moodBasedSongs.first())
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ModernMoodCard(
    title: String,
    description: String,
    songs: List<Song>,
    backgroundColor: Color,
    contentColor: Color,
    icon: ImageVector,
    songCount: Int,
    onPlayClick: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    
    Card(
        modifier = Modifier
            .width(200.dp)
            .height(220.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(32.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(32.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor.copy(alpha = 0.8f)
                    )
                }
                
                FilledIconButton(
                    onClick = {
                        HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.LongPress)
                        onPlayClick()
                    },
                    modifier = Modifier.size(48.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = contentColor,
                        contentColor = backgroundColor
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = context.getString(R.string.ui_play_action, title),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Surface(
                color = contentColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(bottomStart = 20.dp, topEnd = 36.dp),
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Text(
                    text = context.getString(R.string.ui_songs_count, songCount),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun ModernUpdateSection(
    latestVersion: AppVersion,
    onUpdateClick: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var isDownloading by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = {
                if (!isDownloading) {
                    HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                    onUpdateClick(false)
                }
            }),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.rhythm_splash_logo),
                    contentDescription = context.getString(R.string.cd_rhythm_logo),
                    modifier = Modifier.size(45.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = context.getString(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
                ) {
                    Icon(
                        imageVector = RhythmIcons.Download,
                        contentDescription = context.getString(R.string.update_available_cd),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .size(28.dp)
                            .padding(10.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(20.dp))
                
                Text(
                    text = context.getString(R.string.update_available_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = context.getString(R.string.update_version, latestVersion.versionName),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (latestVersion.whatsNew.isNotEmpty()) {
                Text(
                    text = context.getString(R.string.update_whats_new),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                latestVersion.whatsNew.take(2).forEach { item ->
                    Text(
                        text = HtmlCompat.fromHtml(item, HtmlCompat.FROM_HTML_MODE_COMPACT).toString(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            ElevatedCard(
                onClick = {
                    if (!isDownloading) {
                        isDownloading = true
                        HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.TextHandleMove)
                        onUpdateClick(false)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isDownloading) context.getString(R.string.update_downloading) else context.getString(R.string.update_now),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = "Update",
                            tint = MaterialTheme.colorScheme.primaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernRecommendedSection(
    recommendedSongs: List<Song>,
    onSongClick: (Song) -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        ModernSectionTitle(
            title = context.getString(R.string.home_recommended_title),
            subtitle = context.getString(R.string.home_recommended_subtitle)
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        if (recommendedSongs.isNotEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(25.dp)
                ) {
                    recommendedSongs.forEachIndexed { index, song ->
                        RecommendedSongItem(
                            song = song,
                            onClick = { onSongClick(song) }
                        )
                        
                        if (index != recommendedSongs.lastIndex) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        } else {
            ModernEmptyState(
                icon = Icons.Rounded.TipsAndUpdates,
                title = context.getString(R.string.home_no_recommendations),
                subtitle = context.getString(R.string.home_no_recommendations_desc),
                iconSize = 48.dp
            )
        }
    }
}

@Composable
private fun RecommendedSongItem(
    song: Song,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = {
                HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                onClick()
            })
            .padding(vertical = 12.dp), // Increased vertical padding
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(52.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .apply(ImageUtils.buildImageRequest(
                        song.artworkUri,
                        song.title,
                        context.cacheDir,
                        M3PlaceholderType.TRACK
                    ))
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = "${song.artist} • ${song.album}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        FilledIconButton(
            onClick = {
                HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                onClick()
            },
            modifier = Modifier.size(40.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            )
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = "Play",
                modifier = Modifier.size(25.dp)
            )
        }
    }
}

@Composable
private fun ModernEmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconSize: Dp = 64.dp // Increased icon size
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp) // Increased height for better visual presence
            .padding(horizontal = 8.dp), // Added horizontal padding
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow // Subtle background
        ),
        shape = RoundedCornerShape(28.dp) // Rounded corners for the card
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp), // Increased padding inside the card
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), // Changed tint to primary with alpha
                modifier = Modifier.size(iconSize)
            )
            
            Spacer(modifier = Modifier.height(16.dp)) // Increased spacing
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge, // Larger title
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp)) // Increased spacing
            
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge, // Larger subtitle
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp) // Added horizontal padding for subtitle
            )
        }
    }
}
