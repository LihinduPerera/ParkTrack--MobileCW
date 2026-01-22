package com.example.parktrack.utils

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
    private val scanDebounce = 2000L // 2 seconds between scans
    
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
                                if (currentTime - lastScanTime > scanDebounce) {
                                    lastScanTime = currentTime
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
