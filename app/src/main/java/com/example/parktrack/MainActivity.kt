package com.example.parktrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.parktrack.ui.navigation.ParkTrackNavHost
import com.example.parktrack.ui.theme.ParkTrackTheme
import com.example.parktrack.viewmodel.AuthViewModel
import com.example.parktrack.viewmodel.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()
    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isDarkMode by themeViewModel.isDarkMode.collectAsState()

            ParkTrackTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ParkTrackNavHost(
                        authViewModel = authViewModel,
                        themeViewModel = themeViewModel
                    )
                }
            }
        }
    }
}