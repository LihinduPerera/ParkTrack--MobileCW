package com.example.parktrack

import com.example.parktrack.billing.calculateParkingCharge
import com.example.parktrack.data.model.ParkingRate
import com.example.parktrack.data.model.RateType
import com.example.parktrack.data.model.SubscriptionTier
import org.junit.Assert.assertEquals
import org.junit.Test

class BillingLogicTest {

    private val sampleParkingRate = ParkingRate(
        id = "test_rate",
        parkingLotId = "test_lot",
        rateType = RateType.NORMAL,
        basePricePerHour = 10.0,
        normalRate = 10.0,
        goldRate = 8.0,
        platinumRate = 6.0
    )

    @Test
    fun `test normal tier charges full hour even for 1 minute`() {
        // 1 minute parking
        val charge = calculateParkingCharge(1, SubscriptionTier.NORMAL, sampleParkingRate)
        assertEquals(10.0, charge, 0.01) // Should charge full hour

        // 30 minutes parking
        val charge30 = calculateParkingCharge(30, SubscriptionTier.NORMAL, sampleParkingRate)
        assertEquals(10.0, charge30, 0.01) // Should charge full hour

        // 61 minutes parking (1 hour + 1 minute)
        val charge61 = calculateParkingCharge(61, SubscriptionTier.NORMAL, sampleParkingRate)
        assertEquals(20.0, charge61, 0.01) // Should charge 2 full hours
    }

    @Test
    fun `test gold tier no charge until exceeding 1 hour`() {
        // 30 minutes parking
        val charge30 = calculateParkingCharge(30, SubscriptionTier.GOLD, sampleParkingRate)
        assertEquals(0.0, charge30, 0.01) // No charge

        // 60 minutes parking
        val charge60 = calculateParkingCharge(60, SubscriptionTier.GOLD, sampleParkingRate)
        assertEquals(0.0, charge60, 0.01) // No charge

        // 61 minutes parking (1 hour + 1 minute)
        val charge61 = calculateParkingCharge(61, SubscriptionTier.GOLD, sampleParkingRate)
        assertEquals(8.0, charge61, 0.01) // Charge for 1 minute at gold rate

        // 90 minutes parking (1 hour + 30 minutes)
        val charge90 = calculateParkingCharge(90, SubscriptionTier.GOLD, sampleParkingRate)
        assertEquals(8.0 * 0.5, charge90, 0.01) // Charge for 30 minutes at gold rate
    }

    @Test
    fun `test platinum tier no charge until exceeding 1 hour`() {
        // 30 minutes parking
        val charge30 = calculateParkingCharge(30, SubscriptionTier.PLATINUM, sampleParkingRate)
        assertEquals(0.0, charge30, 0.01) // No charge

        // 60 minutes parking
        val charge60 = calculateParkingCharge(60, SubscriptionTier.PLATINUM, sampleParkingRate)
        assertEquals(0.0, charge60, 0.01) // No charge

        // 61 minutes parking (1 hour + 1 minute)
        val charge61 = calculateParkingCharge(61, SubscriptionTier.PLATINUM, sampleParkingRate)
        assertEquals(6.0, charge61, 0.01) // Charge for 1 minute at platinum rate

        // 90 minutes parking (1 hour + 30 minutes)
        val charge90 = calculateParkingCharge(90, SubscriptionTier.PLATINUM, sampleParkingRate)
        assertEquals(6.0 * 0.5, charge90, 0.01) // Charge for 30 minutes at platinum rate
    }

    @Test
    fun `test tier-specific rates are used correctly`() {
        val customRate = ParkingRate(
            id = "custom_rate",
            parkingLotId = "test_lot",
            rateType = RateType.NORMAL,
            basePricePerHour = 10.0,
            normalRate = 15.0, // Higher for normal
            goldRate = 10.0,    // Medium for gold
            platinumRate = 5.0  // Lower for platinum
        )

        // 120 minutes (2 hours) parking
        val normalCharge = calculateParkingCharge(120, SubscriptionTier.NORMAL, customRate)
        assertEquals(30.0, normalCharge, 0.01) // 2 hours * 15.0

        val goldCharge = calculateParkingCharge(120, SubscriptionTier.GOLD, customRate)
        assertEquals(20.0, goldCharge, 0.01) // 1 hour free + 1 hour * 10.0

        val platinumCharge = calculateParkingCharge(120, SubscriptionTier.PLATINUM, customRate)
        assertEquals(10.0, platinumCharge, 0.01) // 1 hour free + 1 hour * 5.0
    }

    @Test
    fun `test fallback to base rate when tier rates not set`() {
        val fallbackRate = ParkingRate(
            id = "fallback_rate",
            parkingLotId = "test_lot",
            rateType = RateType.NORMAL,
            basePricePerHour = 12.0,
            normalRate = 0.0, // Not set
            goldRate = 0.0,    // Not set
            platinumRate = 0.0 // Not set
        )

        // 120 minutes (2 hours) parking
        val normalCharge = calculateParkingCharge(120, SubscriptionTier.NORMAL, fallbackRate)
        assertEquals(24.0, normalCharge, 0.01) // 2 hours * 12.0 base rate

        val goldCharge = calculateParkingCharge(120, SubscriptionTier.GOLD, fallbackRate)
        assertEquals(12.0, goldCharge, 0.01) // 1 hour free + 1 hour * 12.0 * 0.8 discount

        val platinumCharge = calculateParkingCharge(120, SubscriptionTier.PLATINUM, fallbackRate)
        assertEquals(9.6, platinumCharge, 0.01) // 1 hour free + 1 hour * 12.0 * 0.6 discount
    }
}
