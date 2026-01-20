package chromahub.rhythm.app.shared.presentation.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOutQuart
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import chromahub.rhythm.app.core.domain.model.AppMode
import chromahub.rhythm.app.features.local.presentation.navigation.LocalNavigation
import chromahub.rhythm.app.features.streaming.presentation.navigation.StreamingNavigation
import chromahub.rhythm.app.shared.data.repository.UserPreferencesRepository
import chromahub.rhythm.app.shared.presentation.viewmodel.AppModeViewModel
import chromahub.rhythm.app.features.local.presentation.viewmodel.MusicViewModel
import chromahub.rhythm.app.shared.presentation.viewmodel.ThemeViewModel
import chromahub.rhythm.app.shared.data.model.AppSettings
import androidx.compose.ui.platform.LocalContext

/**
 * Main navigation composable that handles switching between Local and Streaming modes.
 * Acts as the root navigation for the entire app.
 */
@Composable
fun RhythmNavigation(
    musicViewModel: MusicViewModel = viewModel(),
    themeViewModel: ThemeViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appSettings = remember { AppSettings.getInstance(context) }
    val appMode by appSettings.appMode.collectAsState()
    
    // Create a NavHostController that can be passed to both local and streaming navigation
    val rootNavController = rememberNavController()
    
    // Settings navigation callback that works for both modes
    val navigateToSettings: () -> Unit = {
        rootNavController.navigate("settings")
    }
    
    // Switch between Local and Streaming navigation based on app mode with animated transitions
    NavHost(
        navController = rootNavController,
        startDestination = "main",
        modifier = modifier.fillMaxSize()
    ) {
        composable("main") {
            AnimatedContent(
                targetState = appMode,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    fadeIn(
                        animationSpec = tween(400)
                    ) + slideInHorizontally(
                        initialOffsetX = { if (targetState == "STREAMING") it else -it },
                        animationSpec = tween(500, easing = EaseInOutQuart)
                    ) togetherWith fadeOut(
                        animationSpec = tween(400)
                    ) + slideOutHorizontally(
                        targetOffsetX = { if (targetState == "STREAMING") -it else it },
                        animationSpec = tween(500, easing = EaseInOutQuart)
                    )
                },
                label = "appModeTransition"
            ) { mode ->
                when (mode) {
                    "STREAMING" -> {
                        // Show streaming navigation with proper bottom nav
                        StreamingNavigation(
                            onNavigateToSettings = navigateToSettings,
                            onNavigateToPlayer = { /* TODO */ },
                            onSwitchToLocalMode = { 
                                // Switch back to local mode
                                appSettings.setAppMode("LOCAL")
                            }
                        )
                    }
                    else -> {
                        // Use LocalNavigation for local mode (default)
                        LocalNavigation(
                            viewModel = musicViewModel,
                            themeViewModel = themeViewModel,
                            appSettings = appSettings
                        )
                    }
                }
            }
        }
        
        // Settings screen (shared between both modes)
        composable("settings") {
            chromahub.rhythm.app.features.local.presentation.screens.settings.SettingsScreenWrapper(
                onBack = {
                    rootNavController.popBackStack()
                },
                appSettings = appSettings,
                navController = rootNavController
            )
        }
    }
}

/**
 * Mode selector screen that allows users to switch between Local and Streaming modes.
 * Can be shown as a full screen or as a bottom sheet.
 */
@Composable
fun ModeSelectorScreen(
    currentMode: AppMode,
    onModeSelected: (AppMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Choose Your Experience",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Switch between local music and streaming services",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Local Mode Card
            ModeCard(
                title = "Local",
                description = "Play music stored on your device",
                icon = if (currentMode == AppMode.LOCAL) Icons.Filled.MusicNote else Icons.Outlined.MusicNote,
                isSelected = currentMode == AppMode.LOCAL,
                onClick = { onModeSelected(AppMode.LOCAL) },
                modifier = Modifier.weight(1f)
            )
            
            // Streaming Mode Card
            ModeCard(
                title = "Streaming",
                description = "Stream from Spotify, Apple Music & more",
                icon = if (currentMode == AppMode.STREAMING) Icons.Filled.CloudQueue else Icons.Outlined.CloudQueue,
                isSelected = currentMode == AppMode.STREAMING,
                onClick = { onModeSelected(AppMode.STREAMING) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Card component for displaying a mode option in the mode selector.
 */
@Composable
private fun ModeCard(
    title: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surface
                },
                modifier = Modifier.size(64.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        modifier = Modifier.size(32.dp),
                        tint = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Selection indicator
            AnimatedVisibility(
                visible = isSelected,
                enter = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "Active",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Compact mode switcher that can be placed in a navigation bar or header.
 */
@Composable
fun CompactModeSwitcher(
    currentMode: AppMode,
    onModeToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onModeToggle),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Local indicator
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (currentMode == AppMode.LOCAL) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.padding(2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MusicNote,
                        contentDescription = "Local",
                        modifier = Modifier.size(16.dp),
                        tint = if (currentMode == AppMode.LOCAL) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    if (currentMode == AppMode.LOCAL) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Local",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
            
            // Streaming indicator
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (currentMode == AppMode.STREAMING) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.padding(2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CloudQueue,
                        contentDescription = "Streaming",
                        modifier = Modifier.size(16.dp),
                        tint = if (currentMode == AppMode.STREAMING) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    if (currentMode == AppMode.STREAMING) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Stream",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}
