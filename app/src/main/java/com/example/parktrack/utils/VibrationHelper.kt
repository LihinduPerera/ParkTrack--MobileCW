package com.example.parktrack.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

object VibrationHelper {
    
    /**
     * Vibrate for a short duration (success feedback)
     */
    fun vibrateSuccess(context: Context) {
        vibrate(context, 50)
    }
    
    /**
     * Vibrate for medium duration (normal feedback)
     */
    fun vibrateMedium(context: Context) {
        vibrate(context, 100)
    }
    
    /**
     * Vibrate for long duration (error feedback)
     */
    fun vibrateError(context: Context) {
        vibrate(context, 200)
    }
    
    /**
     * Double vibration (pattern)
     */
    fun vibratePattern(context: Context) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (vibrator?.hasVibrator() == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val amplitudes = intArrayOf(0, 50, 30, 50, 30)
                vibrator.vibrate(VibrationEffect.createWaveform(amplitudes))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 50, 30, 50, 30), -1)
            }
        }
    }
    
    /**
     * Generic vibration with duration
     */
    private fun vibrate(context: Context, duration: Long) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (vibrator?.hasVibrator() == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(duration)
            }
        }
    }
}
