package chromahub.rhythm.app.infrastructure.widget.glance

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import chromahub.rhythm.app.activities.MainActivity
import chromahub.rhythm.app.R
import androidx.glance.appwidget.cornerRadius
import androidx.glance.layout.ContentScale
import androidx.glance.appwidget.ImageProvider as AppWidgetImageProvider
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.currentState
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey

/**
 * Modern Glance-based Music Widget with Material 3 Expressive Design
 * 
 * Features:
 * - Material 3 dynamic colors and theming
 * - Android 16 style rounded icons and buttons
 * - Expressive rounded corners and spacing  
 * - Responsive layouts for different sizes (2x1 to 5x5)
 * - Rounded rectangle play/pause button (Android 16 style)
 * - Smooth interactions with proper touch feedback
 * - Optimized visual hierarchy and readability
 * - Adaptive content based on available space
 */
class RhythmMusicWidget : GlanceAppWidget() {
    
    companion object {
        // Widget state keys
        const val KEY_SONG_TITLE = "song_title"
        const val KEY_ARTIST_NAME = "artist_name"
        const val KEY_ALBUM_NAME = "album_name"
        const val KEY_IS_PLAYING = "is_playing"
        const val KEY_ARTWORK_URI = "artwork_uri"
        const val KEY_HAS_PREVIOUS = "has_previous"
        const val KEY_HAS_NEXT = "has_next"
    }
    
    // Use preferences-based state definition for reactive updates
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    
    // Responsive sizing - adapts to different widget sizes
    override val sizeMode = SizeMode.Responsive(
        setOf(
            WidgetSize.ExtraSmall,  // 2x1 cells - minimal
            WidgetSize.Small,       // 2x2 cells - compact with controls
            WidgetSize.Medium,      // 3x2 cells - horizontal layout
            WidgetSize.Wide,        // 4x2 cells - extended horizontal
            WidgetSize.Large,       // 3x3 cells - vertical with full info
            WidgetSize.ExtraLarge   // 4x4 cells - maximum detail
        )
    )
    
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val appSettings = chromahub.rhythm.app.shared.data.model.AppSettings.getInstance(context)
            val widgetData = getWidgetData(prefs, appSettings)
            GlanceTheme {
                ResponsiveWidgetContent(widgetData)
            }
        }
    }
    
    @Composable
    private fun ResponsiveWidgetContent(data: WidgetData) {
        val size = androidx.glance.LocalSize.current
        
        when {
            size.width >= WidgetSize.ExtraLarge.width -> ExtraLargeWidgetLayout(data)
            size.width >= WidgetSize.Wide.width -> WideWidgetLayout(data)
            size.width >= WidgetSize.Large.width && size.height >= WidgetSize.Large.height -> LargeWidgetLayout(data)
            size.width >= WidgetSize.Medium.width -> MediumWidgetLayout(data)
            size.height >= WidgetSize.Small.height -> SmallWidgetLayout(data)
            else -> ExtraSmallWidgetLayout(data)
        }
    }
    
    @Composable
    private fun ExtraSmallWidgetLayout(data: WidgetData) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(data.cornerRadius.dp)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            // Content with padding
            Row(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.Start
            ) {
                // Compact album art with background
                if (data.showAlbumArt) {
                    Box(
                        modifier = GlanceModifier
                            .size(32.dp)
                            .cornerRadius(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (data.artworkUri != null) {
                            Image(
                                provider = AppWidgetImageProvider(data.artworkUri),
                                contentDescription = "Album Art",
                                modifier = GlanceModifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = GlanceModifier
                                    .fillMaxSize()
                                    .background(GlanceTheme.colors.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    provider = ImageProvider(R.drawable.ic_music_note),
                                    contentDescription = "Music",
                                    modifier = GlanceModifier.size(18.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = GlanceModifier.width(8.dp))
                }
                
                // Song info - ultra compact
                Column(
                    modifier = GlanceModifier.defaultWeight()
                ) {
                    Text(
                        text = data.songTitle,
                        style = TextStyle(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlanceTheme.colors.onSurface
                        ),
                        maxLines = 1
                    )
                    if (data.showArtist) {
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = data.artistName,
                            style = TextStyle(
                                fontSize = 10.sp,
                                color = GlanceTheme.colors.onSurfaceVariant
                            ),
                            maxLines = 1
                        )
                    }
                }
                
                Spacer(modifier = GlanceModifier.width(6.dp))
                
                // Play button only - Android 16 rounded rectangle
                Box(
                    modifier = GlanceModifier
                        .size(width = 40.dp, height = 32.dp)
                        .cornerRadius(16.dp)
                        .background(GlanceTheme.colors.primary)
                        .clickable(actionRunCallback<PlayPauseAction>()),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        provider = ImageProvider(
                            if (data.isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
                        ),
                        contentDescription = if (data.isPlaying) "Pause" else "Play",
                        modifier = GlanceModifier.size(16.dp)
                    )
                }
            }
        }
    }
    
    @Composable
    private fun SmallWidgetLayout(data: WidgetData) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(data.cornerRadius.dp)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            // Content with improved padding
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album art with modern design
                if (data.showAlbumArt) {
                    Box(
                        modifier = GlanceModifier
                            .size(72.dp)
                            .cornerRadius(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (data.artworkUri != null) {
                            Image(
                                provider = AppWidgetImageProvider(data.artworkUri),
                                contentDescription = "Album Art",
                                modifier = GlanceModifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            // Default background for music icon
                            Box(
                                modifier = GlanceModifier
                                    .fillMaxSize()
                                    .background(GlanceTheme.colors.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    provider = ImageProvider(R.drawable.ic_music_note),
                                    contentDescription = "Music",
                                    modifier = GlanceModifier.size(40.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = GlanceModifier.height(10.dp))
                }
                
                // Song info - improved typography
                Text(
                    text = data.songTitle,
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onSurface
                    ),
                    maxLines = 1
                )
                
                if (data.showArtist) {
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    
                    Text(
                        text = data.artistName,
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = GlanceTheme.colors.onSurfaceVariant
                        ),
                        maxLines = 1
                    )
                }
                
                Spacer(modifier = GlanceModifier.height(14.dp))
                
                // Playback controls - refined buttons
                Row(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous button
                    Box(
                        modifier = GlanceModifier
                            .size(44.dp)
                            .cornerRadius(22.dp)
                            .background(GlanceTheme.colors.secondaryContainer)
                            .clickable(actionRunCallback<SkipPreviousAction>()),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_skip_previous),
                            contentDescription = "Previous",
                            modifier = GlanceModifier.size(22.dp)
                        )
                    }
                    
                    Spacer(modifier = GlanceModifier.width(10.dp))
                    
                    // Play/Pause button - Android 16 rounded rectangle hero
                    Box(
                        modifier = GlanceModifier
                            .size(width = 64.dp, height = 56.dp)
                            .cornerRadius(20.dp)
                            .background(GlanceTheme.colors.primary)
                            .clickable(actionRunCallback<PlayPauseAction>()),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(
                                if (data.isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
                            ),
                            contentDescription = if (data.isPlaying) "Pause" else "Play",
                            modifier = GlanceModifier.size(28.dp)
                        )
                    }
                    
                    Spacer(modifier = GlanceModifier.width(10.dp))
                    
                    // Next button
                    Box(
                        modifier = GlanceModifier
                            .size(44.dp)
                            .cornerRadius(22.dp)
                            .background(GlanceTheme.colors.secondaryContainer)
                            .clickable(actionRunCallback<SkipNextAction>()),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_skip_next),
                            contentDescription = "Next",
                            modifier = GlanceModifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
    
    @Composable
    private fun MediumWidgetLayout(data: WidgetData) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(data.cornerRadius.dp)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            // Content with padding
            Row(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album art - squircle with proper sizing
                Box(
                    modifier = GlanceModifier
                        .size(88.dp)
                        .cornerRadius(24.dp), // Squircle like PlayerScreen
                    contentAlignment = Alignment.Center
                ) {
                    if (data.artworkUri != null) {
                        Image(
                            provider = AppWidgetImageProvider(data.artworkUri),
                            contentDescription = "Album Art",
                            modifier = GlanceModifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            provider = ImageProvider(R.drawable.ic_music_note),
                            contentDescription = "Default Music Icon",
                            modifier = GlanceModifier.size(48.dp)
                        )
                    }
                }
                
                Spacer(modifier = GlanceModifier.width(16.dp))
                
                // Song info and controls
                Column(
                    modifier = GlanceModifier.defaultWeight(),
                    verticalAlignment = Alignment.Top
                ) {
                    // Song title - bold and prominent
                    Text(
                        text = data.songTitle,
                        style = TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlanceTheme.colors.onSurface
                        ),
                        maxLines = 2
                    )
                    
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    
                    // Artist name
                    Text(
                        text = data.artistName,
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = GlanceTheme.colors.onSurfaceVariant
                        ),
                        maxLines = 1
                    )
                    
                    Spacer(modifier = GlanceModifier.height(12.dp))
                    
                    // Playback controls - matching PlayerScreen circular style
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalAlignment = Alignment.Start
                    ) {
                        // Previous button - circular secondary container
                        Box(
                            modifier = GlanceModifier
                                .size(48.dp)
                                .cornerRadius(24.dp) // Circular
                                .background(GlanceTheme.colors.secondaryContainer)
                                .clickable(actionRunCallback<SkipPreviousAction>()),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.ic_skip_previous),
                                contentDescription = "Previous",
                                modifier = GlanceModifier.size(24.dp)
                            )
                        }
                        
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        
                        // Play/Pause button - Android 16 rounded rectangle
                        Box(
                            modifier = GlanceModifier
                                .size(width = 64.dp, height = 56.dp)
                                .cornerRadius(20.dp)
                                .background(GlanceTheme.colors.primary)
                                .clickable(actionRunCallback<PlayPauseAction>()),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                provider = ImageProvider(
                                    if (data.isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
                                ),
                                contentDescription = if (data.isPlaying) "Pause" else "Play",
                                modifier = GlanceModifier.size(28.dp)
                            )
                        }
                        
                        Spacer(modifier = GlanceModifier.width(8.dp))
                        
                        // Next button - circular secondary container
                        Box(
                            modifier = GlanceModifier
                                .size(48.dp)
                                .cornerRadius(24.dp) // Circular
                                .background(GlanceTheme.colors.secondaryContainer)
                                .clickable(actionRunCallback<SkipNextAction>()),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.ic_skip_next),
                                contentDescription = "Next",
                                modifier = GlanceModifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
    
    @Composable
    private fun WideWidgetLayout(data: WidgetData) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(data.cornerRadius.dp)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            // Content with padding
            Row(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album art - larger for wide layout
                Box(
                    modifier = GlanceModifier
                        .size(96.dp)
                        .cornerRadius(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (data.artworkUri != null) {
                        Image(
                            provider = AppWidgetImageProvider(data.artworkUri),
                            contentDescription = "Album Art",
                            modifier = GlanceModifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            provider = ImageProvider(R.drawable.ic_music_note),
                            contentDescription = "Default Music Icon",
                            modifier = GlanceModifier.size(52.dp)
                        )
                    }
                }
                
                Spacer(modifier = GlanceModifier.width(16.dp))
                
                // Song info and controls
                Column(
                    modifier = GlanceModifier.defaultWeight(),
                    verticalAlignment = Alignment.Top
                ) {
                    // Song title
                    Text(
                        text = data.songTitle,
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlanceTheme.colors.onSurface
                        ),
                        maxLines = 1
                    )
                    
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    
                    // Artist name
                    Text(
                        text = data.artistName,
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = GlanceTheme.colors.onSurfaceVariant
                        ),
                        maxLines = 1
                    )
                    
                    if (data.albumName.isNotEmpty()) {
                        Spacer(modifier = GlanceModifier.height(2.dp))
                        Text(
                            text = data.albumName,
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = GlanceTheme.colors.onSurfaceVariant
                            ),
                            maxLines = 1
                        )
                    }
                }
                
                Spacer(modifier = GlanceModifier.width(12.dp))
                
                // Full playback controls horizontally
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.End
                ) {
                    // Previous button
                    Box(
                        modifier = GlanceModifier
                            .size(52.dp)
                            .cornerRadius(26.dp)
                            .background(GlanceTheme.colors.secondaryContainer)
                            .clickable(actionRunCallback<SkipPreviousAction>()),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_skip_previous),
                            contentDescription = "Previous",
                            modifier = GlanceModifier.size(26.dp)
                        )
                    }
                    
                    Spacer(modifier = GlanceModifier.width(10.dp))
                    
                    // Play/Pause button - Android 16 rounded rectangle
                    Box(
                        modifier = GlanceModifier
                            .size(width = 68.dp, height = 60.dp)
                            .cornerRadius(22.dp)
                            .background(GlanceTheme.colors.primary)
                            .clickable(actionRunCallback<PlayPauseAction>()),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(
                                if (data.isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
                            ),
                            contentDescription = if (data.isPlaying) "Pause" else "Play",
                            modifier = GlanceModifier.size(30.dp)
                        )
                    }
                    
                    Spacer(modifier = GlanceModifier.width(10.dp))
                    
                    // Next button
                    Box(
                        modifier = GlanceModifier
                            .size(52.dp)
                            .cornerRadius(26.dp)
                            .background(GlanceTheme.colors.secondaryContainer)
                            .clickable(actionRunCallback<SkipNextAction>()),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_skip_next),
                            contentDescription = "Next",
                            modifier = GlanceModifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }
    
    @Composable
    private fun LargeWidgetLayout(data: WidgetData) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(data.cornerRadius.dp)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            // Content with padding
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Album art - hero element with expressive squircle
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .cornerRadius(32.dp), // Large squircle
                    contentAlignment = Alignment.Center
                ) {
                    if (data.artworkUri != null) {
                        Image(
                            provider = AppWidgetImageProvider(data.artworkUri),
                            contentDescription = "Album Art",
                            modifier = GlanceModifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            provider = ImageProvider(R.drawable.ic_music_note),
                            contentDescription = "Default Music Icon",
                            modifier = GlanceModifier.size(72.dp)
                        )
                    }
                }
                
                Spacer(modifier = GlanceModifier.height(16.dp))
                
                // Song information - prominent and centered
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = data.songTitle,
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlanceTheme.colors.onSurface
                        ),
                        maxLines = 2
                    )
                    
                    Spacer(modifier = GlanceModifier.height(6.dp))
                    
                    Text(
                        text = data.artistName,
                        style = TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = GlanceTheme.colors.onSurfaceVariant
                        ),
                        maxLines = 1
                    )
                    
                    if (data.albumName.isNotEmpty()) {
                        Spacer(modifier = GlanceModifier.height(4.dp))
                        Text(
                            text = data.albumName,
                            style = TextStyle(
                                fontSize = 13.sp,
                                color = GlanceTheme.colors.onSurfaceVariant
                            ),
                            maxLines = 1
                        )
                    }
                }
                
                Spacer(modifier = GlanceModifier.height(20.dp))
                
                // Full playback controls - matching PlayerScreen circular style
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous button - circular secondary
                    Box(
                        modifier = GlanceModifier
                            .size(56.dp)
                            .cornerRadius(28.dp) // Circular
                            .background(GlanceTheme.colors.secondaryContainer)
                            .clickable(actionRunCallback<SkipPreviousAction>()),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_skip_previous),
                            contentDescription = "Previous",
                            modifier = GlanceModifier.size(28.dp)
                        )
                    }
                    
                    Spacer(modifier = GlanceModifier.width(16.dp))
                    
                    // Play/Pause button - Android 16 rounded rectangle hero
                    Box(
                        modifier = GlanceModifier
                            .size(width = 76.dp, height = 68.dp)
                            .cornerRadius(24.dp)
                            .background(GlanceTheme.colors.primary)
                            .clickable(actionRunCallback<PlayPauseAction>()),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(
                                if (data.isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
                            ),
                            contentDescription = if (data.isPlaying) "Pause" else "Play",
                            modifier = GlanceModifier.size(36.dp)
                        )
                    }
                    
                    Spacer(modifier = GlanceModifier.width(16.dp))
                    
                    // Next button - circular secondary
                    Box(
                        modifier = GlanceModifier
                            .size(56.dp)
                            .cornerRadius(28.dp) // Circular
                            .background(GlanceTheme.colors.secondaryContainer)
                            .clickable(actionRunCallback<SkipNextAction>()),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_skip_next),
                            contentDescription = "Next",
                            modifier = GlanceModifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
    
    @Composable
    private fun ExtraLargeWidgetLayout(data: WidgetData) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(GlanceTheme.colors.widgetBackground)
                .cornerRadius(data.cornerRadius.dp)
                .clickable(actionStartActivity<MainActivity>())
        ) {
            // Content with padding
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Album art - maximum size hero element
                Box(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .cornerRadius(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (data.artworkUri != null) {
                        Image(
                            provider = AppWidgetImageProvider(data.artworkUri),
                            contentDescription = "Album Art",
                            modifier = GlanceModifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Image(
                            provider = ImageProvider(R.drawable.ic_music_note),
                            contentDescription = "Default Music Icon",
                            modifier = GlanceModifier.size(96.dp)
                        )
                    }
                }
                
                Spacer(modifier = GlanceModifier.height(20.dp))
                
                // Song information - maximum detail
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = data.songTitle,
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlanceTheme.colors.onSurface
                        ),
                        maxLines = 2
                    )
                    
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    
                    Text(
                        text = data.artistName,
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = GlanceTheme.colors.onSurfaceVariant
                        ),
                        maxLines = 1
                    )
                    
                    if (data.albumName.isNotEmpty()) {
                        Spacer(modifier = GlanceModifier.height(6.dp))
                        Text(
                            text = data.albumName,
                            style = TextStyle(
                                fontSize = 14.sp,
                                color = GlanceTheme.colors.onSurfaceVariant
                            ),
                            maxLines = 1
                        )
                    }
                }
                
                Spacer(modifier = GlanceModifier.height(24.dp))
                
                // Large playback controls
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous button
                    Box(
                        modifier = GlanceModifier
                            .size(64.dp)
                            .cornerRadius(32.dp)
                            .background(GlanceTheme.colors.secondaryContainer)
                            .clickable(actionRunCallback<SkipPreviousAction>()),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_skip_previous),
                            contentDescription = "Previous",
                            modifier = GlanceModifier.size(32.dp)
                        )
                    }
                    
                    Spacer(modifier = GlanceModifier.width(20.dp))
                    
                    // Play/Pause button - Android 16 rounded rectangle hero
                    Box(
                        modifier = GlanceModifier
                            .size(width = 84.dp, height = 76.dp)
                            .cornerRadius(28.dp)
                            .background(GlanceTheme.colors.primary)
                            .clickable(actionRunCallback<PlayPauseAction>()),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(
                                if (data.isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow
                            ),
                            contentDescription = if (data.isPlaying) "Pause" else "Play",
                            modifier = GlanceModifier.size(40.dp)
                        )
                    }
                    
                    Spacer(modifier = GlanceModifier.width(20.dp))
                    
                    // Next button
                    Box(
                        modifier = GlanceModifier
                            .size(64.dp)
                            .cornerRadius(32.dp)
                            .background(GlanceTheme.colors.secondaryContainer)
                            .clickable(actionRunCallback<SkipNextAction>()),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            provider = ImageProvider(R.drawable.ic_skip_next),
                            contentDescription = "Next",
                            modifier = GlanceModifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
    
    private fun getWidgetData(prefs: Preferences, appSettings: chromahub.rhythm.app.shared.data.model.AppSettings): WidgetData {
        return WidgetData(
            songTitle = prefs[stringPreferencesKey(KEY_SONG_TITLE)] ?: "No song playing",
            artistName = prefs[stringPreferencesKey(KEY_ARTIST_NAME)] ?: "Unknown artist",
            albumName = prefs[stringPreferencesKey(KEY_ALBUM_NAME)] ?: "",
            isPlaying = prefs[booleanPreferencesKey(KEY_IS_PLAYING)] ?: false,
            artworkUri = prefs[stringPreferencesKey(KEY_ARTWORK_URI)]?.let { android.net.Uri.parse(it) },
            hasPrevious = prefs[booleanPreferencesKey(KEY_HAS_PREVIOUS)] ?: false,
            hasNext = prefs[booleanPreferencesKey(KEY_HAS_NEXT)] ?: false,
            showAlbumArt = appSettings.widgetShowAlbumArt.value,
            showArtist = appSettings.widgetShowArtist.value,
            showAlbum = appSettings.widgetShowAlbum.value,
            cornerRadius = appSettings.widgetCornerRadius.value
        )
    }
}

/**
 * Widget data class
 */
data class WidgetData(
    val songTitle: String,
    val artistName: String,
    val albumName: String,
    val isPlaying: Boolean,
    val artworkUri: android.net.Uri?,
    val hasPrevious: Boolean,
    val hasNext: Boolean,
    val showAlbumArt: Boolean = true,
    val showArtist: Boolean = true,
    val showAlbum: Boolean = true,
    val cornerRadius: Int = 24,
    val transparency: Int = 85
)

/**
 * Predefined widget sizes following Material 3 responsive guidelines
 */
object WidgetSize {
    val ExtraSmall = DpSize(110.dp, 48.dp)   // 2x1 cells - minimal horizontal
    val Small = DpSize(120.dp, 120.dp)       // 2x2 cells - compact square
    val Medium = DpSize(250.dp, 120.dp)      // 3x2 cells - standard horizontal  
    val Wide = DpSize(320.dp, 120.dp)        // 4x2 cells - extended horizontal
    val Large = DpSize(250.dp, 250.dp)       // 3x3 cells - large square
    val ExtraLarge = DpSize(320.dp, 320.dp)  // 4x4 cells - maximum size
}
