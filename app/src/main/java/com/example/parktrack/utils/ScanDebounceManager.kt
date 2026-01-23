package com.example.parktrack.utils

import androidx.annotation.GuardedBy
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Manages scan debouncing to prevent duplicate entry/exit recordings
 * Uses thread-safe mechanisms to handle rapid QR scanning
 */
object ScanDebounceManager {
    
    // Maps driverId -> lastScanTime for tracking per-driver debouncing
    @GuardedBy("lock")
    private val driverScanTimes = mutableMapOf<String, Long>()
    
    // Maps driverId -> lastSessionStatus for detecting state changes
    @GuardedBy("lock")
    private val driverSessionStatus = mutableMapOf<String, String?>()
    
    private val lock = ReentrantReadWriteLock()
    
    // Minimum time between scans for the same driver (3 seconds)
    private const val DEBOUNCE_MS = 3000L
    
    /**
     * Check if a scan for this driver should be allowed
     * @param driverId The driver ID
     * @param currentSessionStatus The current session status (ACTIVE for entry, COMPLETED for exit)
     * @return true if scan should be processed, false if it should be ignored
     */
    fun shouldProcessScan(driverId: String, currentSessionStatus: String?): Boolean {
        return lock.write {
            val currentTime = System.currentTimeMillis()
            val lastScanTime = driverScanTimes[driverId] ?: 0L
            val lastStatus = driverSessionStatus[driverId]
            
            // Check if status has changed (ACTIVE -> COMPLETED = exit, null/no session -> ACTIVE = entry)
            val statusChanged = lastStatus != currentSessionStatus
            
            // Allow if enough time has passed OR status has changed (indicating real transition)
            val shouldAllow = (currentTime - lastScanTime > DEBOUNCE_MS) || statusChanged
            
            if (shouldAllow) {
                driverScanTimes[driverId] = currentTime
                driverSessionStatus[driverId] = currentSessionStatus
            }
            
            shouldAllow
        }
    }
    
    /**
     * Reset the scan record for a driver
     * @param driverId The driver ID
     */
    fun resetDriver(driverId: String) {
        lock.write {
            driverScanTimes.remove(driverId)
            driverSessionStatus.remove(driverId)
        }
    }
    
    /**
     * Clear all scan records
     */
    fun clearAll() {
        lock.write {
            driverScanTimes.clear()
            driverSessionStatus.clear()
        }
    }
    
    /**
     * Get time remaining before next scan is allowed for a driver
     * @param driverId The driver ID
     * @return Time in milliseconds, or 0 if scan is allowed
     */
    fun getTimeUntilNextScan(driverId: String): Long {
        return lock.read {
            val currentTime = System.currentTimeMillis()
            val lastScanTime = driverScanTimes[driverId] ?: 0L
            val timeSinceLastScan = currentTime - lastScanTime
            
            if (timeSinceLastScan >= DEBOUNCE_MS) 0L else DEBOUNCE_MS - timeSinceLastScan
        }
    }
}
