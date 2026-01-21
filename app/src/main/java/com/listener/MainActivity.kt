package com.listener

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.listener.presentation.MainScreen
import com.listener.presentation.components.LoadingState
import com.listener.presentation.theme.ListenerTheme
import com.listener.service.RechunkOnStartupManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var rechunkOnStartupManager: RechunkOnStartupManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ListenerTheme {
                val isRechunking by rechunkOnStartupManager.isRechunking.collectAsStateWithLifecycle()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isRechunking) {
                        LoadingState(message = "청크 설정 적용 중...")
                    } else {
                        MainScreen()
                    }
                }
            }
        }
    }
}
