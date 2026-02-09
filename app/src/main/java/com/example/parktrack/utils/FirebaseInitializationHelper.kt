package com.example.parktrack.utils

import com.example.parktrack.data.model.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID
import java.util.Calendar

/**
 * Helper class for initializing Firebase database with sample data
 * Run this once on app first launch to set up initial parking lots and rates
 */
object FirebaseInitializationHelper {

    /**
     * Initialize database with sample data
     * Call this on admin login or provide admin menu option
     */
    suspend fun initializeSampleData(firestore: FirebaseFirestore) {
        // Create sample parking lots
        createSampleParkingLots(firestore)
        // Create sample rates for lots
        createSampleParkingRates(firestore)
    }

    private suspend fun createSampleParkingLots(firestore: FirebaseFirestore) {
        val sampleLots = listOf(
            ParkingLot(
                id = "lot_001",
                name = "City Center Parking",
                location = "Downtown",
                latitude = 6.9271,
                longitude = 80.7789,
                totalSpaces = 500,
                availableSpaces = 450,
                occupiedSpaces = 50,
                address = "123 Main Street",
                city = "Colombo",
                state = "Western Province",
                zipCode = "00100",
                openingTime = "06:00",
                closingTime = "23:59",
                twentyFourHours = false,
                hasEVCharging = true,
                hasDisabledParking = true
            ),
            ParkingLot(
                id = "lot_002",
                name = "Airport Parking",
                location = "Airport",
                latitude = 6.9271,
                longitude = 80.1922,
                totalSpaces = 300,
                availableSpaces = 290,
                occupiedSpaces = 10,
                address = "Bandaranaike International Airport",
                city = "Colombo",
                state = "Western Province",
                zipCode = "00600",
                openingTime = "00:00",
                closingTime = "23:59",
                twentyFourHours = true,
                hasEVCharging = true,
                hasDisabledParking = true
            ),
            ParkingLot(
                id = "lot_003",
                name = "Mall Parking",
                location = "Shopping District",
                latitude = 6.9160,
                longitude = 80.7725,
                totalSpaces = 400,
                availableSpaces = 350,
                occupiedSpaces = 50,
                address = "456 Retail Lane",
                city = "Colombo",
                state = "Western Province",
                zipCode = "00200",
                openingTime = "08:00",
                closingTime = "22:00",
                twentyFourHours = false,
                hasEVCharging = false,
                hasDisabledParking = true
            )
        )

        sampleLots.forEach { lot ->
            firestore.collection("parkingLots").document(lot.id).set(lot).await()
        }
    }

    private suspend fun createSampleParkingRates(firestore: FirebaseFirestore) {
        // Rates for City Center
        val centerRates = listOf(
            ParkingRate(
                id = "rate_001",
                parkingLotId = "lot_001",
                rateType = RateType.NORMAL,
                basePricePerHour = 100.0,
                maxDailyPrice = 500.0,
                minChargeHours = 1.0,
                minChargeAmount = 100.0,
                vipMultiplier = 1.5,
                isActive = true
            ),
            ParkingRate(
                id = "rate_002",
                parkingLotId = "lot_001",
                rateType = RateType.VIP,
                basePricePerHour = 150.0,
                maxDailyPrice = 750.0,
                minChargeHours = 1.0,
                minChargeAmount = 150.0,
                vipMultiplier = 2.0,
                isActive = true
            ),
            ParkingRate(
                id = "rate_003",
                parkingLotId = "lot_001",
                rateType = RateType.OVERNIGHT,
                basePricePerHour = 50.0,
                maxDailyPrice = 200.0,
                overnightStartHour = 22,
                overnightEndHour = 6,
                isActive = true
            )
        )

        // Rates for Airport
        val airportRates = listOf(
            ParkingRate(
                id = "rate_004",
                parkingLotId = "lot_002",
                rateType = RateType.NORMAL,
                basePricePerHour = 120.0,
                maxDailyPrice = 600.0,
                minChargeHours = 1.0,
                minChargeAmount = 120.0,
                isActive = true
            ),
            ParkingRate(
                id = "rate_005",
                parkingLotId = "lot_002",
                rateType = RateType.VIP,
                basePricePerHour = 180.0,
                maxDailyPrice = 900.0,
                minChargeHours = 1.0,
                minChargeAmount = 180.0,
                vipMultiplier = 1.5,
                isActive = true
            )
        )

        // Rates for Mall
        val mallRates = listOf(
            ParkingRate(
                id = "rate_006",
                parkingLotId = "lot_003",
                rateType = RateType.HOURLY,
                hourlyRate = 80.0,
                maxDailyPrice = 400.0,
                minChargeHours = 1.0,
                minChargeAmount = 80.0,
                isActive = true
            )
        )

        (centerRates + airportRates + mallRates).forEach { rate ->
            firestore.collection("parkingRates").document(rate.id).set(rate).await()
        }
    }

    /**
     * Generate test users for development
     */
    suspend fun createTestUsers(firestore: FirebaseFirestore) {
        val testUsers = listOf(
            User(
                id = "admin_001",
                name = "Park Admin",
                email = "admin@parktrack.com",
                fullName = "Park Administrator",
                role = UserRole.ADMIN,
                phoneNumber = "+94771234567",
                createdAt = System.currentTimeMillis(),
                isVerified = true
            ),
            User(
                id = "driver_001",
                name = "John Driver",
                email = "john@example.com",
                fullName = "John Driver",
                role = UserRole.DRIVER,
                phoneNumber = "+94772345678",
                vehicleNumber = "SL-12-AB-1234",
                createdAt = System.currentTimeMillis(),
                isVerified = true
            ),
            User(
                id = "driver_002",
                name = "Jane S.",
                email = "jane@example.com",
                fullName = "Jane Smith",
                role = UserRole.DRIVER,
                phoneNumber = "+94773456789",
                vehicleNumber = "SL-12-CD-5678",
                createdAt = System.currentTimeMillis(),
                isVerified = true
            )
        )

        testUsers.forEach { user ->
            firestore.collection("users").document(user.id).set(user).await()
        }
    }

    /**
     * Create test vehicles
     */
    suspend fun createTestVehicles(firestore: FirebaseFirestore) {
        val testVehicles = listOf(
            Vehicle(
                id = UUID.randomUUID().toString(),
                ownerId = "driver_001",
                vehicleNumber = "SL-12-AB-1234",
                vehicleModel = "Toyota Camry 2022",
                vehicleColor = "Black",
                vehicleType = "Car",
                registrationNumber = "ABC-1234",
                isActive = true
            ),
            Vehicle(
                id = UUID.randomUUID().toString(),
                ownerId = "driver_002",
                vehicleNumber = "SL-12-CD-5678",
                vehicleModel = "Honda Civic 2021",
                vehicleColor = "White",
                vehicleType = "Car",
                registrationNumber = "XYZ-5678",
                isActive = true
            )
        )

        testVehicles.forEach { vehicle ->
            firestore.collection("vehicles").document(vehicle.id).set(vehicle).await()
        }
    }

    /**
     * Create sample parking sessions for testing
     */
    suspend fun createTestSessions(firestore: FirebaseFirestore) {
        val now = Calendar.getInstance()
        val cal1 = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, -1)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
        }
        val cal2 = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, -1)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 30)
        }

        val testSessions = listOf(
            ParkingSession(
                id = UUID.randomUUID().toString(),
                driverId = "driver_001",
                driverName = "John Driver",
                vehicleNumber = "SL-12-AB-1234",
                entryTime = Timestamp(cal1.time),
                exitTime = Timestamp(cal2.time),
                gateLocation = "Main Gate",
                scannedByAdminId = "admin_001",
                adminName = "Park Admin",
                status = "COMPLETED",
                durationMinutes = 210
            )
        )

        testSessions.forEach { session ->
            firestore.collection("parkingSessions").document(session.id).set(session).await()
        }
    }
}
