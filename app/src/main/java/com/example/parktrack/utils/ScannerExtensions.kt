package com.example.parktrack.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parktrack.viewmodel.AdminScannerViewModel
import com.example.parktrack.viewmodel.ScanState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Extension function to add vibration feedback to scanner view model
 */
fun AdminScannerViewModel.addVibrationFeedback(onVibrate: (type: String) -> Unit) {
    (this as ViewModel).viewModelScope.launch {
        scanState.collectLatest { state ->
            when (state) {
                ScanState.SUCCESS -> onVibrate("success")
                ScanState.ERROR -> onVibrate("error")
                ScanState.SCANNING -> onVibrate("scan")
                else -> {} // No vibration for other states
            }
        }
    }
}
