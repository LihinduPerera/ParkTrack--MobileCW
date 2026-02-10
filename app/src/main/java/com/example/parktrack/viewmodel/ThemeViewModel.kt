package com.example.parktrack.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parktrack.data.ThemeDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themeDataStore: ThemeDataStore
) : ViewModel() {

    val isDarkMode: StateFlow<Boolean> = themeDataStore.isDarkMode
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            true // Default to dark mode
        )

    fun setDarkMode(isDarkMode: Boolean) {
        viewModelScope.launch {
            themeDataStore.setDarkMode(isDarkMode)
        }
    }
}