package com.example

import com.example.core.security.AdminSecurity
import com.example.domain.model.AppLanguage
import com.example.ui.components.formatMaintenanceWaitSentence
import com.example.ui.components.formatRemainingCountdown
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun withdrawalFee_followsTieredBkashScheduleUnlimited() {
        assertEquals(0.0, AdminSecurity.calculateWithdrawalFeeBdt(0.0), 0.001)
        assertEquals(10.0, AdminSecurity.calculateWithdrawalFeeBdt(300.0), 0.001)
        assertEquals(10.0, AdminSecurity.calculateWithdrawalFeeBdt(550.0), 0.001)
        assertEquals(10.0, AdminSecurity.calculateWithdrawalFeeBdt(800.0), 0.001)
        assertEquals(20.0, AdminSecurity.calculateWithdrawalFeeBdt(801.0), 0.001)
        assertEquals(20.0, AdminSecurity.calculateWithdrawalFeeBdt(1000.0), 0.001)
        assertEquals(25.0, AdminSecurity.calculateWithdrawalFeeBdt(1001.0), 0.001)
        assertEquals(25.0, AdminSecurity.calculateWithdrawalFeeBdt(1300.0), 0.001)
        assertEquals(30.0, AdminSecurity.calculateWithdrawalFeeBdt(1301.0), 0.001)
        assertEquals(30.0, AdminSecurity.calculateWithdrawalFeeBdt(1800.0), 0.001)
        assertEquals(40.0, AdminSecurity.calculateWithdrawalFeeBdt(1801.0), 0.001)
        assertEquals(40.0, AdminSecurity.calculateWithdrawalFeeBdt(2000.0), 0.001)
        assertEquals(45.0, AdminSecurity.calculateWithdrawalFeeBdt(2001.0), 0.001)
        assertEquals(45.0, AdminSecurity.calculateWithdrawalFeeBdt(2300.0), 0.001)
        assertEquals(50.0, AdminSecurity.calculateWithdrawalFeeBdt(2301.0), 0.001)
        assertEquals(50.0, AdminSecurity.calculateWithdrawalFeeBdt(2800.0), 0.001)
        assertEquals(60.0, AdminSecurity.calculateWithdrawalFeeBdt(2801.0), 0.001)
        assertEquals(60.0, AdminSecurity.calculateWithdrawalFeeBdt(3000.0), 0.001)
        assertEquals(200.0, AdminSecurity.calculateWithdrawalFeeBdt(10000.0), 0.001)
        assertEquals(1000.0, AdminSecurity.calculateWithdrawalFeeBdt(50000.0), 0.001)
        assertEquals(3000.0, AdminSecurity.calculateWithdrawalFeeBdt(150000.0), 0.001)
    }

    @Test
    fun coreSettingsAndOwnerPasscodeVerification() {
        assertTrue(AdminSecurity.verifyCoreSettingsPassword("admin166523"))
        assertFalse(AdminSecurity.verifyCoreSettingsPassword("wrongpass"))
        assertTrue(AdminSecurity.isValidBdMobile("01712345678"))
        assertFalse(AdminSecurity.isValidBdMobile("01212345678"))
    }

    @Test
    fun vipBonus_scalesConservativelyAcrossAllTiers() {
        assertEquals(0, AdminSecurity.vipBonusPercentForTier("REGULAR"))
        assertEquals(2, AdminSecurity.vipBonusPercentForTier("BRONZE"))
        assertEquals(4, AdminSecurity.vipBonusPercentForTier("SILVER"))
        assertEquals(7, AdminSecurity.vipBonusPercentForTier("GOLD"))
        assertEquals(10, AdminSecurity.vipBonusPercentForTier("DIAMOND"))

        assertEquals(100.0, AdminSecurity.calculateTaskRewardWithVip(100.0, "REGULAR"), 0.001)
        assertEquals(102.0, AdminSecurity.calculateTaskRewardWithVip(100.0, "BRONZE"), 0.001)
        assertEquals(104.0, AdminSecurity.calculateTaskRewardWithVip(100.0, "SILVER"), 0.001)
        assertEquals(107.0, AdminSecurity.calculateTaskRewardWithVip(100.0, "GOLD"), 0.001)
        assertEquals(110.0, AdminSecurity.calculateTaskRewardWithVip(100.0, "DIAMOND"), 0.001)

        assertEquals(10.2, AdminSecurity.calculateTaskRewardWithVip(10.0, "BRONZE"), 0.001)
        assertEquals(10.4, AdminSecurity.calculateTaskRewardWithVip(10.0, "SILVER"), 0.001)
        assertEquals(10.7, AdminSecurity.calculateTaskRewardWithVip(10.0, "GOLD"), 0.001)
        assertEquals(11.0, AdminSecurity.calculateTaskRewardWithVip(10.0, "DIAMOND"), 0.001)
    }

    @Test
    fun maintenanceCountdown_formatsHoursAndMinutesCleanly() {
        val now = 1_000_000L
        assertEquals("01:30:15", formatRemainingCountdown(now + (5415L * 1000L), now))
        assertEquals(
            "দয়া করে ১ ঘণ্টা ৩০ মিনিট ১৫ সেকেন্ড অপেক্ষা করুন",
            formatMaintenanceWaitSentence(now + (5415L * 1000L), now, AppLanguage.BANGLA)
        )
        assertEquals(
            "দয়া করে ১৫ মিনিট অপেক্ষা করুন",
            formatMaintenanceWaitSentence(now + (900L * 1000L), now, AppLanguage.BANGLA)
        )
        assertEquals(
            "Please wait 1 hr 30 min 15 sec",
            formatMaintenanceWaitSentence(now + (5415L * 1000L), now, AppLanguage.ENGLISH)
        )
    }
}

