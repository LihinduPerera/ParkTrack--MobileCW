package com.example.parktrack.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.parktrack.data.model.ParkingLot

object NavigationUtils {
    
    /**
     * Opens Google Maps navigation to the specified parking lot
     * @param context The context to start the intent from
     * @param parkingLot The parking lot to navigate to
     * @return true if navigation was started successfully, false otherwise
     */
    fun navigateToParkingLot(context: Context, parkingLot: ParkingLot): Boolean {
        return try {
            // Create Google Maps navigation intent
            val uri = Uri.parse("google.navigation:q=${parkingLot.latitude},${parkingLot.longitude}")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.google.android.apps.maps")
            }
            
            // Check if Google Maps is available
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                // Fallback to generic map intent if Google Maps is not available
                navigateToParkingLotGeneric(context, parkingLot)
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Opens navigation using any available map application
     * @param context The context to start the intent from
     * @param parkingLot The parking lot to navigate to
     * @return true if navigation was started successfully, false otherwise
     */
    private fun navigateToParkingLotGeneric(context: Context, parkingLot: ParkingLot): Boolean {
        return try {
            val uri = Uri.parse("geo:${parkingLot.latitude},${parkingLot.longitude}?q=${parkingLot.name}")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Checks if navigation is available on the device
     * @param context The context to check package manager
     * @return true if at least one navigation app is available
     */
    fun isNavigationAvailable(context: Context): Boolean {
        val googleMapsIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=")).apply {
            setPackage("com.google.android.apps.maps")
        }
        val genericIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q="))
        
        return googleMapsIntent.resolveActivity(context.packageManager) != null ||
               genericIntent.resolveActivity(context.packageManager) != null
    }
}