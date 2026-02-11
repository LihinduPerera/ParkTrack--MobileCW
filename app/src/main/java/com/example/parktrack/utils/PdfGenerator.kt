package com.example.parktrack.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.compose.ui.graphics.Color
import androidx.core.content.FileProvider
import com.example.parktrack.data.model.DriverReport
import com.example.parktrack.data.model.ParkingReport
import com.example.parktrack.data.model.ReportType
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.HorizontalAlignment
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Utility class for generating PDF reports using iTextPDF
 */
object PdfGenerator {

    private val PRIMARY_COLOR = DeviceRgb(0, 123, 255)
    private val SECONDARY_COLOR = DeviceRgb(108, 117, 125)
    private val SUCCESS_COLOR = DeviceRgb(40, 167, 69)
    private val DANGER_COLOR = DeviceRgb(220, 53, 69)
    private val WARNING_COLOR = DeviceRgb(255, 193, 7)

    /**
     * Generate PDF for a driver report and return the file URI
     */
    fun generateDriverReportPdf(context: Context, report: DriverReport): Uri? {
        return try {
            val fileName = "Driver_Report_${report.driverName.replace(" ", "_")}_${report.year}_${getMonthName(report.month)}.pdf"
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
            
            val pdfWriter = PdfWriter(file)
            val pdfDocument = PdfDocument(pdfWriter)
            val document = Document(pdfDocument, PageSize.A4)
            document.setMargins(36f, 36f, 36f, 36f)

            // Header
            addHeader(document, "PARKTRACK - DRIVER REPORT", PRIMARY_COLOR)
            addReportMetadata(document, report)
            
            document.add(Paragraph("").setMarginBottom(20f))

            // Driver Information Section
            addSectionTitle(document, "Driver Information", PRIMARY_COLOR)
            addDriverInfoTable(document, report)
            
            document.add(Paragraph("").setMarginBottom(20f))

            // Session Statistics Section
            addSectionTitle(document, "Parking Session Statistics", PRIMARY_COLOR)
            addSessionStatsTable(document, report)
            
            document.add(Paragraph("").setMarginBottom(20f))

            // Financial Summary Section
            addSectionTitle(document, "Financial Summary", PRIMARY_COLOR)
            addFinancialSummaryTable(document, report)
            
            document.add(Paragraph("").setMarginBottom(20f))

            // Location Statistics
            if (report.totalVisitsByLocation.isNotEmpty()) {
                addSectionTitle(document, "Parking Locations", PRIMARY_COLOR)
                addLocationStatsTable(document, report)
                document.add(Paragraph("").setMarginBottom(20f))
            }

            // Peak Days
            if (report.peakParkingDays.isNotEmpty()) {
                addSectionTitle(document, "Peak Parking Days", PRIMARY_COLOR)
                document.add(
                    Paragraph("Your most active parking days: ${report.peakParkingDays.joinToString(", ")}")
                        .setFontSize(11f)
                        .setMarginBottom(10f)
                )
            }

            // Footer
            addFooter(document, report.createdAt?.toDate()?.time ?: System.currentTimeMillis())

            document.close()
            
            // Return content URI using FileProvider
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generate PDF for an admin report and return the file URI
     */
    fun generateAdminReportPdf(context: Context, report: ParkingReport): Uri? {
        return try {
            val fileName = "Admin_Report_${report.year}_${getMonthName(report.month)}_${System.currentTimeMillis()}.pdf"
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
            
            val pdfWriter = PdfWriter(file)
            val pdfDocument = PdfDocument(pdfWriter)
            val document = Document(pdfDocument, PageSize.A4)
            document.setMargins(36f, 36f, 36f, 36f)

            // Header
            addHeader(document, "PARKTRACK - ADMINISTRATIVE REPORT", PRIMARY_COLOR)
            addAdminReportMetadata(document, report)
            
            document.add(Paragraph("").setMarginBottom(20f))

            // System Overview Section
            addSectionTitle(document, "System Overview", PRIMARY_COLOR)
            addSystemOverviewTable(document, report)
            
            document.add(Paragraph("").setMarginBottom(20f))

            // Financial Summary Section
            addSectionTitle(document, "Financial Summary", PRIMARY_COLOR)
            addAdminFinancialTable(document, report)
            
            document.add(Paragraph("").setMarginBottom(20f))

            // Session Breakdown
            addSectionTitle(document, "Session Breakdown", PRIMARY_COLOR)
            addSessionBreakdownTable(document, report)
            
            document.add(Paragraph("").setMarginBottom(20f))

            // Report Details
            addSectionTitle(document, "Report Details", SECONDARY_COLOR)
            addReportDetailsTable(document, report)

            // Footer
            addFooter(document, report.createdAt?.toDate()?.time ?: System.currentTimeMillis())

            document.close()
            
            // Return content URI using FileProvider
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Share the generated PDF via system share dialog
     */
    fun sharePdf(context: Context, uri: Uri, subject: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        
        // Grant permissions to all potential receiving apps
        val chooser = Intent.createChooser(shareIntent, "Share Report")
        chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        
        context.startActivity(chooser)
    }

    /**
     * Open the generated PDF with default PDF viewer
     */
    fun openPdf(context: Context, uri: Uri) {
        val openIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NO_HISTORY)
        }
        
        try {
            context.startActivity(openIntent)
        } catch (e: Exception) {
            // If no PDF viewer is installed, show a toast or fallback
            val chooser = Intent.createChooser(openIntent, "Open PDF")
            context.startActivity(chooser)
        }
    }

    private fun addHeader(document: Document, title: String, color: DeviceRgb) {
        val header = Paragraph(title)
            .setFontSize(24f)
            .setBold()
            .setFontColor(color)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(5f)
        document.add(header)

        val subtitle = Paragraph("Official Parking Management Report")
            .setFontSize(12f)
            .setFontColor(SECONDARY_COLOR)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(20f)
        document.add(subtitle)

        // Add a horizontal line
        val line = Paragraph("_______________________________________________")
            .setTextAlignment(TextAlignment.CENTER)
            .setFontColor(color)
            .setMarginBottom(20f)
        document.add(line)
    }

    private fun addReportMetadata(document: Document, report: DriverReport) {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val periodText = if (report.periodStart != null && report.periodEnd != null) {
            "${dateFormat.format(report.periodStart.toDate())} - ${dateFormat.format(report.periodEnd.toDate())}"
        } else {
            "${getMonthName(report.month)} ${report.year}"
        }

        val metadataTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 2f)))
            .setWidth(UnitValue.createPercentValue(100f))

        metadataTable.addCell(createLabelCell("Report Type:"))
        metadataTable.addCell(createValueCell("${getReportTypeLabel(report.reportType)} Driver Report"))
        
        metadataTable.addCell(createLabelCell("Report Period:"))
        metadataTable.addCell(createValueCell(periodText))
        
        metadataTable.addCell(createLabelCell("Generated On:"))
        metadataTable.addCell(createValueCell(dateFormat.format(report.createdAt.toDate())))

        document.add(metadataTable)
    }

    private fun addAdminReportMetadata(document: Document, report: ParkingReport) {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val periodText = if (report.periodStart != null && report.periodEnd != null) {
            "${dateFormat.format(report.periodStart.toDate())} - ${dateFormat.format(report.periodEnd.toDate())}"
        } else {
            "${getMonthName(report.month)} ${report.year}"
        }

        val metadataTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 2f)))
            .setWidth(UnitValue.createPercentValue(100f))

        metadataTable.addCell(createLabelCell("Report Type:"))
        metadataTable.addCell(createValueCell("${getReportTypeLabel(report.reportType)} Administrative Report"))
        
        metadataTable.addCell(createLabelCell("Report Period:"))
        metadataTable.addCell(createValueCell(periodText))
        
        metadataTable.addCell(createLabelCell("Generated By:"))
        metadataTable.addCell(createValueCell(report.generatedByName.ifEmpty { "Admin" }))
        
        metadataTable.addCell(createLabelCell("Generated On:"))
        metadataTable.addCell(createValueCell(dateFormat.format(report.createdAt.toDate())))

        document.add(metadataTable)
    }

    private fun addDriverInfoTable(document: Document, report: DriverReport) {
        val table = Table(UnitValue.createPercentArray(floatArrayOf(1f, 2f)))
            .setWidth(UnitValue.createPercentValue(100f))
            .setBorder(Border.NO_BORDER)

        table.addCell(createLabelCell("Driver Name:"))
        table.addCell(createValueCell(report.driverName))
        
        table.addCell(createLabelCell("Email:"))
        table.addCell(createValueCell(report.driverEmail))
        
        table.addCell(createLabelCell("Phone Number:"))
        table.addCell(createValueCell(report.driverPhoneNumber.ifEmpty { "N/A" }))
        
        table.addCell(createLabelCell("Vehicle Number:"))
        table.addCell(createValueCell(report.vehicleNumber.ifEmpty { "N/A" }))

        document.add(table)
    }

    private fun addSessionStatsTable(document: Document, report: DriverReport) {
        val table = Table(UnitValue.createPercentArray(floatArrayOf(2f, 1f)))
            .setWidth(UnitValue.createPercentValue(100f))

        // Header row
        table.addHeaderCell(createHeaderCell("Metric"))
        table.addHeaderCell(createHeaderCell("Value"))

        table.addCell(createDataCell("Total Sessions"))
        table.addCell(createDataCell(report.totalSessions.toString()))
        
        table.addCell(createDataCell("Completed Sessions"))
        table.addCell(createDataCell(report.completedSessions.toString(), SUCCESS_COLOR))
        
        table.addCell(createDataCell("Active Sessions"))
        table.addCell(createDataCell(report.activeSessions.toString(), WARNING_COLOR))
        
        table.addCell(createDataCell("Average Session Duration"))
        table.addCell(createDataCell("${report.averageSessionDuration} minutes"))
        
        table.addCell(createDataCell("Total Parking Time"))
        table.addCell(createDataCell(formatDuration(report.totalDurationMinutes)))

        document.add(table)
    }

    private fun addFinancialSummaryTable(document: Document, report: DriverReport) {
        val table = Table(UnitValue.createPercentArray(floatArrayOf(2f, 1f)))
            .setWidth(UnitValue.createPercentValue(100f))

        // Header row
        table.addHeaderCell(createHeaderCell("Description"))
        table.addHeaderCell(createHeaderCell("Amount (Rs.)"))

        table.addCell(createDataCell("Total Charges"))
        table.addCell(createDataCell(String.format("%.2f", report.totalCharges)))
        
        table.addCell(createDataCell("Amount Paid"))
        table.addCell(createDataCell(String.format("%.2f", report.totalPaid), SUCCESS_COLOR))
        
        table.addCell(createDataCell("Outstanding Amount"))
        table.addCell(createDataCell(String.format("%.2f", report.totalOutstanding), 
            if (report.totalOutstanding > 0) DANGER_COLOR else SUCCESS_COLOR))
        
        if (report.overdueCharges > 0) {
            table.addCell(createDataCell("Overdue Charges"))
            table.addCell(createDataCell(String.format("%.2f", report.overdueCharges), DANGER_COLOR))
        }

        document.add(table)
    }

    private fun addLocationStatsTable(document: Document, report: DriverReport) {
        val table = Table(UnitValue.createPercentArray(floatArrayOf(2f, 1f)))
            .setWidth(UnitValue.createPercentValue(100f))

        table.addHeaderCell(createHeaderCell("Location"))
        table.addHeaderCell(createHeaderCell("Visits"))

        report.totalVisitsByLocation.forEach { (location, visits) ->
            table.addCell(createDataCell(location))
            table.addCell(createDataCell(visits.toString()))
        }

        document.add(table)
    }

    private fun addSystemOverviewTable(document: Document, report: ParkingReport) {
        val table = Table(UnitValue.createPercentArray(floatArrayOf(2f, 1f)))
            .setWidth(UnitValue.createPercentValue(100f))

        table.addHeaderCell(createHeaderCell("Metric"))
        table.addHeaderCell(createHeaderCell("Value"))

        table.addCell(createDataCell("Total Sessions"))
        table.addCell(createDataCell(report.totalSessions.toString()))
        
        table.addCell(createDataCell("Unique Vehicles"))
        table.addCell(createDataCell(report.numberOfUniqueVehicles.toString()))
        
        table.addCell(createDataCell("Registered Drivers"))
        table.addCell(createDataCell(report.numberOfRegisteredDrivers.toString()))
        
        table.addCell(createDataCell("Average Session Duration"))
        table.addCell(createDataCell("${report.averageSessionDuration} minutes"))
        
        table.addCell(createDataCell("Average Occupancy"))
        table.addCell(createDataCell("${String.format("%.1f", report.averageOccupancy)}%"))

        document.add(table)
    }

    private fun addAdminFinancialTable(document: Document, report: ParkingReport) {
        val table = Table(UnitValue.createPercentArray(floatArrayOf(2f, 1f)))
            .setWidth(UnitValue.createPercentValue(100f))

        table.addHeaderCell(createHeaderCell("Description"))
        table.addHeaderCell(createHeaderCell("Amount (Rs.)"))

        table.addCell(createDataCell("Total Revenue"))
        table.addCell(createDataCell(String.format("%.2f", report.totalRevenue)))
        
        table.addCell(createDataCell("Amount Collected"))
        table.addCell(createDataCell(String.format("%.2f", report.amountCollected), SUCCESS_COLOR))
        
        table.addCell(createDataCell("Outstanding Amount"))
        table.addCell(createDataCell(String.format("%.2f", report.outstandingAmount), 
            if (report.outstandingAmount > 0) DANGER_COLOR else SUCCESS_COLOR))
        
        if (report.totalOverdueCharges > 0) {
            table.addCell(createDataCell("Overdue Charges"))
            table.addCell(createDataCell(String.format("%.2f", report.totalOverdueCharges), DANGER_COLOR))
        }

        document.add(table)
    }

    private fun addSessionBreakdownTable(document: Document, report: ParkingReport) {
        val table = Table(UnitValue.createPercentArray(floatArrayOf(2f, 1f)))
            .setWidth(UnitValue.createPercentValue(100f))

        table.addHeaderCell(createHeaderCell("Status"))
        table.addHeaderCell(createHeaderCell("Count"))

        table.addCell(createDataCell("Paid Sessions"))
        table.addCell(createDataCell(report.paidSessions.toString(), SUCCESS_COLOR))
        
        table.addCell(createDataCell("Unpaid Sessions"))
        table.addCell(createDataCell(report.unpaidSessions.toString(), WARNING_COLOR))
        
        table.addCell(createDataCell("Overdue Sessions"))
        table.addCell(createDataCell(report.overdueSessions.toString(), DANGER_COLOR))

        document.add(table)
    }

    private fun addReportDetailsTable(document: Document, report: ParkingReport) {
        val table = Table(UnitValue.createPercentArray(floatArrayOf(1f, 2f)))
            .setWidth(UnitValue.createPercentValue(100f))

        table.addCell(createLabelCell("Report ID:"))
        table.addCell(createValueCell(report.id))
        
        table.addCell(createLabelCell("Generated By Admin ID:"))
        table.addCell(createValueCell(report.generatedBy))
        
        if (report.peakHours.isNotEmpty()) {
            table.addCell(createLabelCell("Peak Hours:"))
            table.addCell(createValueCell(report.peakHours.joinToString(", ")))
        }

        document.add(table)
    }

    private fun addSectionTitle(document: Document, title: String, color: DeviceRgb) {
        val sectionTitle = Paragraph(title)
            .setFontSize(14f)
            .setBold()
            .setFontColor(color)
            .setMarginBottom(10f)
            .setBorderBottom(SolidBorder(color, 1f))
        document.add(sectionTitle)
    }

    private fun addFooter(document: Document, timestamp: Long) {
        document.add(Paragraph("").setMarginTop(30f))
        
        val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
        val footer = Paragraph("This report was generated by ParkTrack on ${dateFormat.format(timestamp)}")
            .setFontSize(9f)
            .setFontColor(SECONDARY_COLOR)
            .setTextAlignment(TextAlignment.CENTER)
            .setItalic()
        document.add(footer)

        val disclaimer = Paragraph("This is a computer-generated document and does not require a signature.")
            .setFontSize(8f)
            .setFontColor(SECONDARY_COLOR)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(5f)
        document.add(disclaimer)
    }

    // Helper methods for creating table cells
    private fun createLabelCell(text: String): Cell {
        return Cell()
            .add(Paragraph(text).setBold().setFontSize(10f))
            .setBorder(Border.NO_BORDER)
            .setPadding(5f)
    }

    private fun createValueCell(text: String): Cell {
        return Cell()
            .add(Paragraph(text).setFontSize(10f))
            .setBorder(Border.NO_BORDER)
            .setPadding(5f)
    }

    private fun createHeaderCell(text: String): Cell {
        return Cell()
            .add(Paragraph(text).setBold().setFontSize(11f).setFontColor(ColorConstants.WHITE))
            .setBackgroundColor(PRIMARY_COLOR)
            .setTextAlignment(TextAlignment.CENTER)
            .setPadding(8f)
    }

    private fun createDataCell(text: String, color: DeviceRgb = PRIMARY_COLOR): Cell {
        return Cell()
            .add(Paragraph(text).setFontSize(10f).setFontColor(color))
            .setPadding(5f)
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

    private fun getReportTypeLabel(type: ReportType): String {
        return when (type) {
            ReportType.MONTHLY -> "Monthly"
            ReportType.QUARTERLY -> "Quarterly"
            ReportType.ANNUAL -> "Annual"
            ReportType.CUSTOM -> "Custom"
            ReportType.DRIVER_PERSONAL -> "Personal"
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
}
