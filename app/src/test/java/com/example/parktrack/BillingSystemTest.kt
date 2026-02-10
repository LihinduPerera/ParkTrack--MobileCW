package com.example.parktrack

import com.example.parktrack.billing.TierPricing
import com.example.parktrack.billing.calculateParkingCharge
import com.example.parktrack.data.model.ParkingRate
import com.example.parktrack.data.model.SubscriptionTier
import org.junit.Test
import org.junit.Assert.*

/**
 * FIXED: Test summary for the billing system implementation:
 *
 * ✅ Normal tier pricing: Rs. 100/hour - IMPLEMENTED
 * ✅ Gold tier pricing: Rs. 80/hour - IMPLEMENTED
 * ✅ Platinum tier pricing: Rs. 60/hour - IMPLEMENTED
 * ✅ All tiers: Fee calculated ONLY on exit - IMPLEMENTED
 * ✅ Gold users get first hour FREE - IMPLEMENTED
 * ✅ Platinum users get first hour FREE - IMPLEMENTED
 * ✅ Gold/Platinum users pay only for COMPLETED hours after free hour - IMPLEMENTED
 * ✅ Normal users: Minimum Rs 100 charge even for 0 minutes - IMPLEMENTED
 * ✅ Normal users: Hours rounded UP (ceil) - IMPLEMENTED
 * ✅ Gold/Platinum users: Hours rounded DOWN (floor) after free hour - IMPLEMENTED
 * ✅ Comprehensive test coverage - IMPLEMENTED
 */
class BillingSystemTest {

    private val defaultRate = ParkingRate(
        id = "test",
        parkingLotId = "test_lot",
        basePricePerHour = TierPricing.NORMAL_HOURLY_RATE,
        normalRate = TierPricing.NORMAL_HOURLY_RATE,      // 100.0
        goldRate = TierPricing.GOLD_HOURLY_RATE,          // 80.0
        platinumRate = TierPricing.PLATINUM_HOURLY_RATE,  // 60.0
        maxDailyPrice = 500.0,
        vipMultiplier = 1.5,
        overnightRate = 50.0,
        isActive = true
    )

    @Test
    fun testNormalUserPaysFullHourEvenForOneMinute() {
        // Normal user parked for 1 minute should pay full hour (Rs. 100)
        val charge = calculateParkingCharge(1, SubscriptionTier.NORMAL, defaultRate)
        assertEquals(100.0, charge, 0.01)
    }

    @Test
    fun testNormalUserPaysFullHourForPartialHour() {
        // Normal user parked for 45 minutes should pay full hour (Rs. 100)
        val charge = calculateParkingCharge(45, SubscriptionTier.NORMAL, defaultRate)
        assertEquals(100.0, charge, 0.01)
    }

    @Test
    fun testNormalUserPaysMultipleHours() {
        // Normal user parked for 2.5 hours should pay 3 hours (Rs. 300)
        val charge = calculateParkingCharge(150, SubscriptionTier.NORMAL, defaultRate)
        assertEquals(300.0, charge, 0.01)
    }

    @Test
    fun testGoldUserFirstHourFree() {
        // Gold user parked for 30 minutes should pay nothing (FREE first hour)
        val charge = calculateParkingCharge(30, SubscriptionTier.GOLD, defaultRate)
        assertEquals(0.0, charge, 0.01)
    }

    @Test
    fun testGoldUserPaysAfterFirstHour() {
        // Gold user parked for 2 hours should pay for 1 completed hour at gold rate
        val charge = calculateParkingCharge(120, SubscriptionTier.GOLD, defaultRate)
        assertEquals(80.0, charge, 0.01) // (2-1) completed hours * 80.0 rate
    }

    @Test
    fun testGoldUserPartialHourNotCharged() {
        // Gold user parked for 2h 30m should pay for 1 completed hour (partial hour not charged)
        val charge = calculateParkingCharge(150, SubscriptionTier.GOLD, defaultRate)
        assertEquals(80.0, charge, 0.01) // 1 completed hour * 80.0 rate
    }

    @Test
    fun testPlatinumUserFirstHourFree() {
        // Platinum user parked for 45 minutes should pay nothing (FREE first hour)
        val charge = calculateParkingCharge(45, SubscriptionTier.PLATINUM, defaultRate)
        assertEquals(0.0, charge, 0.01)
    }

    @Test
    fun testPlatinumUserPaysAfterFirstHour() {
        // Platinum user parked for 3 hours should pay for 2 completed hours at platinum rate
        val charge = calculateParkingCharge(180, SubscriptionTier.PLATINUM, defaultRate)
        assertEquals(120.0, charge, 0.01) // (3-1) completed hours * 60.0 rate
    }

    @Test
    fun testPlatinumUserPartialHourNotCharged() {
        // Platinum user parked for 3h 45m should pay for 2 completed hours
        val charge = calculateParkingCharge(225, SubscriptionTier.PLATINUM, defaultRate)
        assertEquals(120.0, charge, 0.01) // 2 completed hours * 60.0 rate
    }

    @Test
    fun testNormalUserZeroMinutesPaysMinimum() {
        // Normal user with 0 minutes should still pay Rs 100 minimum
        val charge = calculateParkingCharge(0, SubscriptionTier.NORMAL, defaultRate)
        assertEquals(100.0, charge, 0.01) // Minimum charge is Rs 100
    }

    @Test
    fun testNormalUserTenHours() {
        // Normal user with 10 hours parking
        val charge = calculateParkingCharge(600, SubscriptionTier.NORMAL, defaultRate) // 10 hours
        assertEquals(1000.0, charge, 0.01) // 10 hours * 100
    }

    @Test
    fun testGoldUserEightHours() {
        // Gold user with 8 hours parking
        val charge = calculateParkingCharge(480, SubscriptionTier.GOLD, defaultRate) // 8 hours
        // (8-1) completed hours * 80.0 = 560.0
        assertEquals(560.0, charge, 0.01)
    }

    @Test
    fun testPlatinumUserTenHours() {
        // Platinum user with 10 hours parking
        val charge = calculateParkingCharge(600, SubscriptionTier.PLATINUM, defaultRate) // 10 hours
        // (10-1) completed hours * 60.0 = 540.0
        assertEquals(540.0, charge, 0.01)
    }

    @Test
    fun testEdgeCaseExactlyOneHour() {
        // Test edge case of exactly 1 hour
        val normalCharge = calculateParkingCharge(60, SubscriptionTier.NORMAL, defaultRate)
        val goldCharge = calculateParkingCharge(60, SubscriptionTier.GOLD, defaultRate)
        val platinumCharge = calculateParkingCharge(60, SubscriptionTier.PLATINUM, defaultRate)
        
        assertEquals(100.0, normalCharge, 0.01)   // Normal pays full hour
        assertEquals(0.0, goldCharge, 0.01)       // Gold gets first hour free
        assertEquals(0.0, platinumCharge, 0.01)   // Platinum gets first hour free
    }

    @Test
    fun testEdgeCaseJustOverOneHour() {
        // Test edge case of just over 1 hour
        val normalCharge = calculateParkingCharge(61, SubscriptionTier.NORMAL, defaultRate)
        val goldCharge = calculateParkingCharge(61, SubscriptionTier.GOLD, defaultRate)
        val platinumCharge = calculateParkingCharge(61, SubscriptionTier.PLATINUM, defaultRate)
        
        assertEquals(200.0, normalCharge, 0.01)   // Normal pays for 2 hours (ceil)
        assertEquals(0.0, goldCharge, 0.01)       // Gold: 0 completed hours (1h free + 1m = 0 completed)
        assertEquals(0.0, platinumCharge, 0.01)   // Platinum: 0 completed hours
    }

    @Test
    fun testEdgeCaseTwoHours() {
        // Test exactly 2 hours
        val normalCharge = calculateParkingCharge(120, SubscriptionTier.NORMAL, defaultRate)
        val goldCharge = calculateParkingCharge(120, SubscriptionTier.GOLD, defaultRate)
        val platinumCharge = calculateParkingCharge(120, SubscriptionTier.PLATINUM, defaultRate)
        
        assertEquals(200.0, normalCharge, 0.01)   // Normal: 2 hours * 100
        assertEquals(80.0, goldCharge, 0.01)      // Gold: 1 completed hour * 80
        assertEquals(60.0, platinumCharge, 0.01)  // Platinum: 1 completed hour * 60
    }

    @Test
    fun testZeroDuration() {
        // Test zero duration parking
        val normalCharge = calculateParkingCharge(0, SubscriptionTier.NORMAL, defaultRate)
        val goldCharge = calculateParkingCharge(0, SubscriptionTier.GOLD, defaultRate)
        val platinumCharge = calculateParkingCharge(0, SubscriptionTier.PLATINUM, defaultRate)
        
        assertEquals(100.0, normalCharge, 0.01)   // Normal: Rs 100 minimum charge
        assertEquals(0.0, goldCharge, 0.01)       // Gold: 1st hour free
        assertEquals(0.0, platinumCharge, 0.01)   // Platinum: 1st hour free
    }

    @Test
    fun testTierPricingConstants() {
        // Verify the pricing constants are correct
        assertEquals(100.0, TierPricing.NORMAL_HOURLY_RATE, 0.01)
        assertEquals(80.0, TierPricing.GOLD_HOURLY_RATE, 0.01)
        assertEquals(60.0, TierPricing.PLATINUM_HOURLY_RATE, 0.01)
        assertEquals(1, TierPricing.FREE_HOURS_GOLD_PLATINUM)
    }

    @Test
    fun testComparisonBetweenTiers() {
        // Compare costs for same duration across tiers
        val duration = 300L // 5 hours
        
        val normalCharge = calculateParkingCharge(duration, SubscriptionTier.NORMAL, defaultRate)
        val goldCharge = calculateParkingCharge(duration, SubscriptionTier.GOLD, defaultRate)
        val platinumCharge = calculateParkingCharge(duration, SubscriptionTier.PLATINUM, defaultRate)
        
        assertEquals(500.0, normalCharge, 0.01)   // 5 hours * 100
        assertEquals(320.0, goldCharge, 0.01)     // (5-1) completed hours * 80
        assertEquals(240.0, platinumCharge, 0.01) // (5-1) completed hours * 60
        
        // Verify savings
        assertEquals(180.0, normalCharge - goldCharge, 0.01)     // Gold saves Rs. 180
        assertEquals(260.0, normalCharge - platinumCharge, 0.01) // Platinum saves Rs. 260
    }

    @Test
    fun testRateTypeVariations() {
        // Test VIP rate (should use base price with multiplier concept)
        val vipRate = defaultRate.copy(rateType = com.example.parktrack.data.model.RateType.VIP)
        val charge = calculateParkingCharge(120, SubscriptionTier.NORMAL, vipRate)
        // VIP rate uses the same tier pricing logic
        assertEquals(200.0, charge, 0.01) // 2 hours * 100
    }

    @Test
    fun testLongDurationWithCompletedHours() {
        // Test 6 hours 30 minutes - completed hours logic
        val duration = 390L // 6h 30m
        
        val normalCharge = calculateParkingCharge(duration, SubscriptionTier.NORMAL, defaultRate)
        val goldCharge = calculateParkingCharge(duration, SubscriptionTier.GOLD, defaultRate)
        val platinumCharge = calculateParkingCharge(duration, SubscriptionTier.PLATINUM, defaultRate)
        
        assertEquals(700.0, normalCharge, 0.01)   // ceil(6.5) = 7 hours * 100
        assertEquals(400.0, goldCharge, 0.01)     // (6.5-1) = 5.5 -> 5 completed hours * 80
        assertEquals(300.0, platinumCharge, 0.01) // (6.5-1) = 5.5 -> 5 completed hours * 60
    }
}