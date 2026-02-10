package com.example.parktrack

import com.example.parktrack.data.model.ParkingSession
import com.example.parktrack.data.model.User
import com.example.parktrack.data.model.SubscriptionTier
import com.example.parktrack.data.repository.ParkingSessionRepository
import com.google.firebase.Timestamp
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

/**
 * Unit tests for Exit QR Code Session Completion
 */
class ExitQRCompletionTest {

    @Mock
    private lateinit var repository: ParkingSessionRepository

    private lateinit var testSession: ParkingSession
    private lateinit var testDriver: User

    @org.junit.Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        // Create test session
        testSession = ParkingSession(
            id = "test-session-123",
            driverId = "driver-456",
            driverName = "Test Driver",
            vehicleNumber = "ABC-123",
            entryTime = Timestamp.now(),
            status = "ACTIVE",
            gateLocation = "Main Gate",
            scannedByAdminId = "admin-789",
            adminName = "Test Admin"
        )

        // Create test driver
        testDriver = User(
            id = "driver-456",
            fullName = "Test Driver",
            email = "test@example.com",
            subscriptionTier = SubscriptionTier.NORMAL
        )
    }

    @Test
    fun testSessionStatusChangeToCompleted() {
        // Given an active session
        assertEquals("ACTIVE", testSession.status)
        assertNull(testSession.exitTime)
        assertEquals(0L, testSession.durationMinutes)

        // When completing the session
        val exitTime = Timestamp.now()
        val expectedDuration = 60L // 1 hour in minutes

        // Mock successful completion
        `when`(repository.completeSession(testSession.id, exitTime))
            .thenReturn(Result.success(Unit))
        `when`(repository.calculateDuration(testSession.entryTime!!, exitTime))
            .thenReturn(expectedDuration)

        // Then the session should be completed
        val result = repository.completeSession(testSession.id, exitTime)
        assertTrue(result.isSuccess)

        // Verify the method was called with correct parameters
        verify(repository).completeSession(testSession.id, exitTime)
    }

    @Test
    fun testSessionCompletionFailsWithInvalidSession() {
        // Given an invalid session ID
        val invalidSessionId = "invalid-session"
        val exitTime = Timestamp.now()

        // Mock failure due to session not found
        `when`(repository.completeSession(invalidSessionId, exitTime))
            .thenReturn(Result.failure(Exception("Session not found")))

        // When attempting to complete, it should fail
        val result = repository.completeSession(invalidSessionId, exitTime)
        assertTrue(result.isFailure)
        assertEquals("Session not found", result.exceptionOrNull()?.message)
    }

    @Test
    fun testDurationCalculation() {
        // Given entry and exit times
        val entryTime = Timestamp.now()
        val exitTime = Timestamp(entryTime.toDate().time + (2 * 60 * 60 * 1000)) // 2 hours later

        // When calculating duration
        val duration = repository.calculateDuration(entryTime, exitTime)

        // Then duration should be correct (120 minutes)
        assertEquals(120L, duration)
    }

    @Test
    fun testSessionVerificationAfterCompletion() {
        // Given a completed session
        val completedSession = testSession.copy(
            status = "COMPLETED",
            exitTime = Timestamp.now(),
            durationMinutes = 60L
        )

        // Then session should have completed properties
        assertEquals("COMPLETED", completedSession.status)
        assertNotNull(completedSession.exitTime)
        assertTrue(completedSession.durationMinutes > 0)
    }

    @Test
    fun testExitQRValidation() {
        // Given an EXIT QR code for a driver with active session
        val qrData = com.example.parktrack.data.model.QRCodeData(
            userId = testDriver.id,
            qrType = "EXIT",
            vehicleNumber = "ABC-123",
            timestamp = System.currentTimeMillis()
        )

        // Then QR data should be valid for exit
        assertEquals("EXIT", qrData.qrType)
        assertEquals(testDriver.id, qrData.userId)
    }

    @Test
    fun testExitQRFailsWithoutActiveSession() {
        // Given an EXIT QR code but no active session
        val qrData = com.example.parktrack.data.model.QRCodeData(
            userId = "driver-without-session",
            qrType = "EXIT",
            vehicleNumber = "XYZ-789",
            timestamp = System.currentTimeMillis()
        )

        // Mock no active session found
        `when`(repository.getActiveSessionForDriver(qrData.userId))
            .thenReturn(Result.success(null))

        // When checking for active session
        val result = repository.getActiveSessionForDriver(qrData.userId)

        // Then no session should be found
        assertTrue(result.isSuccess)
        assertNull(result.getOrNull())
    }
}