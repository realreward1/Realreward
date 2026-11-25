package com.example.domain.model

import androidx.annotation.DrawableRes
import androidx.annotation.Keep
import com.example.R
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class AppLanguage(val code: String, val label: String) {
    BANGLA("bn", "বাংলা"),
    ENGLISH("en", "EN")
}

data class LocalizedText(val bn: String, val en: String) {
    fun get(lang: AppLanguage): String = if (lang == AppLanguage.BANGLA) bn else en
}

enum class TaskCategory(val id: String, val title: LocalizedText) {
    SURVEY("SURVEY", LocalizedText("সার্ভে টাস্ক", "Survey Tasks")),
    WEBSITE("WEBSITE", LocalizedText("ওয়েবসাইট টাস্ক", "Website Tasks")),
    APP_TESTING("APP_TESTING", LocalizedText("অ্যাপ টেস্টিং", "App Testing")),
    QUICK_QUIZ("QUICK_QUIZ", LocalizedText("কুইক কুইজ", "Quick Quiz")),
    DATA_ENTRY("DATA_ENTRY", LocalizedText("ডেটা এন্ট্রি", "Data Entry")),
    IMAGE_TASKS("IMAGE_TASKS", LocalizedText("ইমেজ টাস্ক", "Image Tasks")),
    ADVERTISEMENT("ADVERTISEMENT", LocalizedText("বিজ্ঞাপন টাস্ক", "Advertisement Tasks")),
    OTHER("OTHER", LocalizedText("অন্যান্য টাস্ক", "Other Tasks"));

    companion object {
        fun fromRaw(raw: String): TaskCategory =
            entries.firstOrNull { it.name == raw || it.id == raw } ?: OTHER
    }

    @DrawableRes
    fun imageRes(): Int = when (this) {
        SURVEY, QUICK_QUIZ, DATA_ENTRY, OTHER -> R.drawable.img_task_survey_1795531054412
        WEBSITE, APP_TESTING, IMAGE_TASKS, ADVERTISEMENT -> R.drawable.img_task_digital_1795531054827
    }
}

enum class ProofStatus(val label: LocalizedText) {
    PENDING_REVIEW(LocalizedText("পর্যালোচনাধীন", "Pending Review")),
    APPROVED(LocalizedText("অনুমোদিত", "Approved")),
    REJECTED(LocalizedText("প্রত্যাখ্যাত", "Rejected"));

    companion object {
        fun fromRaw(raw: String): ProofStatus =
            entries.firstOrNull { it.name == raw } ?: PENDING_REVIEW
    }
}

enum class TransactionType(val label: LocalizedText) {
    TASK_EARNING(LocalizedText("টাস্ক আয়", "Task Earning")),
    WITHDRAWAL(LocalizedText("উত্তোলন", "Withdrawal")),
    BONUS(LocalizedText("বোনাস", "Bonus")),
    ADMIN_ADJUST(LocalizedText("অ্যাডমিন সমন্বয়", "Admin Adjustment"));

    companion object {
        fun fromRaw(raw: String): TransactionType =
            entries.firstOrNull { it.name == raw } ?: TASK_EARNING
    }
}

enum class TransactionStatus(val label: LocalizedText) {
    COMPLETED(LocalizedText("পেমেন্ট সফল", "Payment Success")),
    APPROVED(LocalizedText("অনুমোদিত", "Approved")),
    PENDING(LocalizedText("অপেক্ষমান", "Pending")),
    FAILED(LocalizedText("বাতিল", "Cancelled"));

    companion object {
        fun fromRaw(raw: String): TransactionStatus =
            entries.firstOrNull { it.name == raw } ?: PENDING
    }
}

enum class WithdrawalMethod(val title: LocalizedText, val shortName: String) {
    BKASH_PERSONAL(LocalizedText("বিকাশ পার্সোনাল", "bKash Personal"), "bKash"),
    NAGAD_PERSONAL(LocalizedText("নগদ পার্সোনাল", "Nagad Personal"), "Nagad");

    companion object {
        fun fromRaw(raw: String): WithdrawalMethod =
            entries.firstOrNull { it.name == raw } ?: BKASH_PERSONAL
    }
}

enum class VipTierCode(
    val title: LocalizedText,
    val duration: LocalizedText,
    val bonusPercent: Int
) {
    REGULAR(
        LocalizedText("সাধারণ ইউজার (ফ্রি)", "Regular User (Free)"),
        LocalizedText("মূল টাস্ক রেট (০% বোনাস)", "Standard Task Rate (0% Bonus)"),
        0
    ),
    BRONZE(
        LocalizedText("ব্রোঞ্জ মেম্বারশিপ", "Bronze Membership"),
        LocalizedText("মেয়াদ: ৩০ দিন • +২% টাস্ক বোনাস", "Duration: 30 Days • +2% Task Bonus"),
        2
    ),
    SILVER(
        LocalizedText("সিলভার মেম্বারশিপ", "Silver Membership"),
        LocalizedText("মেয়াদ: ৯০ দিন • +৪% টাস্ক বোনাস", "Duration: 90 Days • +4% Task Bonus"),
        4
    ),
    GOLD(
        LocalizedText("গোল্ড মেম্বারশিপ", "Gold Membership"),
        LocalizedText("মেয়াদ: ১৮০ দিন • +৭% টাস্ক বোনাস", "Duration: 180 Days • +7% Task Bonus"),
        7
    ),
    DIAMOND(
        LocalizedText("ডায়মন্ড মেম্বারশিপ", "Diamond Membership"),
        LocalizedText("মেয়াদ: ৩৬৫ দিন • +১০% টাস্ক বোনাস", "Duration: 365 Days • +10% Task Bonus"),
        10
    );

    companion object {
        fun fromRaw(raw: String): VipTierCode =
            entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) } ?: REGULAR

        fun paidTiers(): List<VipTierCode> = listOf(BRONZE, SILVER, GOLD, DIAMOND)
    }
}

enum class NotificationCategory(val title: LocalizedText) {
    ALL(LocalizedText("সকল", "All")),
    NEW(LocalizedText("নতুন বিজ্ঞপ্তি", "New")),
    TASK_UPDATE(LocalizedText("টাস্ক আপডেট", "Task Updates")),
    WITHDRAWAL_UPDATE(LocalizedText("উত্তোলন আপডেট", "Withdrawal Updates")),
    ACCOUNT_ALERT(LocalizedText("অ্যাকাউন্ট সতর্কতা", "Account Alerts")),
    ANNOUNCEMENT(LocalizedText("অ্যাপ আপডেট ও ঘোষণা", "Updates & Notices"));

    companion object {
        fun fromRaw(raw: String): NotificationCategory =
            entries.firstOrNull { it.name == raw } ?: ANNOUNCEMENT
    }
}

enum class SupportCategory(val title: LocalizedText) {
    TASK_ISSUE(LocalizedText("টাস্ক সংক্রান্ত সমস্যা", "Task Issue")),
    WALLET_WITHDRAWAL(LocalizedText("ওয়ালেট ও উত্তোলন", "Wallet & Withdrawal")),
    ACCOUNT_SECURITY(LocalizedText("অ্যাকাউন্ট ও নিরাপত্তা", "Account & Security")),
    VIP_GENERAL(LocalizedText("ভিআইপি ও সাধারণ জিজ্ঞাসা", "VIP & General"));

    companion object {
        fun fromRaw(raw: String): SupportCategory =
            entries.firstOrNull { it.name == raw } ?: TASK_ISSUE
    }
}

enum class TicketStatus(val label: LocalizedText) {
    OPEN(LocalizedText("খোলা", "Open")),
    IN_REVIEW(LocalizedText("পর্যালোচনাধীন", "In Review")),
    RESOLVED(LocalizedText("সমাধানকৃত", "Resolved"));

    companion object {
        fun fromRaw(raw: String): TicketStatus =
            entries.firstOrNull { it.name == raw } ?: OPEN
    }
}

// --- FIRESTORE DATA MODELS ---

@Keep
@IgnoreExtraProperties
data class UserAccountDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val accountId: String = "",
    val email: String = "",
    val fullName: String = "",
    val username: String = "",
    val mobileNumber: String = "",
    val deviceId: String = "",
    val bkashPersonal: String = "",
    val nagadPersonal: String = "",
    val paymentAccountsLocked: Boolean = false,
    val walletChangeApprovedOnce: Boolean = false,
    val walletResetCount: Long = 0L,
    val securityPasswordHash: String = "",
    val vipTier: String = VipTierCode.REGULAR.name,
    val banned: Boolean = false,
    val permanentlyBanned: Boolean = false,
    val twoFactorEnabled: Boolean = true,
    val loginAlertsEnabled: Boolean = true,
    val registrationBonusClaimed: Boolean = true,
    val availableBalanceBdt: Double = 0.0,
    val pendingBalanceBdt: Double = 0.0,
    val totalEarningsBdt: Double = 0.0,
    val totalWithdrawalsBdt: Double = 0.0,
    val dailyEarningsBdt: Double = 0.0,
    val completedTasksCount: Long = 0L,
    val lastIpAddress: String = "",
    val lastCountryCode: String = "",
    val lastLocation: String = "",
    val vpnDetected: Boolean = false,
    val banReason: String? = null,
    val referralCode: String = "",
    val referredByCode: String? = null,
    val referredByUid: String? = null,
    val referralCount: Long = 0L,
    val referralEarningsBdt: Double = 0.0,
    val referralScamCount: Long = 0L,
    val lastCompletedTaskId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun vipTierEnum(): VipTierCode = VipTierCode.fromRaw(vipTier)
}

@Keep
@IgnoreExtraProperties
data class LeaderboardPublicDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val username: String = "",
    val vipTier: String = VipTierCode.REGULAR.name,
    val tasksCompleted: Long = 0L,
    val earningsBdt: Double = 0.0,
    val availableBalanceBdt: Double? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ReferralCodeDoc(
    @DocumentId val id: String = "",
    val code: String = "",
    val userId: String = "",
    val username: String = "",
    val deviceId: String = "",
    val lastIpAddress: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ReferralRecordDoc(
    @DocumentId val id: String = "",
    val referrerId: String = "",
    val referrerUsername: String = "",
    val referredId: String = "",
    val referredUsername: String = "",
    val referredEmail: String? = null,
    val referralCode: String = "",
    val referredIp: String? = null,
    val referredDeviceId: String? = null,
    val bonusBdt: Double = 0.0,
    val status: String = "CREDITED",
    val scamReason: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FraudAlertDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val email: String? = null,
    val alertType: String = "REFERRAL_SCAM",
    val titleBn: String = "",
    val titleEn: String = "",
    val details: String = "",
    val ipAddress: String? = null,
    val locationLabel: String? = null,
    val resolved: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class RemovedAccountDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String? = null,
    val reason: String? = null,
    @ServerTimestamp val removedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MicroTaskDoc(
    @DocumentId val id: String = "",
    val titleBn: String = "",
    val titleEn: String = "",
    val category: String = TaskCategory.OTHER.name,
    val rewardBdt: Double = 0.0,
    val descriptionBn: String = "",
    val descriptionEn: String = "",
    val instructionsBn: String = "",
    val instructionsEn: String = "",
    val taskLink: String = "",
    val deadlineText: String = "",
    val totalSlots: Long = 0L,
    val availableSlots: Long = 0L,
    val featured: Boolean = false,
    val active: Boolean = true,
    val instantCredit: Boolean = false,
    val verificationCode: String = "",
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun categoryEnum(): TaskCategory = TaskCategory.fromRaw(category)
    fun title(lang: AppLanguage): String = if (lang == AppLanguage.BANGLA) titleBn else titleEn
    fun description(lang: AppLanguage): String = if (lang == AppLanguage.BANGLA) descriptionBn else descriptionEn
    fun instructionSteps(lang: AppLanguage): List<String> {
        val raw = if (lang == AppLanguage.BANGLA) instructionsBn else instructionsEn
        return raw.lines().map { it.trim() }.filter { it.isNotEmpty() }
    }
}

@Keep
@IgnoreExtraProperties
data class TaskProofDoc(
    @DocumentId val id: String = "",
    val taskId: String = "",
    val taskTitle: String = "",
    val userId: String = "",
    val username: String = "",
    val rewardBdt: Double = 0.0,
    val proofNote: String = "",
    val attachmentLabel: String? = null,
    val status: String = ProofStatus.PENDING_REVIEW.name,
    val adminNote: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun statusEnum(): ProofStatus = ProofStatus.fromRaw(status)
}

@Keep
@IgnoreExtraProperties
data class TransactionDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val titleBn: String = "",
    val titleEn: String = "",
    val type: String = TransactionType.TASK_EARNING.name,
    val amountBdt: Double = 0.0,
    val feeBdt: Double? = null,
    val netAmountBdt: Double? = null,
    val status: String = TransactionStatus.COMPLETED.name,
    val referenceId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun typeEnum(): TransactionType = TransactionType.fromRaw(type)
    fun statusEnum(): TransactionStatus = TransactionStatus.fromRaw(status)
    fun title(lang: AppLanguage): String = if (lang == AppLanguage.BANGLA) titleBn else titleEn
}

@Keep
@IgnoreExtraProperties
data class WithdrawalDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val method: String = WithdrawalMethod.BKASH_PERSONAL.name,
    val accountNumber: String = "",
    val amountBdt: Double = 0.0,
    val feeBdt: Double = 10.0,
    val totalDeductedBdt: Double = 0.0,
    val status: String = TransactionStatus.PENDING.name,
    val adminNote: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun methodEnum(): WithdrawalMethod = WithdrawalMethod.fromRaw(method)
    fun statusEnum(): TransactionStatus = TransactionStatus.fromRaw(status)
}

@Keep
@IgnoreExtraProperties
data class VipRequestDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val targetTier: String = VipTierCode.SILVER.name,
    val method: String = WithdrawalMethod.BKASH_PERSONAL.name,
    val senderNumber: String = "",
    val transactionId: String = "",
    val amountBdt: Double = 0.0,
    val status: String = TransactionStatus.PENDING.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun tierEnum(): VipTierCode = VipTierCode.fromRaw(targetTier)
    fun methodEnum(): WithdrawalMethod = WithdrawalMethod.fromRaw(method)
    fun statusEnum(): TransactionStatus = TransactionStatus.fromRaw(status)
}

@Keep
@IgnoreExtraProperties
data class SupportTicketDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val username: String = "",
    val category: String = SupportCategory.TASK_ISSUE.name,
    val subject: String = "",
    val message: String = "",
    val attachmentLabel: String? = null,
    val status: String = TicketStatus.OPEN.name,
    val adminReply: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun categoryEnum(): SupportCategory = SupportCategory.fromRaw(category)
    fun statusEnum(): TicketStatus = TicketStatus.fromRaw(status)
}

@Keep
@IgnoreExtraProperties
data class AnnouncementDoc(
    @DocumentId val id: String = "",
    val category: String = NotificationCategory.ANNOUNCEMENT.name,
    val titleBn: String = "",
    val titleEn: String = "",
    val messageBn: String = "",
    val messageEn: String = "",
    val appVersion: String? = null,
    val updateUrl: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun categoryEnum(): NotificationCategory = NotificationCategory.fromRaw(category)
    fun title(lang: AppLanguage): String = if (lang == AppLanguage.BANGLA) titleBn else titleEn
    fun message(lang: AppLanguage): String = if (lang == AppLanguage.BANGLA) messageBn else messageEn
}

@Keep
@IgnoreExtraProperties
data class AppConfigDoc(
    @DocumentId val id: String = "payment_settings",
    val bkashNumber: String = "01700000000",
    val nagadNumber: String = "01800000000",
    val vipBronzePriceBdt: Double = 150.0,
    val vipSilverPriceBdt: Double = 350.0,
    val vipGoldPriceBdt: Double = 750.0,
    val vipDiamondPriceBdt: Double = 1500.0,
    val latestAppVersion: String = "1.0.0",
    val appDownloadUrl: String = "",
    val updateMessageBn: String? = null,
    val updateMessageEn: String? = null,
    val videoAdsEnabled: Boolean = false,
    val videoAdsComingSoon: Boolean = true,
    val videoAdsRewardBdt: Double = 2.0,
    val videoAdsTitleBn: String? = null,
    val videoAdsTitleEn: String? = null,
    val videoAdsUrl: String? = null,
    val vpnAutoBanEnabled: Boolean = true,
    val foreignIpAutoBanEnabled: Boolean = true,
    val appStopped: Boolean = false,
    val stopMessageBn: String? = null,
    val stopMessageEn: String? = null,
    val stopEndEpochMillis: Long = 0L,
    val dashboardTopNoticeActive: Boolean = false,
    val dashboardTopNoticeBn: String? = null,
    val dashboardTopNoticeEn: String? = null,
    val supportEnabled: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun priceForTier(tier: VipTierCode): Double = when (tier) {
        VipTierCode.REGULAR -> 0.0
        VipTierCode.BRONZE -> vipBronzePriceBdt
        VipTierCode.SILVER -> vipSilverPriceBdt
        VipTierCode.GOLD -> vipGoldPriceBdt
        VipTierCode.DIAMOND -> vipDiamondPriceBdt
    }
}

data class UserFullHistory(
    val user: UserAccountDoc,
    val transactions: List<TransactionDoc> = emptyList(),
    val withdrawals: List<WithdrawalDoc> = emptyList(),
    val proofs: List<TaskProofDoc> = emptyList(),
    val referrals: List<ReferralRecordDoc> = emptyList()
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class FaqItem(
    val question: LocalizedText,
    val answer: LocalizedText
)
