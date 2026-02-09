package com.example.parktrack.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.parktrack.data.model.ParkingReport
import com.example.parktrack.ui.components.ErrorDialog
import com.example.parktrack.viewmodel.ReportViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onBackClick: () -> Unit,
    viewModel: ReportViewModel = hiltViewModel()
) {
    val reports by viewModel.reports.collectAsStateWithLifecycle()
    val currentReport by viewModel.currentReport.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val generationSuccess by viewModel.generationSuccess.collectAsStateWithLifecycle()

    var showGenerateDialog by remember { mutableStateOf(false) }
    var showReportDetail by remember { mutableStateOf(false) }
    var selectedReport by remember { mutableStateOf<ParkingReport?>(null) }

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
                title = { Text("Parking Reports") },
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
            if (isLoading && reports.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (reports.isEmpty()) {
                EmptyReportsView(
                    onGenerateClick = { showGenerateDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "Generated Reports",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(reports) { report ->
                        ReportCard(
                            report = report,
                            viewModel = viewModel,
                            onClick = {
                                selectedReport = report
                                showReportDetail = true
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    // Generate Report Dialog
    if (showGenerateDialog) {
        GenerateReportDialog(
            viewModel = viewModel,
            isGenerating = isGenerating,
            onDismiss = { showGenerateDialog = false },
            onGenerate = { year, month ->
                viewModel.generateMonthlyReport(year, month)
            }
        )
    }

    // Report Detail Dialog
    if (showReportDetail && selectedReport != null) {
        ReportDetailDialog(
            report = selectedReport!!,
            viewModel = viewModel,
            onDismiss = { 
                showReportDetail = false
                selectedReport = null
            }
        )
    }

    if (errorMessage != null) {
        ErrorDialog(
            title = "Error",
            message = errorMessage ?: "Unknown error",
            onDismiss = { viewModel.clearError() }
        )
    }
}

@Composable
private fun EmptyReportsView(onGenerateClick: () -> Unit) {
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
            text = "No Reports Yet",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Generate your first parking report to view statistics",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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
private fun ReportCard(
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
                StatItem("Paid", "${report.paidSessions}")
                StatItem("Unpaid", "${report.unpaidSessions}")
                StatItem("Overdue", "${report.overdueSessions}")
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
    viewModel: ReportViewModel,
    isGenerating: Boolean,
    onDismiss: () -> Unit,
    onGenerate: (Int, Int) -> Unit
) {
    var selectedYear by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH) + 1) }

    AlertDialog(
        onDismissRequest = { if (!isGenerating) onDismiss() },
        title = { Text("Generate Monthly Report") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
                    text = "Month: ${viewModel.getMonthName(selectedMonth)}",
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
private fun ReportDetailDialog(
    report: ParkingReport,
    viewModel: ReportViewModel,
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("${viewModel.getReportTypeLabel(report.reportType)} Report")
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
                            text = "Total Revenue",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = "Rs. ${String.format("%.2f", report.totalRevenue)}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Collected: Rs. ${String.format("%.2f", report.amountCollected)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Outstanding: Rs. ${String.format("%.2f", report.outstandingAmount)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (report.outstandingAmount > 0) Color.Red else Color.Unspecified
                            )
                        }
                    }
                }

                // Statistics
                Text(
                    text = "Statistics",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                DetailRow("Total Sessions", "${report.totalSessions}")
                DetailRow("Unique Vehicles", "${report.numberOfUniqueVehicles}")
                DetailRow("Average Duration", "${report.averageSessionDuration} minutes")
                DetailRow("Paid Sessions", "${report.paidSessions}")
                DetailRow("Unpaid Sessions", "${report.unpaidSessions}")
                DetailRow("Overdue Sessions", "${report.overdueSessions}")
                DetailRow("Overdue Charges", "Rs. ${String.format("%.2f", report.totalOverdueCharges)}")

                // Period Info
                if (report.periodStart != null && report.periodEnd != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Report Period",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "From: ${dateFormat.format(report.periodStart.toDate())}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "To: ${dateFormat.format(report.periodEnd.toDate())}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (report.createdAt != null) {
                    Text(
                        text = "Generated: ${dateFormat.format(report.createdAt.toDate())}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
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
            fontWeight = FontWeight.Medium
        )
    }
}
