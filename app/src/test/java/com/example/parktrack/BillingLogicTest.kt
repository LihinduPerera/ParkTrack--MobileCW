package com.example.parktrack

import com.example.parktrack.billing.TierPricing
import com.example.parktrack.billing.calculateParkingCharge
import com.example.parktrack.data.model.ParkingRate
import com.example.parktrack.data.model.RateType
import com.example.parktrack.data.model.SubscriptionTier
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * FIXED: Billing logic tests with correct pricing
 * - Normal Tier: Rs. 100/hour (fee calculated on exit only)
 * - Gold Tier: Rs. 80/hour (1st hour free, then completed hours only)
 * - Platinum Tier: Rs. 60/hour (1st hour free, then completed hours only)
 * 
 * ALL TIERS: Fee is calculated ONLY on exit, not on entry
 */
class BillingLogicTest {

    private val sampleParkingRate = ParkingRate(
        id = "test_rate",
        parkingLotId = "test_lot",
        rateType = RateType.NORMAL,
        basePricePerHour = 100.0,
        normalRate = TierPricing.NORMAL_HOURLY_RATE,      // 100.0
        goldRate = TierPricing.GOLD_HOURLY_RATE,          // 80.0
        platinumRate = TierPricing.PLATINUM_HOURLY_RATE   // 60.0
    )

    // ==================== NORMAL TIER TESTS ====================

    @Test
    fun `test normal tier minimum charge is Rs 100 even for 0 minutes`() {
        // 0 minutes parking - should still charge minimum 1 hour = Rs. 100
        val charge0 = calculateParkingCharge(0, SubscriptionTier.NORMAL, sampleParkingRate)
        assertEquals(100.0, charge0, 0.01)
        
        // 1 minute parking - should charge 1 hour = Rs. 100
        val charge1 = calculateParkingCharge(1, SubscriptionTier.NORMAL, sampleParkingRate)
        assertEquals(100.0, charge1, 0.01)
        
        // 30 minutes parking - should charge 1 hour = Rs. 100
        val charge30 = calculateParkingCharge(30, SubscriptionTier.NORMAL, sampleParkingRate)
        assertEquals(100.0, charge30, 0.01)
    }

    @Test
    fun `test normal tier charges full hour even for 1 minute`() {
        // 1 minute parking - should charge 1 hour = Rs. 100
        val charge = calculateParkingCharge(1, SubscriptionTier.NORMAL, sampleParkingRate)
        assertEquals(100.0, charge, 0.01)

        // 30 minutes parking - should charge 1 hour = Rs. 100
        val charge30 = calculateParkingCharge(30, SubscriptionTier.NORMAL, sampleParkingRate)
        assertEquals(100.0, charge30, 0.01)

        // 61 minutes parking (1 hour + 1 minute) - should charge 2 hours = Rs. 200
        val charge61 = calculateParkingCharge(61, SubscriptionTier.NORMAL, sampleParkingRate)
        assertEquals(200.0, charge61, 0.01)
        
        // 120 minutes (2 hours) - should charge 2 hours = Rs. 200
        val charge120 = calculateParkingCharge(120, SubscriptionTier.NORMAL, sampleParkingRate)
        assertEquals(200.0, charge120, 0.01)
    }

    @Test
    fun `test normal tier multiple hours`() {
        // 3 hours exactly - should charge Rs. 300
        val charge3h = calculateParkingCharge(180, SubscriptionTier.NORMAL, sampleParkingRate)
        assertEquals(300.0, charge3h, 0.01)
        
        // 3 hours 30 minutes - should round up to 4 hours = Rs. 400
        val charge3h30m = calculateParkingCharge(210, SubscriptionTier.NORMAL, sampleParkingRate)
        assertEquals(400.0, charge3h30m, 0.01)
    }

    // ==================== GOLD TIER TESTS ====================

    @Test
    fun `test gold tier no charge within first hour`() {
        // 30 minutes parking - should be FREE
        val charge30 = calculateParkingCharge(30, SubscriptionTier.GOLD, sampleParkingRate)
        assertEquals(0.0, charge30, 0.01)

        // 60 minutes parking - should be FREE (exactly 1 hour)
        val charge60 = calculateParkingCharge(60, SubscriptionTier.GOLD, sampleParkingRate)
        assertEquals(0.0, charge60, 0.01)
    }

    @Test
    fun `test gold tier charges only completed hours after free hour`() {
        // 61 minutes (1h 1m) - should charge 0 completed hours = Rs. 0
        // (1h free + 0 completed hours = 0 charge)
        val charge61 = calculateParkingCharge(61, SubscriptionTier.GOLD, sampleParkingRate)
        assertEquals(0.0, charge61, 0.01)

        // 90 minutes (1h 30m) - should charge 0 completed hours = Rs. 0
        val charge90 = calculateParkingCharge(90, SubscriptionTier.GOLD, sampleParkingRate)
        assertEquals(0.0, charge90, 0.01)
        
        // 120 minutes (2h) - should charge 1 completed hour = Rs. 80
        val charge120 = calculateParkingCharge(120, SubscriptionTier.GOLD, sampleParkingRate)
        assertEquals(80.0, charge120, 0.01)
        
        // 150 minutes (2h 30m) - should charge 1 completed hour = Rs. 80
        val charge150 = calculateParkingCharge(150, SubscriptionTier.GOLD, sampleParkingRate)
        assertEquals(80.0, charge150, 0.01)
        
        // 180 minutes (3h) - should charge 2 completed hours = Rs. 160
        val charge180 = calculateParkingCharge(180, SubscriptionTier.GOLD, sampleParkingRate)
        assertEquals(160.0, charge180, 0.01)
    }

    @Test
    fun `test gold tier longer durations`() {
        // 4 hours - should charge 3 completed hours = Rs. 240
        val charge4h = calculateParkingCharge(240, SubscriptionTier.GOLD, sampleParkingRate)
        assertEquals(240.0, charge4h, 0.01)
        
        // 5 hours 45 minutes - should charge 4 completed hours = Rs. 320
        val charge5h45m = calculateParkingCharge(345, SubscriptionTier.GOLD, sampleParkingRate)
        assertEquals(320.0, charge5h45m, 0.01)
    }

    // ==================== PLATINUM TIER TESTS ====================

    @Test
    fun `test platinum tier no charge within first hour`() {
        // 30 minutes parking - should be FREE
        val charge30 = calculateParkingCharge(30, SubscriptionTier.PLATINUM, sampleParkingRate)
        assertEquals(0.0, charge30, 0.01)

        // 60 minutes parking - should be FREE (exactly 1 hour)
        val charge60 = calculateParkingCharge(60, SubscriptionTier.PLATINUM, sampleParkingRate)
        assertEquals(0.0, charge60, 0.01)
    }

    @Test
    fun `test platinum tier charges only completed hours after free hour`() {
        // 61 minutes (1h 1m) - should charge 0 completed hours = Rs. 0
        val charge61 = calculateParkingCharge(61, SubscriptionTier.PLATINUM, sampleParkingRate)
        assertEquals(0.0, charge61, 0.01)

        // 90 minutes (1h 30m) - should charge 0 completed hours = Rs. 0
        val charge90 = calculateParkingCharge(90, SubscriptionTier.PLATINUM, sampleParkingRate)
        assertEquals(0.0, charge90, 0.01)
        
        // 120 minutes (2h) - should charge 1 completed hour = Rs. 60
        val charge120 = calculateParkingCharge(120, SubscriptionTier.PLATINUM, sampleParkingRate)
        assertEquals(60.0, charge120, 0.01)
        
        // 180 minutes (3h) - should charge 2 completed hours = Rs. 120
        val charge180 = calculateParkingCharge(180, SubscriptionTier.PLATINUM, sampleParkingRate)
        assertEquals(120.0, charge180, 0.01)
    }

    @Test
    fun `test platinum tier longer durations`() {
        // 4 hours - should charge 3 completed hours = Rs. 180
        val charge4h = calculateParkingCharge(240, SubscriptionTier.PLATINUM, sampleParkingRate)
        assertEquals(180.0, charge4h, 0.01)
        
        // 6 hours 30 minutes - should charge 5 completed hours = Rs. 300
        val charge6h30m = calculateParkingCharge(390, SubscriptionTier.PLATINUM, sampleParkingRate)
        assertEquals(300.0, charge6h30m, 0.01)
    }

    // ==================== COMPARISON TESTS ====================

    @Test
    fun `test tier pricing comparison for same duration`() {
        val duration = 180L // 3 hours
        
        val normalCharge = calculateParkingCharge(duration, SubscriptionTier.NORMAL, sampleParkingRate)
        val goldCharge = calculateParkingCharge(duration, SubscriptionTier.GOLD, sampleParkingRate)
        val platinumCharge = calculateParkingCharge(duration, SubscriptionTier.PLATINUM, sampleParkingRate)
        
        // Normal: 3 hours * Rs. 100 = Rs. 300
        assertEquals(300.0, normalCharge, 0.01)
        
        // Gold: (3-1) completed hours * Rs. 80 = Rs. 160
        assertEquals(160.0, goldCharge, 0.01)
        
        // Platinum: (3-1) completed hours * Rs. 60 = Rs. 120
        assertEquals(120.0, platinumCharge, 0.01)
        
        // Verify savings
        assertEquals(140.0, normalCharge - goldCharge, 0.01) // Gold saves Rs. 140
        assertEquals(180.0, normalCharge - platinumCharge, 0.01) // Platinum saves Rs. 180
    }

    @Test
    fun `test tier pricing for short duration under one hour`() {
        val duration = 30L // 30 minutes
        
        val normalCharge = calculateParkingCharge(duration, SubscriptionTier.NORMAL, sampleParkingRate)
        val goldCharge = calculateParkingCharge(duration, SubscriptionTier.GOLD, sampleParkingRate)
        val platinumCharge = calculateParkingCharge(duration, SubscriptionTier.PLATINUM, sampleParkingRate)
        
        // Normal: Must pay full hour = Rs. 100
        assertEquals(100.0, normalCharge, 0.01)
        
        // Gold: Free (within 1st hour)
        assertEquals(0.0, goldCharge, 0.01)
        
        // Platinum: Free (within 1st hour)
        assertEquals(0.0, platinumCharge, 0.01)
    }

    // ==================== FALLBACK RATE TESTS ====================

    @Test
    fun `test fallback to base rate when tier rates not set`() {
        val fallbackRate = ParkingRate(
            id = "fallback_rate",
            parkingLotId = "test_lot",
            rateType = RateType.NORMAL,
            basePricePerHour = 100.0,
            normalRate = 0.0,    // Not set - should use fixed rate
            goldRate = 0.0,      // Not set - should use fixed rate
            platinumRate = 0.0   // Not set - should use fixed rate
        )

        // Should use fixed tier pricing from TierPricing object
        val normalCharge = calculateParkingCharge(60, SubscriptionTier.NORMAL, fallbackRate)
        assertEquals(TierPricing.NORMAL_HOURLY_RATE, normalCharge, 0.01)

        val goldCharge = calculateParkingCharge(120, SubscriptionTier.GOLD, fallbackRate)
        assertEquals(TierPricing.GOLD_HOURLY_RATE, goldCharge, 0.01) // 1 completed hour

        val platinumCharge = calculateParkingCharge(120, SubscriptionTier.PLATINUM, fallbackRate)
        assertEquals(TierPricing.PLATINUM_HOURLY_RATE, platinumCharge, 0.01) // 1 completed hour
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    fun `test zero duration parking`() {
        val charge = calculateParkingCharge(0, SubscriptionTier.NORMAL, sampleParkingRate)
        assertEquals(100.0, charge, 0.01) // Normal tier charges minimum
        
        val goldCharge = calculateParkingCharge(0, SubscriptionTier.GOLD, sampleParkingRate)
        assertEquals(0.0, goldCharge, 0.01) // Gold is free
        
        val platinumCharge = calculateParkingCharge(0, SubscriptionTier.PLATINUM, sampleParkingRate)
        assertEquals(0.0, platinumCharge, 0.01) // Platinum is free
    }

    @Test
    fun `test very long duration parking`() {
        val duration = 24 * 60L // 24 hours
        
        val normalCharge = calculateParkingCharge(duration, SubscriptionTier.NORMAL, sampleParkingRate)
        assertEquals(2400.0, normalCharge, 0.01) // 24 * 100
        
        val goldCharge = calculateParkingCharge(duration, SubscriptionTier.GOLD, sampleParkingRate)
        assertEquals(1840.0, goldCharge, 0.01) // (24-1) * 80
        
        val platinumCharge = calculateParkingCharge(duration, SubscriptionTier.PLATINUM, sampleParkingRate)
        assertEquals(1380.0, platinumCharge, 0.01) // (24-1) * 60
    }

    // ==================== TIER PRICING CONSTANTS TESTS ====================

    @Test
    fun `test tier pricing constants are correct`() {
        assertEquals(100.0, TierPricing.NORMAL_HOURLY_RATE, 0.01)
        assertEquals(80.0, TierPricing.GOLD_HOURLY_RATE, 0.01)
        assertEquals(60.0, TierPricing.PLATINUM_HOURLY_RATE, 0.01)
        assertEquals(1, TierPricing.FREE_HOURS_GOLD_PLATINUM)
    }
}
