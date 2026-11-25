package com.example.ui.i18n

import androidx.compose.runtime.compositionLocalOf

interface AppStrings {
    val brandName: String
    val tagline: String
    val liveBadge: String
    val livePlatformBanner: String
    val login: String
    val register: String
    val logout: String
    val back: String
    val save: String
    val cancel: String
    val okConfirm: String
    val submit: String
    val retry: String
    val refresh: String
    val searchPlaceholder: String
    val loadingLabel: String
    val errorTitle: String
    val errorSubtitle: String
    val emptyTitle: String
    val emptySubtitle: String

    // Bottom Navigation
    val navHome: String
    val navTasks: String
    val navWallet: String
    val navVip: String
    val navProfile: String

    // Public Homepage & Google Sign-In
    val heroBadge: String
    val heroTitle: String
    val heroSubtitle: String
    val signInWithGoogle: String
    val signingInLabel: String
    val signupBonusBadge: String
    val singleAccountRuleShort: String
    val howItWorksTitle: String
    val howStep1Title: String
    val howStep1Desc: String
    val howStep2Title: String
    val howStep2Desc: String
    val howStep3Title: String
    val howStep3Desc: String
    val howStep4Title: String
    val howStep4Desc: String
    val categoriesTitle: String
    val platformFeaturesTitle: String
    val feature1Title: String
    val feature1Desc: String
    val feature2Title: String
    val feature2Desc: String
    val feature3Title: String
    val feature3Desc: String
    val feature4Title: String
    val feature4Desc: String
    val whyChooseUsTitle: String
    val why1: String
    val why2: String
    val why3: String
    val why4: String
    val rulesSectionTitle: String
    val rule1Title: String
    val rule1Desc: String
    val rule2Title: String
    val rule2Desc: String
    val rule3Title: String
    val rule3Desc: String
    val rule4Title: String
    val rule4Desc: String
    val faqTitle: String
    val contactTitle: String
    val contactSubtitle: String
    val footerRights: String
    val aboutUs: String
    val termsConditions: String
    val privacyPolicy: String

    // Registration (First-time Google Auth Account Lock)
    val registerTitle: String
    val registerSubtitle: String
    val registerBonusHighlight: String
    val registerSingleAccountNotice: String
    val fullNameLabel: String
    val usernameLabel: String
    val mobileLabel: String
    val referralCodeInputLabel: String
    val passwordLabel: String
    val confirmPasswordLabel: String
    val acceptTermsLabel: String
    val viewTerms: String
    val completeRegistration: String
    val switchGoogleAccount: String

    // Validation & Policy Errors
    val errFullName: String
    val errUsername: String
    val errBdMobile: String
    val errAcceptTerms: String
    val errDuplicateMobile: String
    val errDuplicateDevice: String
    val errInvalidReferralCode: String
    val errAccountBannedTitle: String
    val errAccountBannedBody: String
    val accountRemovedTitle: String
    val accountRemovedBody: String
    val maintenanceTitle: String
    val maintenanceRemainingLabel: String
    val maintenanceDefaultMsg: String

    // Dashboard & Referral Program
    val welcomePrefix: String
    val dashboardSubtitle: String
    val dashboardExtraNoticeTag: String
    val availableBalance: String
    val pendingBalance: String
    val totalEarnings: String
    val totalWithdrawals: String
    val completedTasks: String
    val dailyEarnings: String
    val quickActions: String
    val leaderboard: String
    val notifications: String
    val supportCenter: String
    val withdrawNow: String
    val latestNotices: String
    val recentTransactions: String
    val featuredTasks: String
    val viewAll: String
    val appUpdateBadge: String
    val referralCardTitle: String
    val referralCardSubtitle: String
    val referralCodeLabel: String
    val copyReferralCode: String
    val referralCodeCopied: String
    val shareReferralButton: String
    val referralCountLabel: String
    val referralEarningsLabel: String

    // Tasks & Task Details
    val tasksPageTitle: String
    val allCategories: String
    val rewardLabel: String
    val deadlineLabel: String
    val slotsLabel: String
    val viewTask: String
    val taskAvailableLabel: String
    val taskDetailsTitle: String
    val taskInstructionsTitle: String
    val taskLinkTitle: String
    val copyLink: String
    val linkCopied: String
    val openExternalLink: String
    val submitProofTitle: String
    val proofInputPlaceholder: String
    val attachScreenshot: String
    val screenshotSelected: String
    val submitProofButton: String
    val proofAlreadySubmitted: String
    val proofSubmittedSuccess: String
    val errProofEmpty: String
    val errNoSlots: String

    // Wallet & Withdrawal
    val walletTitle: String
    val transactionHistory: String
    val filterAll: String
    val filterEarnings: String
    val filterWithdrawals: String
    val filterPending: String
    val withdrawalTitle: String
    val minWithdrawalNotice: String
    val withdrawalFeeBreakdown: String
    val selectMethod: String
    val accountNumberLabel: String
    val withdrawalAmountLabel: String
    val withdrawalFeeLabel: String
    val withdrawalTotalDeductionLabel: String
    val submitWithdrawalRequest: String
    val withdrawalHistory: String
    val errWithdrawalMin: String
    val errWithdrawalInsufficientWithFee: String
    val withdrawalSubmittedSuccess: String

    // VIP Membership
    val vipTitle: String
    val vipSubtitle: String
    val currentTierLabel: String
    val adminPaymentNumbersTitle: String
    val copyNumber: String
    val numberCopied: String
    val upgradeVipTitle: String
    val senderNumberLabel: String
    val trxIdLabel: String
    val submitVipRequest: String
    val vipRequestHistory: String
    val vipRequestSubmitted: String
    val errTrxId: String

    // Leaderboard
    val leaderboardTitle: String
    val leaderboardSubtitle: String
    val tasksDoneShort: String

    // Profile
    val profileTitle: String
    val accountIdLabel: String
    val editProfile: String
    val paymentAccountSettings: String
    val securitySettings: String
    val languageSettings: String
    val twoFactorAuth: String
    val loginAlerts: String
    val bkashPersonalLabel: String
    val nagadPersonalLabel: String
    val setupWalletOnceButton: String
    val paymentSetupWarning: String
    val paymentLockedBadge: String
    val paymentAlreadyLockedError: String
    val setupWalletPromptForWithdraw: String
    val changePasswordTitle: String
    val oldPasswordLabel: String
    val newPasswordLabel: String
    val passwordUpdatedSuccess: String
    val errPasswordLength: String
    val errPasswordMismatch: String
    val errWrongOldPassword: String
    val profileUpdatedSuccess: String
    val paymentSavedSuccess: String
    val ownerAdminEntryTitle: String
    val ownerAdminEntrySubtitle: String

    // Notifications
    val notificationsTitle: String

    // Support Center (Direct Admin Chat, No Gmail)
    val supportTitle: String
    val supportDirectAdminNotice: String
    val createTicketTitle: String
    val issueCategoryLabel: String
    val subjectLabel: String
    val messageLabel: String
    val attachFileButton: String
    val submitTicketButton: String
    val supportHistoryTitle: String
    val ticketSubmittedSuccess: String
    val adminReplyLabel: String
    val errTicketSubject: String
    val errTicketMessage: String

    // Legal
    val legalTitle: String

    // Owner-Only Admin Panel
    val adminLockTitle: String
    val adminLockSubtitle: String
    val adminPasscodeLabel: String
    val adminUnlockButton: String
    val adminWrongPasscode: String
    val adminPanelTitle: String
    val adminConfirmTitle: String
    val adminTabOverview: String
    val adminTabUsers: String
    val adminTabTasks: String
    val adminTabProofs: String
    val adminTabFinance: String
    val adminTabSupport: String
    val adminTabNotices: String
    val adminTabCoreSettings: String
    val adminCoreLockTitle: String
    val adminCoreLockSubtitle: String
    val adminBanUser: String
    val adminUnbanUser: String
    val adminRemoveAccount: String
    val adminViewHistory: String
    val adminBannedTag: String
    val adminUpgradeVip: String
    val adminAdjustBalance: String
    val adminCreateTask: String
    val adminDeleteTask: String
    val adminApprove: String
    val adminReject: String
    val adminSavePaymentConfig: String
    val adminSendUpdateNotice: String
    val adminReplyTicket: String
    val adminBanFromSupport: String
    val adminPaymentSuccessButton: String

    // App Download & Sound Notification System
    val appDownloadTitle: String
    val appDownloadSubtitle: String
    val downloadAppButton: String
    val shareAppButton: String
    val currentVersionLabel: String
    val downloadLinkUnavailable: String
    val notificationSoundLabel: String
    val notificationSoundDesc: String
    val enableNotificationsButton: String
    val notificationsEnabledTag: String
    val testNotificationSoundButton: String
    val testNotificationTitle: String
    val testNotificationBody: String

    // Device Lock, VPN/Geo Auto-Ban, & Lifetime Video Ads Folder
    val deviceLockedTitle: String
    val deviceLockedBody: String
    val banReasonVpn: String
    val banReasonForeignIp: String
    val banReasonAdmin: String
    val videoAdsFolderTitle: String
    val videoAdsFolderSubtitle: String
    val videoAdsComingSoonTag: String
    val videoAdsPausedTag: String
    val videoAdsLiveTag: String
    val videoAdsWatchButton: String

    // Wallet Uniqueness, 1-Time Admin Wallet Reset, Support Multi-Select & Toggle
    val walletDuplicateNumberError: String
    val walletResetApprovedNotice: String
    val walletLockedChangeViaSupport: String
    val requestWalletChangeButton: String
    val supportWalletChangePreset: String
    val supportDisabledTitle: String
    val supportDisabledBody: String
    val adminSelectAll: String
    val adminDeselectAll: String
    val adminCustomSelect: String
    val adminDeleteSelected: String
    val adminAllowWalletChangeOnce: String
    val adminWalletAlreadyUnlockedTag: String
    val adminSupportOptionToggle: String
    val adminSelectedCountLabel: String
    val adminRemoveWithdrawal: String
    val adminRemoveAllPaid: String
    val leaderboardWalletLabel: String
    val leaderboardLifetimePrefix: String
    val balanceUpdatedAlertTitle: String
    val proofInstantCreditSuccess: String
    val taskVerificationCodeLabel: String
    val errInvalidTaskVerification: String
    val instantCreditBadge: String
    val transactionFeeDeductedLabel: String
    val vipBonusBadgePrefix: String
    val vipTaskBonusRateLabel: String
    val vipRateComparisonTitle: String
    val vipRateComparisonDesc: String
    val vipBaseRateNote: String
}

val LocalStrings = compositionLocalOf<AppStrings> { BnStrings }
