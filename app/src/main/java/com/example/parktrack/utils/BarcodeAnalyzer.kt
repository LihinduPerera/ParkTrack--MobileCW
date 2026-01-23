package com.example.parktrack.utils

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import android.util.Log

/**
 * ML Kit barcode analyzer for real-time QR code scanning
 */
class BarcodeAnalyzer(
    private val onBarcodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {
    
    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    )
    
    private var lastScanTime = 0L
    private var lastScannedValue = ""
    private val scanDebounce = 3000L // 3 seconds between same QR scans
    
    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            try {
                val image = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )
                
                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        for (barcode in barcodes) {
                            barcode.rawValue?.let { qrString ->
                                val currentTime = System.currentTimeMillis()
                                // Only trigger if enough time passed OR it's a different QR code
                                if (currentTime - lastScanTime > scanDebounce || qrString != lastScannedValue) {
                                    lastScanTime = currentTime
                                    lastScannedValue = qrString
                                    Log.d("BarcodeAnalyzer", "QR Code detected: $qrString")
                                    onBarcodeDetected(qrString)
                                }
                            }
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("BarcodeAnalyzer", "Barcode scanning failed: ${exception.message}", exception)
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } catch (e: Exception) {
                Log.e("BarcodeAnalyzer", "Error analyzing image: ${e.message}", e)
                imageProxy.close()
            }
        } else {
            imageProxy.close()
        }
    }
}
