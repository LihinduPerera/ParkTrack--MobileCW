package com.example.parktrack.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parktrack.data.model.ParkingReport
import com.example.parktrack.data.model.ReportType
import com.example.parktrack.data.repository.ReportRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val reportRepository: ReportRepository
) : ViewModel() {

    private val _reports = MutableStateFlow<List<ParkingReport>>(emptyList())
    val reports: StateFlow<List<ParkingReport>> = _reports.asStateFlow()

    private val _currentReport = MutableStateFlow<ParkingReport?>(null)
    val currentReport: StateFlow<ParkingReport?> = _currentReport.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _generationSuccess = MutableStateFlow(false)
    val generationSuccess: StateFlow<Boolean> = _generationSuccess.asStateFlow()

    init {
        loadReports()
    }

    fun loadReports() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = reportRepository.getAllReports(limit = 50)
                if (result.isSuccess) {
                    _reports.value = result.getOrDefault(emptyList())
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to load reports"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun generateMonthlyReport(year: Int, month: Int) {
        viewModelScope.launch {
            _isGenerating.value = true
            _generationSuccess.value = false
            try {
                val adminId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                if (adminId.isEmpty()) {
                    _errorMessage.value = "Admin not authenticated"
                    return@launch
                }

                val result = reportRepository.generateMonthlyReport(year, month, adminId)
                if (result.isSuccess) {
                    _currentReport.value = result.getOrNull()
                    _generationSuccess.value = true
                    // Reload reports to include the new one
                    loadReports()
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to generate report"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An error occurred"
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun generateCurrentMonthReport() {
        val calendar = Calendar.getInstance()
        generateMonthlyReport(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)
    }

    fun deleteReport(reportId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = reportRepository.deleteReport(reportId)
                if (result.isSuccess) {
                    loadReports()
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to delete report"
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "An error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearGenerationSuccess() {
        _generationSuccess.value = false
    }

    fun getReportTypeLabel(type: ReportType): String {
        return when (type) {
            ReportType.MONTHLY -> "Monthly"
            ReportType.QUARTERLY -> "Quarterly"
            ReportType.ANNUAL -> "Annual"
            ReportType.CUSTOM -> "Custom"
        }
    }

    fun getMonthName(month: Int): String {
        return when (month) {
            1 -> "January"
            2 -> "February"
            3 -> "March"
            4 -> "April"
            5 -> "May"
            6 -> "June"
            7 -> "July"
            8 -> "August"
            9 -> "September"
            10 -> "October"
            11 -> "November"
            12 -> "December"
            else -> "Unknown"
        }
    }
}
