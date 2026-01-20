package chromahub.rhythm.app.features.local.presentation.screens.onboarding

enum class OnboardingStep {
    WELCOME,
    PERMISSIONS,
    NOTIFICATIONS, // Step for notification preferences setup
    BACKUP_RESTORE, // Step for backup and restore setup
    AUDIO_PLAYBACK, // Step for audio and playback settings
    THEMING,
    GESTURES, // Step for gesture controls introduction
    LIBRARY_SETUP, // Step for library organization preferences
    MEDIA_SCAN, // Step for choosing blacklist/whitelist filtering mode
    WIDGETS, // Step for home screen widget setup
    INTEGRATIONS, // Step for API services, scrobbling, Discord presence
    RHYTHM_STATS, // Step for listening statistics introduction
    UPDATER,
    SETUP_FINISHED, // Step showing setup completion with finish button
    COMPLETE
}

sealed class PermissionScreenState {
    object Loading : PermissionScreenState()
    object PermissionsRequired : PermissionScreenState()
    object ShowRationale : PermissionScreenState()
    object RedirectToSettings : PermissionScreenState()
    object PermissionsGranted : PermissionScreenState()
}
