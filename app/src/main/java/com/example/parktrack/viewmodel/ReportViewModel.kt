package com.example.parktrack.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parktrack.data.model.DriverReport
import com.example.parktrack.data.model.ParkingReport
import com.example.parktrack.data.model.ReportType
import com.example.parktrack.data.model.User
import com.example.parktrack.data.model.UserRole
import com.example.parktrack.data.repository.ReportRepository
import com.example.parktrack.data.repository.UserRepository
import com.example.parktrack.utils.PdfGenerator
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

sealed class ReportListState {
    data object Loading : ReportListState()
    data class Success(val reports: List<Any>) : ReportListState()
    data class Error(val message: String) : ReportListState()
}

sealed class PdfGenerationState {
    data object Idle : PdfGenerationState()
    data object Generating : PdfGenerationState()
    data class Success(val uri: Uri) : PdfGenerationState()
    data class Error(val message: String) : PdfGenerationState()
}

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    // Current user state
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // Report lists based on user role
    private val _adminReports = MutableStateFlow<List<ParkingReport>>(emptyList())
    val adminReports: StateFlow<List<ParkingReport>> = _adminReports.asStateFlow()

    private val _driverReports = MutableStateFlow<List<DriverReport>>(emptyList())
    val driverReports: StateFlow<List<DriverReport>> = _driverReports.asStateFlow()

    // Generic report state for UI
    private val _reportListState = MutableStateFlow<ReportListState>(ReportListState.Loading)
    val reportListState: StateFlow<ReportListState> = _reportListState.asStateFlow()

    // Current report being viewed
    private val _currentAdminReport = MutableStateFlow<ParkingReport?>(null)
    val currentAdminReport: StateFlow<ParkingReport?> = _currentAdminReport.asStateFlow()

    private val _currentDriverReport = MutableStateFlow<DriverReport?>(null)
    val currentDriverReport: StateFlow<DriverReport?> = _currentDriverReport.asStateFlow()

    // Loading and error states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _generationSuccess = MutableStateFlow(false)
    val generationSuccess: StateFlow<Boolean> = _generationSuccess.asStateFlow()

    // PDF generation state
    private val _pdfGenerationState = MutableStateFlow<PdfGenerationState>(PdfGenerationState.Idle)
    val pdfGenerationState: StateFlow<PdfGenerationState> = _pdfGenerationState.asStateFlow()

    init {
        loadCurrentUser()
    }

    /**
     * Load the current authenticated user
     */
    private fun loadCurrentUser() {
        viewModelScope.launch {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId != null) {
                val user = userRepository.getUserById(userId)
                if (user != null) {
                    _currentUser.value = user
                    loadReports()
                } else {
                    _errorMessage.value = "Failed to load user data"
                }
            } else {
                _errorMessage.value = "User not authenticated"
            }
        }
    }

    /**
     * Load reports based on current user role
     */
    fun loadReports() {
        val user = _currentUser.value ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            _reportListState.value = ReportListState.Loading
            
            android.util.Log.d("ReportViewModel", "Loading reports for user: ${user.id}, role: ${user.role}")
            
            try {
                when (user.role) {
                    UserRole.ADMIN -> {
                        android.util.Log.d("ReportViewModel", "Fetching admin reports")
                        val result = reportRepository.getAdminReports(limit = 50)
                        if (result.isSuccess) {
                            val reports = result.getOrDefault(emptyList())
                            android.util.Log.d("ReportViewModel", "Loaded ${reports.size} admin reports")
                            _adminReports.value = reports
                            _reportListState.value = ReportListState.Success(reports)
                        } else {
                            val error = result.exceptionOrNull()
                            val errorMsg = error?.message ?: "Failed to load reports"
                            android.util.Log.e("ReportViewModel", "Error loading admin reports", error)
                            _errorMessage.value = errorMsg
                            _reportListState.value = ReportListState.Error(errorMsg)
                        }
                    }
                    UserRole.DRIVER -> {
                        android.util.Log.d("ReportViewModel", "Fetching driver reports for: ${user.id}")
                        val result = reportRepository.getDriverReports(user.id, limit = 50)
                        if (result.isSuccess) {
                            val reports = result.getOrDefault(emptyList())
                            android.util.Log.d("ReportViewModel", "Loaded ${reports.size} driver reports")
                            _driverReports.value = reports
                            _reportListState.value = ReportListState.Success(reports)
                        } else {
                            val error = result.exceptionOrNull()
                            val errorMsg = error?.message ?: "Failed to load reports"
                            android.util.Log.e("ReportViewModel", "Error loading driver reports", error)
                            _errorMessage.value = errorMsg
                            _reportListState.value = ReportListState.Error(errorMsg)
                        }
                    }
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "An error occurred"
                android.util.Log.e("ReportViewModel", "Exception loading reports", e)
                _errorMessage.value = errorMsg
                _reportListState.value = ReportListState.Error(errorMsg)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Generate monthly report - role-aware
     * Admins generate system-wide reports, drivers generate personal reports
     */
    fun generateMonthlyReport(year: Int, month: Int) {
        val user = _currentUser.value
        if (user == null) {
            _errorMessage.value = "User not authenticated"
            return
        }

        viewModelScope.launch {
            _isGenerating.value = true
            _generationSuccess.value = false
            
            android.util.Log.d("ReportViewModel", "Generating report for user: ${user.id}, role: ${user.role}")
            
            try {
                when (user.role) {
                    UserRole.ADMIN -> {
                        android.util.Log.d("ReportViewModel", "Calling generateMonthlyAdminReport")
                        val result = reportRepository.generateMonthlyAdminReport(year, month, user)
                        if (result.isSuccess) {
                            android.util.Log.d("ReportViewModel", "Admin report generated successfully")
                            _currentAdminReport.value = result.getOrNull()
                            _generationSuccess.value = true
                            loadReports()
                        } else {
                            val error = result.exceptionOrNull()
                            android.util.Log.e("ReportViewModel", "Failed to generate admin report", error)
                            _errorMessage.value = error?.message ?: "Failed to generate admin report"
                        }
                    }
                    UserRole.DRIVER -> {
                        android.util.Log.d("ReportViewModel", "Calling generateDriverReport")
                        val result = reportRepository.generateDriverReport(year, month, user)
                        if (result.isSuccess) {
                            android.util.Log.d("ReportViewModel", "Driver report generated successfully")
                            _currentDriverReport.value = result.getOrNull()
                            _generationSuccess.value = true
                            loadReports()
                        } else {
                            val error = result.exceptionOrNull()
                            android.util.Log.e("ReportViewModel", "Failed to generate driver report", error)
                            _errorMessage.value = error?.message ?: "Failed to generate driver report"
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ReportViewModel", "Exception generating report", e)
                _errorMessage.value = e.message ?: "An error occurred"
            } finally {
                _isGenerating.value = false
            }
        }
    }

    /**
     * Generate report for current month
     */
    fun generateCurrentMonthReport() {
        val calendar = Calendar.getInstance()
        generateMonthlyReport(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1)
    }

    /**
     * Delete a report - respects role-based permissions
     */
    fun deleteReport(reportId: String) {
        val user = _currentUser.value
        if (user == null) {
            _errorMessage.value = "User not authenticated"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = reportRepository.deleteReport(reportId, user)
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

    /**
     * Generate PDF for a driver report
     */
    fun generateDriverReportPdf(context: Context, report: DriverReport) {
        viewModelScope.launch {
            _pdfGenerationState.value = PdfGenerationState.Generating
            try {
                val uri = PdfGenerator.generateDriverReportPdf(context, report)
                if (uri != null) {
                    _pdfGenerationState.value = PdfGenerationState.Success(uri)
                } else {
                    _pdfGenerationState.value = PdfGenerationState.Error("Failed to generate PDF")
                }
            } catch (e: Exception) {
                _pdfGenerationState.value = PdfGenerationState.Error(e.message ?: "PDF generation failed")
            }
        }
    }

    /**
     * Generate PDF for an admin report
     */
    fun generateAdminReportPdf(context: Context, report: ParkingReport) {
        viewModelScope.launch {
            _pdfGenerationState.value = PdfGenerationState.Generating
            try {
                val uri = PdfGenerator.generateAdminReportPdf(context, report)
                if (uri != null) {
                    _pdfGenerationState.value = PdfGenerationState.Success(uri)
                } else {
                    _pdfGenerationState.value = PdfGenerationState.Error("Failed to generate PDF")
                }
            } catch (e: Exception) {
                _pdfGenerationState.value = PdfGenerationState.Error(e.message ?: "PDF generation failed")
            }
        }
    }

    /**
     * Share the generated PDF
     */
    fun sharePdf(context: Context, uri: Uri, isDriverReport: Boolean) {
        val subject = if (isDriverReport) {
            "My ParkTrack Parking Report"
        } else {
            "ParkTrack Administrative Report"
        }
        PdfGenerator.sharePdf(context, uri, subject)
    }

    /**
     * Open the generated PDF with default PDF viewer
     */
    fun openPdf(context: Context, uri: Uri) {
        PdfGenerator.openPdf(context, uri)
    }

    /**
     * Clear PDF generation state
     */
    fun clearPdfGenerationState() {
        _pdfGenerationState.value = PdfGenerationState.Idle
    }

    /**
     * Select a report for viewing
     */
    fun selectAdminReport(report: ParkingReport) {
        _currentAdminReport.value = report
    }

    fun selectDriverReport(report: DriverReport) {
        _currentDriverReport.value = report
    }

    fun clearSelectedReport() {
        _currentAdminReport.value = null
        _currentDriverReport.value = null
    }

    /**
     * Utility methods for formatting
     */
    fun getReportTypeLabel(type: ReportType): String {
        return when (type) {
            ReportType.MONTHLY -> "Monthly"
            ReportType.QUARTERLY -> "Quarterly"
            ReportType.ANNUAL -> "Annual"
            ReportType.CUSTOM -> "Custom"
            ReportType.DRIVER_PERSONAL -> "Personal"
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

    /**
     * Check if current user is an admin
     */
    fun isAdmin(): Boolean {
        return _currentUser.value?.role == UserRole.ADMIN
    }

    /**
     * Check if current user is a driver
     */
    fun isDriver(): Boolean {
        return _currentUser.value?.role == UserRole.DRIVER
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearGenerationSuccess() {
        _generationSuccess.value = false
    }
}
