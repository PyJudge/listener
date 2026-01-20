package chromahub.rhythm.app.features.streaming.presentation.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOutQuart
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import chromahub.rhythm.app.features.streaming.presentation.viewmodel.StreamingMusicViewModel
import chromahub.rhythm.app.features.streaming.presentation.screens.StreamingHomeScreen
import chromahub.rhythm.app.features.streaming.presentation.screens.StreamingSearchScreen
import chromahub.rhythm.app.features.streaming.presentation.screens.StreamingLibraryScreen
import chromahub.rhythm.app.util.HapticUtils

/**
 * Streaming screen route definitions.
 */
sealed class StreamingScreen(val route: String) {
    object Home : StreamingScreen("streaming_home")
    object Search : StreamingScreen("streaming_search")
    object Library : StreamingScreen("streaming_library")
    object PlaylistDetail : StreamingScreen("streaming_playlist/{playlistId}") {
        fun createRoute(playlistId: String) = "streaming_playlist/$playlistId"
    }
}

/**
 * Navigation host for the streaming music feature.
 * Uses same navigation bar style as local navigation.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun StreamingNavigation(
    navController: NavHostController = rememberNavController(),
    streamingMusicViewModel: StreamingMusicViewModel = viewModel(),
    onNavigateToSettings: () -> Unit = {},
    onNavigateToPlayer: () -> Unit = {},
    onSwitchToLocalMode: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: StreamingScreen.Home.route
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    
    // Tablet detection
    val windowSizeClass = calculateWindowSizeClass(context as android.app.Activity)
    val isTablet = windowSizeClass.widthSizeClass >= WindowWidthSizeClass.Medium
    
    // Show bottom nav on main screens only
    val showNavBar = currentRoute in listOf(
        StreamingScreen.Home.route,
        StreamingScreen.Library.route,
        StreamingScreen.Search.route
    )
    
    if (isTablet) {
        // Tablet layout with Navigation Rail
        Row(modifier = modifier.fillMaxSize()) {
            // Navigation rail for tablets
            AnimatedVisibility(
                visible = showNavBar,
                enter = slideInHorizontally(
                    initialOffsetX = { -it / 2 },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ),
                exit = slideOutHorizontally(
                    targetOffsetX = { -it / 2 },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeOut(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            ) {
                StreamingNavigationRail(
                    currentRoute = currentRoute,
                    navController = navController,
                    onNavigateToSettings = onNavigateToSettings,
                    context = context,
                    haptic = haptic
                )
            }
            
            // Main content for tablet
            NavHost(
                navController = navController,
                startDestination = StreamingScreen.Home.route,
                modifier = Modifier.fillMaxSize()
            ) {
                // Home Screen
                composable(
                    route = StreamingScreen.Home.route,
                    enterTransition = { fadeIn(animationSpec = tween(300)) },
                    exitTransition = { fadeOut(animationSpec = tween(300)) }
                ) {
                    StreamingHomeScreen(
                        onNavigateToSearch = { navController.navigate(StreamingScreen.Search.route) },
                        onNavigateToLibrary = { navController.navigate(StreamingScreen.Library.route) },
                        onNavigateToSettings = onNavigateToSettings,
                        onSwitchToLocalMode = onSwitchToLocalMode
                    )
                }
                
                // Search Screen
                composable(
                    route = StreamingScreen.Search.route,
                    enterTransition = {
                        fadeIn(animationSpec = tween(300)) +
                        slideInVertically(
                            initialOffsetY = { it / 4 },
                            animationSpec = tween(350, easing = EaseInOutQuart)
                        )
                    },
                    exitTransition = {
                        fadeOut(animationSpec = tween(300)) +
                        slideOutVertically(
                            targetOffsetY = { it / 4 },
                            animationSpec = tween(350, easing = EaseInOutQuart)
                        )
                    }
                ) {
                    StreamingSearchScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                
                // Library Screen
                composable(
                    route = StreamingScreen.Library.route,
                    enterTransition = {
                        fadeIn(animationSpec = tween(300)) +
                        slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = tween(350, easing = EaseInOutQuart)
                        )
                    },
                    exitTransition = {
                        fadeOut(animationSpec = tween(300)) +
                        slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = tween(350, easing = EaseInOutQuart)
                        )
                    }
                ) {
                    StreamingLibraryScreen(
                        onNavigateToPlaylist = { playlistId ->
                            navController.navigate(StreamingScreen.PlaylistDetail.createRoute(playlistId))
                        }
                    )
                }
                
                // Playlist Detail (placeholder)
                composable(
                    route = StreamingScreen.PlaylistDetail.route,
                    arguments = listOf(
                        navArgument("playlistId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val playlistId = backStackEntry.arguments?.getString("playlistId") ?: return@composable
                    // Placeholder for now
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Playlist: $playlistId\n\nComing Soon")
                    }
                }
            }
        }
    } else {
        // Phone layout with bottom navigation
        Scaffold(
            bottomBar = {
                // Bottom navigation bar matching local navigation style
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    // Navigation bar shown only on specific routes with spring animation
                    AnimatedVisibility(
                        visible = showNavBar,
                        enter = slideInVertically(
                            initialOffsetY = { fullHeight -> fullHeight / 2 },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        ) + fadeIn(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        ),
                        exit = slideOutVertically(
                            targetOffsetY = { fullHeight -> fullHeight / 2 },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        ) + fadeOut(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    ) {
                        StreamingBottomNavBar(
                            currentRoute = currentRoute,
                            navController = navController,
                            context = context
                        )
                    }
                }
            },
            modifier = modifier
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = StreamingScreen.Home.route,
                modifier = Modifier.padding(paddingValues)
            ) {
                // Home Screen
                composable(
                    route = StreamingScreen.Home.route,
                    enterTransition = { fadeIn(animationSpec = tween(300)) },
                    exitTransition = { fadeOut(animationSpec = tween(300)) }
                ) {
                    StreamingHomeScreen(
                        onNavigateToSearch = { navController.navigate(StreamingScreen.Search.route) },
                        onNavigateToLibrary = { navController.navigate(StreamingScreen.Library.route) },
                        onNavigateToSettings = onNavigateToSettings,
                        onSwitchToLocalMode = onSwitchToLocalMode
                    )
                }
                
                // Search Screen
                composable(
                    route = StreamingScreen.Search.route,
                    enterTransition = {
                        fadeIn(animationSpec = tween(300)) +
                        slideInVertically(
                            initialOffsetY = { it / 4 },
                            animationSpec = tween(350, easing = EaseInOutQuart)
                        )
                    },
                    exitTransition = {
                        fadeOut(animationSpec = tween(300)) +
                        slideOutVertically(
                            targetOffsetY = { it / 4 },
                            animationSpec = tween(350, easing = EaseInOutQuart)
                        )
                    }
                ) {
                    StreamingSearchScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                
                // Library Screen
                composable(
                    route = StreamingScreen.Library.route,
                    enterTransition = {
                        fadeIn(animationSpec = tween(300)) +
                        slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = tween(350, easing = EaseInOutQuart)
                        )
                    },
                    exitTransition = {
                        fadeOut(animationSpec = tween(300)) +
                        slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = tween(350, easing = EaseInOutQuart)
                        )
                    }
                ) {
                    StreamingLibraryScreen(
                        onNavigateToPlaylist = { playlistId ->
                            navController.navigate(StreamingScreen.PlaylistDetail.createRoute(playlistId))
                        }
                    )
                }
                
                // Playlist Detail (placeholder)
                composable(
                    route = StreamingScreen.PlaylistDetail.route,
                    arguments = listOf(
                        navArgument("playlistId") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val playlistId = backStackEntry.arguments?.getString("playlistId") ?: return@composable
                    // Placeholder for now
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Playlist: $playlistId\n\nComing Soon")
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamingBottomNavBar(
    currentRoute: String,
    navController: NavHostController,
    context: android.content.Context
) {
    val haptic = LocalHapticFeedback.current
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 8.dp,
                bottom = 8.dp,
                start = 16.dp,
                end = 16.dp
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            // Navigation bar Surface - matching local navigation style
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(25.dp),
                tonalElevation = 3.dp,
                modifier = Modifier
                    .height(64.dp)
                    .weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val items = listOf(
                        Triple(
                            StreamingScreen.Home.route, "Home",
                            Pair(Icons.Filled.Home, Icons.Outlined.Home)
                        ),
                        Triple(
                            StreamingScreen.Library.route, "Library",
                            Pair(Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic)
                        )
                    )

                    items.forEachIndexed { index, (route, title, icons) ->
                        val isSelected = currentRoute == route
                        val (selectedIcon, unselectedIcon) = icons

                        // Enhanced animation values with spring physics
                        val animatedScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.05f else 1.0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "scale_$title"
                        )

                        val animatedAlpha by animateFloatAsState(
                            targetValue = if (isSelected) 1f else 0.7f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "alpha_$title"
                        )

                        // Background pill animation with spring
                        val pillWidth by animateDpAsState(
                            targetValue = if (isSelected) 120.dp else 0.dp,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "pillWidth_$title"
                        )
                        
                        // Icon color animation
                        val iconColor by animateColorAsState(
                            targetValue = if (isSelected)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = tween(300),
                            label = "iconColor_$title"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable {
                                    HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.LongPress)
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            // Horizontal layout for icon and text with animated pill background
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .graphicsLayer {
                                        scaleX = animatedScale
                                        scaleY = animatedScale
                                        alpha = animatedAlpha
                                    }
                                    .then(
                                        if (isSelected) Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                            .height(48.dp)
                                            .widthIn(min = pillWidth)
                                            .padding(horizontal = 18.dp)
                                        else Modifier.padding(horizontal = 16.dp)
                                    )
                            ) {
                                // Animated icon with crossfade
                                androidx.compose.animation.Crossfade(
                                    targetState = isSelected,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessVeryLow
                                    ),
                                    label = "iconCrossfade_$title"
                                ) { selected ->
                                    Icon(
                                        imageVector = if (selected) selectedIcon else unselectedIcon,
                                        contentDescription = title,
                                        tint = iconColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                AnimatedVisibility(
                                    visible = isSelected,
                                    enter = fadeIn(
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessMedium
                                        )
                                    ) + expandHorizontally(
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    ),
                                    exit = fadeOut(
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    ) + shrinkHorizontally(
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioNoBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    )
                                ) {
                                    Row {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = iconColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Search button (matching local navigation style)
            Spacer(modifier = Modifier.width(12.dp))
            
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 3.dp,
                modifier = Modifier.size(64.dp)
            ) {
                IconButton(
                    onClick = {
                        HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.LongPress)
                        navController.navigate(StreamingScreen.Search.route)
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

/**
 * Navigation rail for tablets - floating design with vertically centered contents
 */
@Composable
private fun StreamingNavigationRail(
    currentRoute: String,
    navController: NavHostController,
    onNavigateToSettings: () -> Unit,
    context: android.content.Context,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback
) {
    // Calculate rail height based on number of items (4 items * 64dp + padding)
    val railHeight = (4 * 64 + 24).dp
    
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .padding(8.dp),
        contentAlignment = Alignment.Center // Vertically center the rail
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 3.dp,
            modifier = Modifier
                .height(railHeight)
                .width(80.dp)
                .clip(RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top)
            ) {
                val items = listOf(
                    StreamingNavRailItem(
                        route = StreamingScreen.Home.route,
                        title = "Home",
                        selectedIcon = Icons.Filled.Home,
                        unselectedIcon = Icons.Outlined.Home,
                        onClick = {
                            navController.navigate(StreamingScreen.Home.route) {
                                popUpTo(StreamingScreen.Home.route) {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        }
                    ),
                    StreamingNavRailItem(
                        route = StreamingScreen.Library.route,
                        title = "Library",
                        selectedIcon = Icons.Filled.LibraryMusic,
                        unselectedIcon = Icons.Outlined.LibraryMusic,
                        onClick = {
                            navController.navigate(StreamingScreen.Library.route) {
                                popUpTo(StreamingScreen.Home.route)
                                launchSingleTop = true
                            }
                        }
                    ),
                    StreamingNavRailItem(
                        route = StreamingScreen.Search.route,
                        title = "Search",
                        selectedIcon = Icons.Filled.Search,
                        unselectedIcon = Icons.Outlined.Search,
                        onClick = {
                            navController.navigate(StreamingScreen.Search.route)
                        }
                    ),
                    StreamingNavRailItem(
                        route = "settings", // Placeholder route for settings
                        title = "Settings",
                        selectedIcon = Icons.Filled.Settings,
                        unselectedIcon = Icons.Outlined.Settings,
                        onClick = onNavigateToSettings
                    )
                )
                
                items.forEach { item ->
                    StreamingNavigationRailItemWithAnimation(
                        item = item,
                        isSelected = currentRoute == item.route,
                        haptic = haptic,
                        context = context
                    )
                }
            }
        }
    }
}

/**
 * Data class for streaming navigation rail items
 */
private data class StreamingNavRailItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val onClick: () -> Unit
)

/**
 * Streaming navigation rail item with animated selection indicator
 */
@Composable
private fun StreamingNavigationRailItemWithAnimation(
    item: StreamingNavRailItem,
    isSelected: Boolean,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    context: android.content.Context
) {
    // Enhanced animation values with spring physics
    val animatedScale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale_${item.title}"
    )

    val animatedAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.7f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "alpha_${item.title}"
    )

    // Icon color animation
    val iconColor by animateColorAsState(
        targetValue = if (isSelected)
            MaterialTheme.colorScheme.onPrimaryContainer
        else
            MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(300),
        label = "iconColor_${item.title}"
    )

    // Indicator pill animation
    val indicatorHeight by animateDpAsState(
        targetValue = if (isSelected) 56.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "indicatorHeight_${item.title}"
    )

    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                HapticUtils.performHapticFeedback(context, haptic, HapticFeedbackType.LongPress)
                item.onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
                    alpha = animatedAlpha
                }
                .then(
                    if (isSelected) Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .size(64.dp, indicatorHeight.coerceAtLeast(0.dp))
                        .padding(vertical = 8.dp)
                    else Modifier.padding(8.dp)
                )
        ) {
            // Animated icon with crossfade
            androidx.compose.animation.Crossfade(
                targetState = isSelected,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessVeryLow
                ),
                label = "iconCrossfade_${item.title}"
            ) { selected ->
                Icon(
                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                    contentDescription = item.title,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ),
                exit = fadeOut(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + shrinkVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelSmall,
                    color = iconColor
                )
            }
        }
    }
}
