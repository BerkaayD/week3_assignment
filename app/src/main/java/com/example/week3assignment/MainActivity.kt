package com.example.week3assignment

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.week3assignment.ui.HardwareScreen
import com.example.week3assignment.ui.HardwareViewModel
import com.example.week3assignment.ui.HardwareViewModelFactory

class MainActivity : ComponentActivity() {
    private val viewModel: HardwareViewModel by viewModels { HardwareViewModelFactory() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF176B52),
                    primaryContainer = Color(0xFFD6F1E4),
                    onPrimaryContainer = Color(0xFF103D2F),
                    secondaryContainer = Color(0xFFD6E8DE),
                    onSecondaryContainer = Color(0xFF203F30),
                    surfaceContainerHighest = Color(0xFFE7EDE8),
                    onSurfaceVariant = Color(0xFF44564B),
                    background = Color(0xFFF6F8F5),
                    surface = Color(0xFFF6F8F5),
                ),
            ) {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                HardwareScreen(
                    uiState = uiState,
                    onSearchQueryChanged = viewModel::onSearchQueryChanged,
                    onRoomSelected = viewModel::onRoomSelected,
                    onDeviceToggle = viewModel::onDeviceToggle,
                )
            }
        }
    }
}
