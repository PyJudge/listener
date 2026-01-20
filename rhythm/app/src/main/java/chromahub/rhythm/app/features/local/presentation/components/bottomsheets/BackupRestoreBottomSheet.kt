@file:OptIn(ExperimentalMaterial3Api::class)
package chromahub.rhythm.app.features.local.presentation.components.bottomsheets

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import chromahub.rhythm.app.R
import chromahub.rhythm.app.shared.data.model.AppSettings
import chromahub.rhythm.app.util.HapticUtils
import chromahub.rhythm.app.features.local.presentation.viewmodel.MusicViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupRestoreBottomSheet(
    onDismiss: () -> Unit,
    appSettings: AppSettings
) {
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val musicViewModel: MusicViewModel = viewModel()
    
    // Collect states
    val autoBackupEnabled by appSettings.autoBackupEnabled.collectAsState()
    val lastBackupTimestamp by appSettings.lastBackupTimestamp.collectAsState()
    val backupLocation by appSettings.backupLocation.collectAsState()
    
    // Local states
    var isCreatingBackup by remember { mutableStateOf(false) }
    var isRestoringBackup by remember { mutableStateOf(false) }
    var isRestoringFromFile by remember { mutableStateOf(false) }
    var showBackupSuccess by remember { mutableStateOf(false) }
    var showRestoreSuccess by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // File picker launcher for restore functionality
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                scope.launch {
                    try {
                        isRestoringFromFile = true
                        HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                        
                        // Read the backup file content
                        val inputStream = context.contentResolver.openInputStream(uri)
                        val backupJson = inputStream?.bufferedReader()?.use { it.readText() }
                        
                        if (!backupJson.isNullOrEmpty()) {
                            if (appSettings.restoreFromBackup(backupJson)) {
                                // Reload playlists from restored settings
                                musicViewModel.reloadPlaylistsFromSettings()
                                showRestoreSuccess = true
                            } else {
                                errorMessage = "Invalid backup format or corrupted data"
                                showError = true
                            }
                        } else {
                            errorMessage = "Unable to read the backup file"
                            showError = true
                        }
                    } catch (e: Exception) {
                        errorMessage = "Failed to restore from file: ${e.message}"
                        showError = true
                    } finally {
                        isRestoringFromFile = false
                        isRestoringBackup = false
                    }
                }
            }
        } else {
            isRestoringFromFile = false
            isRestoringBackup = false
        }
    }

    // Backup location selector launcher
    val backupLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                scope.launch {
                    try {
                        isCreatingBackup = true
                        HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.LongPress)
                        
                        // Ensure current playlists are saved before backup
                        musicViewModel.ensurePlaylistsSaved()
                        
                        val backupJson = appSettings.createBackup()
                        // Get the filename from the URI or create a default one
                        val fileName = "rhythm_backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.json"
                        
                        // Write to the selected location
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(backupJson.toByteArray())
                            outputStream.flush()
                        }
                        
                        appSettings.setLastBackupTimestamp(System.currentTimeMillis())
                        appSettings.setBackupLocation(uri.toString())
                        
                        showBackupSuccess = true
                        
                        // Also copy to clipboard for easy sharing
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Rhythm Backup", backupJson)
                        clipboard.setPrimaryClip(clip)
                        
                    } catch (e: Exception) {
                        errorMessage = "Failed to create backup: ${e.message}"
                        showError = true
                    } finally {
                        isCreatingBackup = false
                    }
                }
            }
        } else {
            isCreatingBackup = false
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
            onDismiss()
        },
        sheetState = bottomSheetState,
        dragHandle = { 
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.primary
            )
        },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Backup,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = context.getString(R.string.backup_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = context.getString(R.string.backup_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Main content
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Backup Status
                // item {
                //     Row(
                //         modifier = Modifier
                //             .fillMaxWidth()
                //             .padding(horizontal = 0.dp, vertical = 8.dp),
                //         verticalAlignment = Alignment.CenterVertically
                //     ) {
                //         Text(
                //             text = "BACKUP STATUS",
                //             style = MaterialTheme.typography.titleSmall,
                //             fontWeight = FontWeight.Bold,
                //             color = MaterialTheme.colorScheme.primary
                //         )

                //         Spacer(modifier = Modifier.width(8.dp))

                //         androidx.compose.material3.HorizontalDivider(
                //             modifier = Modifier
                //                 .weight(1f)
                //                 .padding(horizontal = 8.dp),
                //             thickness = 1.dp,
                //             color = MaterialTheme.colorScheme.outlineVariant
                //         )
                //     }
                // }

                // Backup Status Cards Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Backup Status Card
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = if (lastBackupTimestamp > 0) 
                                    MaterialTheme.colorScheme.primaryContainer 
                                else 
                                    MaterialTheme.colorScheme.errorContainer
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = if (lastBackupTimestamp > 0) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = if (lastBackupTimestamp > 0) 
                                        MaterialTheme.colorScheme.onPrimaryContainer 
                                    else 
                                        MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (lastBackupTimestamp > 0) {
                                        val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
                                        sdf.format(Date(lastBackupTimestamp))
                                    } else "Never",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (lastBackupTimestamp > 0) 
                                        MaterialTheme.colorScheme.onPrimaryContainer 
                                    else 
                                        MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = context.getString(R.string.last_backup),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (lastBackupTimestamp > 0) 
                                        MaterialTheme.colorScheme.onPrimaryContainer 
                                    else 
                                        MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                        
                        // Auto Backup Status Card
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = if (autoBackupEnabled) 
                                    MaterialTheme.colorScheme.tertiaryContainer 
                                else 
                                    MaterialTheme.colorScheme.surfaceVariant
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = if (autoBackupEnabled) Icons.Filled.Autorenew else Icons.Filled.Schedule,
                                    contentDescription = null,
                                    tint = if (autoBackupEnabled) 
                                        MaterialTheme.colorScheme.onTertiaryContainer 
                                    else 
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (autoBackupEnabled) "Enabled" else "Manual",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (autoBackupEnabled) 
                                        MaterialTheme.colorScheme.onTertiaryContainer 
                                    else 
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = context.getString(R.string.auto_backup),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (autoBackupEnabled) 
                                        MaterialTheme.colorScheme.onTertiaryContainer 
                                    else 
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    
                    // Backup location info if available
                    backupLocation?.let { location ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = location.substringAfterLast("/"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Backup Management
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 0.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = context.getString(R.string.manage_backup),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        androidx.compose.material3.HorizontalDivider(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }

                // Auto-backup toggle
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                appSettings.triggerImmediateBackup()
                                appSettings.setAutoBackupEnabled(!autoBackupEnabled)
                                HapticUtils.performHapticFeedback(context, haptics, androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Autorenew,
                                contentDescription = null,
                                tint = if (autoBackupEnabled) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = context.getString(R.string.auto_backup),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = context.getString(R.string.backup_auto_weekly),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Switch(
                                checked = autoBackupEnabled,
                                onCheckedChange = {
                                    appSettings.setAutoBackupEnabled(it)
                                    HapticUtils.performHapticFeedback(context, haptics, androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                }
                
                // Test auto-backup button (only shown when auto-backup is enabled)
//                if (autoBackupEnabled) {
//                    item {
//                        Card(
//                            shape = RoundedCornerShape(16.dp),
//                            colors = CardDefaults.cardColors(
//                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
//                            ),
//                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .clickable {
//                                    HapticUtils.performHapticFeedback(context, haptics, androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
//                                    appSettings.triggerImmediateBackup()
//                                    scope.launch {
//                                        kotlinx.coroutines.delay(500)
//                                        android.widget.Toast.makeText(
//                                            context,
//                                            "Auto-backup triggered! Check status card in a few seconds.",
//                                            android.widget.Toast.LENGTH_LONG
//                                        ).show()
//                                    }
//                                }
//                        ) {
//                            Row(
//                                verticalAlignment = Alignment.CenterVertically,
//                                modifier = Modifier.padding(16.dp)
//                            ) {
//                                Icon(
//                                    imageVector = Icons.Filled.PlayArrow,
//                                    contentDescription = null,
//                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
//                                    modifier = Modifier.size(24.dp)
//                                )
//
//                                Spacer(modifier = Modifier.width(16.dp))
//
//                                Column(modifier = Modifier.weight(1f)) {
//                                    Text(
//                                        text = "Test Auto-Backup Now",
//                                        style = MaterialTheme.typography.bodyMedium,
//                                        fontWeight = FontWeight.Medium,
//                                        color = MaterialTheme.colorScheme.onTertiaryContainer
//                                    )
//                                    Text(
//                                        text = "Trigger an immediate background backup to test",
//                                        style = MaterialTheme.typography.bodySmall,
//                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
//                                    )
//                                }
//
//                                Icon(
//                                    imageVector = Icons.Filled.ChevronRight,
//                                    contentDescription = null,
//                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
//                                    modifier = Modifier.size(20.dp)
//                                )
//                            }
//                        }
//                    }
//                }


                // Create Backup Button
                item {
                    // Create Backup to Custom Location Button
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isCreatingBackup && !isRestoringBackup) {
                                HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                
                                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                                    addCategory(Intent.CATEGORY_OPENABLE)
                                    type = "application/json"
                                    val fileName = "rhythm_backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.json"
                                    putExtra(Intent.EXTRA_TITLE, fileName)
                                }
                                backupLocationLauncher.launch(intent)
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(20.dp)
                        ) {
                            if (isCreatingBackup) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Backup,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isCreatingBackup) "Creating backup..." else "Create Backup",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = context.getString(R.string.backup_choose_save_location),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Icon(
                                imageVector = Icons.Filled.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Restore Backup
                item {
                    // Section header with UP NEXT style
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 0.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = context.getString(R.string.restore_backup),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        androidx.compose.material3.HorizontalDivider(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }

                // Restore Backup Buttons Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Restore from Clipboard Button
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = !isCreatingBackup && !isRestoringBackup && !isRestoringFromFile) {
                                    scope.launch {
                                        try {
                                            isRestoringBackup = true
                                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                            
                                            // Get backup from clipboard
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = clipboard.primaryClip
                                            if (clip != null && clip.itemCount > 0) {
                                                val backupJson = clip.getItemAt(0).text.toString()
                                                
                                                if (appSettings.restoreFromBackup(backupJson)) {
                                                    // Reload playlists from restored settings
                                                    musicViewModel.reloadPlaylistsFromSettings()
                                                    showRestoreSuccess = true
                                                } else {
                                                    errorMessage = "Invalid backup format or corrupted data"
                                                    showError = true
                                                }
                                            } else {
                                                errorMessage = "No backup data found in clipboard. Please copy a backup first."
                                                showError = true
                                            }
                                        } catch (e: Exception) {
                                            errorMessage = "Failed to restore backup: ${e.message}"
                                            showError = true
                                        } finally {
                                            isRestoringBackup = false
                                        }
                                    }
                                }
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                if (isRestoringBackup && !isRestoringFromFile) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.ContentCopy,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = if (isRestoringBackup && !isRestoringFromFile) "Restoring..." else "From Clipboard",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = context.getString(R.string.backup_paste_data),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        
                        // Restore from File Button
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable(enabled = !isCreatingBackup && !isRestoringBackup && !isRestoringFromFile) {
                                    isRestoringFromFile = true
                                    isRestoringBackup = true
                                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                                    
                                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                        type = "application/json"
                                        // Also accept all files in case JSON MIME type isn't recognized
                                        putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/json", "text/plain", "*/*"))
                                        
                                        // Try to open in the backup directory by default
                                        try {
                                            val documentsDir = android.os.Environment.getExternalStoragePublicDirectory(
                                                android.os.Environment.DIRECTORY_DOCUMENTS
                                            )
                                            val backupDir = java.io.File(documentsDir, "RhythmBackups")
                                            if (backupDir.exists()) {
                                                val uri = android.net.Uri.fromFile(backupDir)
                                                putExtra(android.provider.DocumentsContract.EXTRA_INITIAL_URI, uri)
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("BackupRestore", "Failed to set initial directory", e)
                                        }
                                    }
                                    filePickerLauncher.launch(intent)
                                }
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                if (isRestoringFromFile) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Filled.FolderOpen,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = if (isRestoringFromFile) "Opening..." else "From File",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = context.getString(R.string.backup_select_file),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                
                // Spacer between restore buttons and tip card
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Tips/Information Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Lightbulb,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = context.getString(R.string.backup_whats_included),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            BackupInfoItem(
                                icon = Icons.Filled.Save,
                                text = context.getString(R.string.backup_included_1)
                            )
                            BackupInfoItem(
                                icon = Icons.Filled.RestoreFromTrash,
                                text = context.getString(R.string.backup_included_2)
                            )
                            BackupInfoItem(
                                icon = Icons.Filled.Security,
                                text = context.getString(R.string.backup_included_3)
                            )
                        }
                    }
                }
                
                // Close button
//                item {
//                    Spacer(modifier = Modifier.height(16.dp))
//
//                    FilledTonalButton(
//                        onClick = {
//                            HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
//                            onDismiss()
//                        },
//                        colors = ButtonDefaults.filledTonalButtonColors(
//                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
//                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
//                        ),
//                        modifier = Modifier.fillMaxWidth(),
//                        shape = RoundedCornerShape(16.dp)
//                    ) {
//                        Icon(
//                            imageVector = Icons.Filled.Close,
//                            contentDescription = null,
//                            modifier = Modifier.size(18.dp)
//                        )
//                        Spacer(modifier = Modifier.width(8.dp))
//                        Text(
//                            text = "Close",
//                            fontWeight = FontWeight.Medium
//                        )
//                    }
//
//                    Spacer(modifier = Modifier.height(16.dp))
//                }
            }
        }
    }

    // Success/Error Dialogs
    if (showBackupSuccess) {
        AlertDialog(
            onDismissRequest = { showBackupSuccess = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Backup Created Successfully") },
            text = { 
                Text("Your complete Rhythm backup has been created including:\n\n" +
                     "• All app settings and preferences\n" +
                     "• Your playlists and favorite songs\n" +
                     "• Blacklisted/whitelisted songs and folders\n" +
                     "• Pinned folders and library customization\n" +
                     "• Theme settings (colors, fonts, album art colors)\n" +
                     "• Audio preferences and API settings\n" +
                     "• Recently played history and statistics\n\n" +
                     "The backup has been saved and copied to your clipboard for easy sharing.")
            },
            confirmButton = {
                Button(onClick = { 
                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                    showBackupSuccess = false 
                }) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("OK")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showRestoreSuccess) {
        AlertDialog(
            onDismissRequest = { showRestoreSuccess = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Restore Completed Successfully") },
            text = { 
                Text("Your Rhythm data has been restored successfully including:\n\n" +
                     "• All app settings and preferences\n" +
                     "• Your playlists and favorite songs\n" +
                     "• Blacklisted songs and folders\n" +
                     "• Theme and audio preferences\n\n" +
                     "Please restart the app for all changes to take full effect.")
            },
            confirmButton = {
                Button(onClick = { 
                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                    showRestoreSuccess = false
                    
                    // Restart the app
                    val packageManager = context.packageManager
                    val intent = packageManager.getLaunchIntentForPackage(context.packageName)
                    val componentName = intent?.component
                    val mainIntent = Intent.makeRestartActivityTask(componentName)
                    context.startActivity(mainIntent)
                    (context as? Activity)?.finish()
                    Runtime.getRuntime().exit(0)
                }) {
                    Icon(
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restart Now")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    if (showError) {
        AlertDialog(
            onDismissRequest = { showError = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Error") },
            text = { Text(errorMessage) },
            confirmButton = {
                Button(onClick = { 
                    HapticUtils.performHapticFeedback(context, haptics, HapticFeedbackType.TextHandleMove)
                    showError = false 
                }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("OK")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun BackupInfoItem(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun InfoItem(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CompactInfoItem(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
    }
}
