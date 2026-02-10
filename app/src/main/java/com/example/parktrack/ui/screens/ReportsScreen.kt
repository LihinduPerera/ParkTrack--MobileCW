package com.example.parktrack.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.parktrack.data.model.DriverReport
import com.example.parktrack.data.model.ParkingReport
import com.example.parktrack.ui.components.ErrorDialog
import com.example.parktrack.viewmodel.PdfGenerationState
import com.example.parktrack.viewmodel.ReportListState
import com.example.parktrack.viewmodel.ReportViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onBackClick: () -> Unit,
    viewModel: ReportViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val reportListState by viewModel.reportListState.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val generationSuccess by viewModel.generationSuccess.collectAsStateWithLifecycle()
    val pdfGenerationState by viewModel.pdfGenerationState.collectAsStateWithLifecycle()
    val isAdmin by remember { derivedStateOf { viewModel.isAdmin() } }

    var showGenerateDialog by remember { mutableStateOf(false) }
    var showAdminReportDetail by remember { mutableStateOf(false) }
    var showDriverReportDetail by remember { mutableStateOf(false) }
    var selectedAdminReport by remember { mutableStateOf<ParkingReport?>(null) }
    var selectedDriverReport by remember { mutableStateOf<DriverReport?>(null) }

    // Handle PDF generation success
    LaunchedEffect(pdfGenerationState) {
        when (val state = pdfGenerationState) {
            is PdfGenerationState.Success -> {
                // PDF generated successfully, optionally auto-share
                viewModel.sharePdf(context, state.uri, !isAdmin)
                viewModel.clearPdfGenerationState()
            }
            is PdfGenerationState.Error -> {
                // Error handled in UI
            }
            else -> {}
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadReports()
    }

    LaunchedEffect(generationSuccess) {
        if (generationSuccess) {
            showGenerateDialog = false
            viewModel.clearGenerationSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(if (isAdmin) "Admin Reports" else "My Reports") 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showGenerateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Generate Report")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = reportListState) {
                is ReportListState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is ReportListState.Error -> {
                    ErrorReportsView(
                        message = state.message,
                        onRetry = { viewModel.loadReports() }
                    )
                }
                is ReportListState.Success -> {
                    if (state.reports.isEmpty()) {
                        EmptyReportsView(
                            isAdmin = isAdmin,
                            onGenerateClick = { showGenerateDialog = true }
                        )
                    } else {
                        ReportsList(
                            reports = state.reports,
                            isAdmin = isAdmin,
                            viewModel = viewModel,
                            onReportClick = { report ->
                                when (report) {
                                    is ParkingReport -> {
                                        selectedAdminReport = report
                                        showAdminReportDetail = true
                                    }
                                    is DriverReport -> {
                                        selectedDriverReport = report
                                        showDriverReportDetail = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Generate Report Dialog
    if (showGenerateDialog) {
        GenerateReportDialog(
            isAdmin = isAdmin,
            isGenerating = isGenerating,
            onDismiss = { showGenerateDialog = false },
            onGenerate = { year, month ->
                viewModel.generateMonthlyReport(year, month)
            }
        )
    }

    // Admin Report Detail Dialog
    if (showAdminReportDetail && selectedAdminReport != null) {
        AdminReportDetailDialog(
            report = selectedAdminReport!!,
            viewModel = viewModel,
            onDismiss = { 
                showAdminReportDetail = false
                selectedAdminReport = null
            },
            onDownloadPdf = { report ->
                viewModel.generateAdminReportPdf(context, report)
            }
        )
    }

    // Driver Report Detail Dialog
    if (showDriverReportDetail && selectedDriverReport != null) {
        DriverReportDetailDialog(
            report = selectedDriverReport!!,
            viewModel = viewModel,
            onDismiss = { 
                showDriverReportDetail = false
                selectedDriverReport = null
            },
            onDownloadPdf = { report ->
                viewModel.generateDriverReportPdf(context, report)
            }
        )
    }

    // PDF Generation Progress Dialog
    if (pdfGenerationState is PdfGenerationState.Generating) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Generating PDF") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Please wait while we generate your PDF report...")
                }
            },
            confirmButton = { }
        )
    }

    // Error Dialog
    if (errorMessage != null) {
        ErrorDialog(
            title = "Error",
            message = errorMessage ?: "Unknown error",
            onDismiss = { viewModel.clearError() }
        )
    }
}

@Composable
private fun ReportsList(
    reports: List<Any>,
    isAdmin: Boolean,
    viewModel: ReportViewModel,
    onReportClick: (Any) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = if (isAdmin) "Administrative Reports" else "My Parking Reports",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(reports) { report ->
            when (report) {
                is ParkingReport -> AdminReportCard(
                    report = report,
                    viewModel = viewModel,
                    onClick = { onReportClick(report) }
                )
                is DriverReport -> DriverReportCard(
                    report = report,
                    viewModel = viewModel,
                    onClick = { onReportClick(report) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun EmptyReportsView(
    isAdmin: Boolean,
    onGenerateClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Assessment,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isAdmin) "No Admin Reports Yet" else "No Reports Yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isAdmin) {
                "Generate your first administrative report to view system statistics"
            } else {
                "Generate your first parking report to view your activity"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onGenerateClick) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Generate Report")
        }
    }
}

@Composable
private fun ErrorReportsView(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Failed to Load Reports",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Retry")
        }
    }
}

@Composable
private fun AdminReportCard(
    report: ParkingReport,
    viewModel: ReportViewModel,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val periodText = if (report.periodStart != null && report.periodEnd != null) {
        "${dateFormat.format(report.periodStart.toDate())} - ${dateFormat.format(report.periodEnd.toDate())}"
    } else {
        "${viewModel.getMonthName(report.month)} ${report.year}"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${viewModel.getReportTypeLabel(report.reportType)} Admin Report",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = periodText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "Rs. ${String.format("%.2f", report.totalRevenue)}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Sessions", "${report.totalSessions}")
                StatItem("Vehicles", "${report.numberOfUniqueVehicles}")
                StatItem("Drivers", "${report.numberOfRegisteredDrivers}")
                StatItem("Paid", "${report.paidSessions}")
            }
        }
    }
}

@Composable
private fun DriverReportCard(
    report: DriverReport,
    viewModel: ReportViewModel,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val periodText = if (report.periodStart != null && report.periodEnd != null) {
        "${dateFormat.format(report.periodStart.toDate())} - ${dateFormat.format(report.periodEnd.toDate())}"
    } else {
        "${viewModel.getMonthName(report.month)} ${report.year}"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${viewModel.getReportTypeLabel(report.reportType)} Report",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = periodText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Surface(
                    color = if (report.totalOutstanding > 0) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "Rs. ${String.format("%.2f", report.totalCharges)}",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (report.totalOutstanding > 0) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Sessions", "${report.totalSessions}")
                StatItem("Completed", "${report.completedSessions}")
                StatItem("Paid", "${report.paidSessions}")
                StatItem("Duration", "${report.averageSessionDuration}m")
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun GenerateReportDialog(
    isAdmin: Boolean,
    isGenerating: Boolean,
    onDismiss: () -> Unit,
    onGenerate: (Int, Int) -> Unit
) {
    var selectedYear by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH) + 1) }

    AlertDialog(
        onDismissRequest = { if (!isGenerating) onDismiss() },
        title = { 
            Text(if (isAdmin) "Generate Administrative Report" else "Generate My Report") 
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isAdmin) {
                    Text(
                        text = "This will generate a system-wide report with all parking data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                } else {
                    Text(
                        text = "This will generate a personal report with only your parking data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                // Year Selection
                Text(
                    text = "Year: $selectedYear",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { selectedYear-- },
                        modifier = Modifier.weight(1f),
                        enabled = !isGenerating
                    ) {
                        Text("-")
                    }
                    Button(
                        onClick = { selectedYear++ },
                        modifier = Modifier.weight(1f),
                        enabled = !isGenerating
                    ) {
                        Text("+")
                    }
                }

                // Month Selection
                Text(
                    text = "Month: ${getMonthName(selectedMonth)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { 
                            selectedMonth = if (selectedMonth > 1) selectedMonth - 1 else 12 
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isGenerating
                    ) {
                        Text("Previous")
                    }
                    Button(
                        onClick = { 
                            selectedMonth = if (selectedMonth < 12) selectedMonth + 1 else 1 
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isGenerating
                    ) {
                        Text("Next")
                    }
                }

                if (isGenerating) {
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        text = "Generating report...",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onGenerate(selectedYear, selectedMonth) },
                enabled = !isGenerating
            ) {
                Text("Generate")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isGenerating
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AdminReportDetailDialog(
    report: ParkingReport,
    viewModel: ReportViewModel,
    onDismiss: () -> Unit,
    onDownloadPdf: (ParkingReport) -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("${viewModel.getReportTypeLabel(report.reportType)} Administrative Report")
                Text(
                    text = "${viewModel.getMonthName(report.month)} ${report.year}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Revenue Card
                RevenueCard(report.totalRevenue, report.amountCollected, report.outstandingAmount)

                // Statistics
                SectionTitle("System Statistics")
                DetailRow("Total Sessions", "${report.totalSessions}")
                DetailRow("Unique Vehicles", "${report.numberOfUniqueVehicles}")
                DetailRow("Registered Drivers", "${report.numberOfRegisteredDrivers}")
                DetailRow("Average Duration", "${report.averageSessionDuration} minutes")
                DetailRow("Average Occupancy", "${String.format("%.1f", report.averageOccupancy)}%")

                // Session Breakdown
                SectionTitle("Session Breakdown")
                DetailRow("Paid Sessions", "${report.paidSessions}", Color(0xFF4CAF50))
                DetailRow("Unpaid Sessions", "${report.unpaidSessions}", Color(0xFFFF9800))
                DetailRow("Overdue Sessions", "${report.overdueSessions}", Color(0xFFF44336))

                // Financial Details
                if (report.totalOverdueCharges > 0) {
                    SectionTitle("Overdue Charges")
                    DetailRow("Total Overdue", "Rs. ${String.format("%.2f", report.totalOverdueCharges)}", Color.Red)
                }

                // Period Info
                if (report.periodStart != null && report.periodEnd != null) {
                    SectionTitle("Report Period")
                    Text(
                        text = "From: ${dateFormat.format(report.periodStart.toDate())}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "To: ${dateFormat.format(report.periodEnd.toDate())}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (report.peakHours.isNotEmpty()) {
                    SectionTitle("Peak Hours")
                    Text(
                        text = report.peakHours.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Text(
                    text = "Generated: ${dateFormat.format(report.createdAt.toDate())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = { onDownloadPdf(report) }
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Download PDF")
            }
        }
    )
}

@Composable
private fun DriverReportDetailDialog(
    report: DriverReport,
    viewModel: ReportViewModel,
    onDismiss: () -> Unit,
    onDownloadPdf: (DriverReport) -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("${viewModel.getReportTypeLabel(report.reportType)} Driver Report")
                Text(
                    text = "${viewModel.getMonthName(report.month)} ${report.year}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Driver Info
                SectionTitle("Driver Information")
                DetailRow("Name", report.driverName)
                DetailRow("Vehicle", report.vehicleNumber.ifEmpty { "N/A" })
                DetailRow("Phone", report.driverPhoneNumber.ifEmpty { "N/A" })

                // Financial Summary
                RevenueCard(report.totalCharges, report.totalPaid, report.totalOutstanding)

                // Session Statistics
                SectionTitle("Session Statistics")
                DetailRow("Total Sessions", "${report.totalSessions}")
                DetailRow("Completed", "${report.completedSessions}")
                DetailRow("Active", "${report.activeSessions}")
                DetailRow("Average Duration", "${report.averageSessionDuration} minutes")
                DetailRow("Total Parking Time", formatDuration(report.totalDurationMinutes))

                // Payment Status
                SectionTitle("Payment Status")
                DetailRow("Paid Sessions", "${report.paidSessions}", Color(0xFF4CAF50))
                DetailRow("Unpaid Sessions", "${report.unpaidSessions}", Color(0xFFFF9800))
                DetailRow("Overdue Sessions", "${report.overdueSessions}", Color(0xFFF44336))

                if (report.overdueCharges > 0) {
                    DetailRow("Overdue Charges", "Rs. ${String.format("%.2f", report.overdueCharges)}", Color.Red)
                }

                // Location Info
                if (report.totalVisitsByLocation.isNotEmpty()) {
                    SectionTitle("Parking Locations")
                    DetailRow("Favorite Location", report.favoriteParkingLocation)
                    report.totalVisitsByLocation.forEach { (location, visits) ->
                        DetailRow(location, "$visits visits")
                    }
                }

                if (report.peakParkingDays.isNotEmpty()) {
                    SectionTitle("Peak Parking Days")
                    Text(
                        text = report.peakParkingDays.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Period Info
                if (report.periodStart != null && report.periodEnd != null) {
                    SectionTitle("Report Period")
                    Text(
                        text = "From: ${dateFormat.format(report.periodStart.toDate())}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "To: ${dateFormat.format(report.periodEnd.toDate())}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Text(
                    text = "Generated: ${dateFormat.format(report.createdAt.toDate())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = { onDownloadPdf(report) }
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Download PDF")
            }
        }
    )
}

@Composable
private fun RevenueCard(
    total: Double,
    collected: Double,
    outstanding: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Total Amount",
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = "Rs. ${String.format("%.2f", total)}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Paid: Rs. ${String.format("%.2f", collected)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF4CAF50)
                )
                Text(
                    text = "Outstanding: Rs. ${String.format("%.2f", outstanding)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (outstanding > 0) Color.Red else Color(0xFF4CAF50)
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun DetailRow(label: String, value: String, valueColor: Color = Color.Unspecified) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

private fun getMonthName(month: Int): String {
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

private fun formatDuration(minutes: Long): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return when {
        hours > 0 && mins > 0 -> "${hours}h ${mins}m"
        hours > 0 -> "${hours}h"
        else -> "${mins}m"
    }
}
