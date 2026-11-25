package com.example.features.dashboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.core.notifications.AppNotificationHelper
import com.example.core.security.AdminSecurity
import com.example.domain.model.*
import com.example.ui.components.*
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.*
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    firebaseUser: FirebaseUser,
    user: UserAccountDoc,
    language: AppLanguage,
    tasksState: UiState<List<MicroTaskDoc>>,
    proofsState: UiState<List<TaskProofDoc>>,
    transactionsState: UiState<List<TransactionDoc>>,
    announcementsState: UiState<List<AnnouncementDoc>>,
    appConfig: AppConfigDoc,
    onToggleLanguage: () -> Unit,
    onRetry: () -> Unit,
    onNavigateWithdraw: () -> Unit,
    onNavigateLeaderboard: () -> Unit,
    onNavigateNotifications: () -> Unit,
    onNavigateSupport: () -> Unit,
    onNavigateAllTasks: () -> Unit,
    onNavigateWallet: () -> Unit,
    onOpenTask: (String) -> Unit,
    onOpenOwnerAdmin: () -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val isOwner = AdminSecurity.isVerifiedOwner(firebaseUser)
    val announcements = (announcementsState as? UiState.Success)?.data.orEmpty()
    val transactions = (transactionsState as? UiState.Success)?.data.orEmpty()
    val tasks = (tasksState as? UiState.Success)?.data.orEmpty()
    val proofsByTaskId = ((proofsState as? UiState.Success)?.data.orEmpty())
        .associateBy { it.taskId }
    val resolvedDownloadUrl = appConfig.appDownloadUrl.trim().ifBlank {
        announcements.firstOrNull { !it.updateUrl.isNullOrBlank() }?.updateUrl.orEmpty()
    }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(appConfig.stopEndEpochMillis, appConfig.appStopped) {
        if (appConfig.appStopped && appConfig.stopEndEpochMillis > 0L) {
            while (true) {
                nowMillis = System.currentTimeMillis()
                kotlinx.coroutines.delay(1000L)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            RealRewardTopBar(
                title = strings.navHome,
                language = language,
                onToggleLanguage = onToggleLanguage,
                noticeCount = announcements.size,
                onOpenNotifications = onNavigateNotifications
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Extra Dashboard Top Notice uploaded by Admin
            val extraNoticeText = if (language == AppLanguage.BANGLA) {
                appConfig.dashboardTopNoticeBn.orEmpty()
            } else {
                appConfig.dashboardTopNoticeEn.orEmpty()
            }.ifBlank { appConfig.dashboardTopNoticeBn ?: appConfig.dashboardTopNoticeEn.orEmpty() }

            if (appConfig.dashboardTopNoticeActive && extraNoticeText.isNotBlank()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dashboard_extra_top_notice"),
                        shape = RoundedCornerShape(16.dp),
                        color = GoldSoft,
                        border = BorderStroke(1.5.dp, GoldBright)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(GoldBright.copy(alpha = 0.28f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Campaign, contentDescription = null, tint = GoldDarkContainer)
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = strings.dashboardExtraNoticeTag,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = GoldDarkContainer
                                )
                                Text(
                                    text = extraNoticeText,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = NavyDeep
                                )
                            }
                        }
                    }
                }
            }

            if (appConfig.appStopped) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = StatusErrorSoft,
                        border = BorderStroke(1.dp, StatusError.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Engineering, contentDescription = null, tint = StatusError)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(strings.maintenanceTitle, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = StatusError)
                                if (appConfig.stopEndEpochMillis > 0L) {
                                    Text(
                                        text = "${strings.maintenanceRemainingLabel} ${formatRemainingCountdown(appConfig.stopEndEpochMillis, nowMillis)}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = NavyDeep
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Brush.linearGradient(colors = listOf(NavyDeep, NavyPrimary, NavySurface)))
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${strings.welcomePrefix} ${user.fullName}",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = strings.dashboardSubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.78f)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = GoldBright.copy(alpha = 0.22f),
                                border = BorderStroke(1.dp, GoldBright)
                            ) {
                                Text(
                                    text = user.vipTierEnum().title.get(language),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = GoldSoft,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
                                Text(
                                    text = strings.availableBalance,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = EmeraldSoft
                                )
                                Text(
                                    text = AdminSecurity.formatBdt(user.availableBalanceBdt),
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = Color.White
                                )
                            }
                            Button(
                                onClick = onNavigateWithdraw,
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("dash_withdraw_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White)
                            ) {
                                Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.withdrawNow)
                            }
                        }
                    }
                }
            }

            if (isOwner) {
                item {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenOwnerAdmin)
                            .testTag("owner_admin_dashboard_banner"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = GoldSoft)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = GoldDarkContainer)
                                Column {
                                    Text(strings.ownerAdminEntryTitle, style = MaterialTheme.typography.titleSmall, color = GoldDarkContainer)
                                    Text(strings.ownerAdminEntrySubtitle, style = MaterialTheme.typography.bodySmall, color = GoldDarkContainer.copy(alpha = 0.85f))
                                }
                            }
                            Icon(Icons.Default.LockOpen, contentDescription = null, tint = GoldDarkContainer)
                        }
                    }
                }
            }

            // Refer & Earn ৳15 Bonus Card (IP + Device Anti-Scam Protected)
            item {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_referral_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.GroupAdd, contentDescription = null, tint = EmeraldPrimary)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.referralCardTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = strings.referralCardSubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = EmeraldSoft,
                            border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(strings.referralCodeLabel, style = MaterialTheme.typography.labelSmall, color = EmeraldDarkContainer)
                                    Text(
                                        text = user.referralCode.ifBlank { "-" },
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontFamily = PlusJakartaSansFontFamily,
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = NavyDeep
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = NavyDeep
                                    ) {
                                        Text(
                                            text = "${strings.referralCountLabel}: ${user.referralCount}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = EmeraldPrimary
                                    ) {
                                        Text(
                                            text = AdminSecurity.formatBdt(user.referralEarningsBdt),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("referral_code", user.referralCode))
                                    scope.launch { snackbarHostState.showSnackbar(strings.referralCodeCopied) }
                                },
                                enabled = user.referralCode.isNotBlank(),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("copy_referral_code_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.copyReferralCode, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Button(
                                onClick = {
                                    val inviteBody = if (language == AppLanguage.BANGLA) {
                                        "${strings.brandName} অ্যাপে যোগ দিন এবং আমার রেফার কোড (${user.referralCode}) ব্যবহার করে ৳১০ বোনাস পান! ${resolvedDownloadUrl}".trim()
                                    } else {
                                        "Join ${strings.brandName} with my referral code (${user.referralCode}) and claim your ৳10 bonus! ${resolvedDownloadUrl}".trim()
                                    }
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, strings.brandName)
                                        putExtra(Intent.EXTRA_TEXT, inviteBody)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(shareIntent, strings.shareReferralButton)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                },
                                enabled = user.referralCode.isNotBlank(),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("share_referral_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.shareReferralButton, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }

            // App update banner controlled by Admin
            val updateMsg = if (language == AppLanguage.BANGLA) appConfig.updateMessageBn else appConfig.updateMessageEn
            if (!updateMsg.isNullOrBlank()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onNavigateNotifications),
                        shape = RoundedCornerShape(14.dp),
                        color = StatusInfoSoft,
                        border = BorderStroke(1.dp, StatusInfo.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = StatusInfo)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${strings.appUpdateBadge} • v${appConfig.latestAppVersion}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = StatusInfo
                                )
                                Text(updateMsg, style = MaterialTheme.typography.bodySmall, color = NavyDeep)
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FintechStatCard(
                            title = strings.pendingBalance,
                            value = AdminSecurity.formatBdt(user.pendingBalanceBdt),
                            icon = Icons.Default.HourglassTop,
                            accentColor = GoldAccent,
                            modifier = Modifier.weight(1f)
                        )
                        FintechStatCard(
                            title = strings.totalEarnings,
                            value = AdminSecurity.formatBdt(user.totalEarningsBdt),
                            icon = Icons.Default.TrendingUp,
                            accentColor = EmeraldPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FintechStatCard(
                            title = strings.totalWithdrawals,
                            value = AdminSecurity.formatBdt(user.totalWithdrawalsBdt),
                            icon = Icons.Default.AccountBalance,
                            accentColor = NavyPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        FintechStatCard(
                            title = strings.completedTasks,
                            value = "${user.completedTasksCount}",
                            subtitle = "${strings.dailyEarnings}: ${AdminSecurity.formatBdt(user.dailyEarningsBdt)}",
                            icon = Icons.Default.TaskAlt,
                            accentColor = EmeraldBright,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(title = strings.quickActions)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickActionButton(Icons.Default.Payments, strings.withdrawNow, EmeraldPrimary, onNavigateWithdraw, Modifier.weight(1f))
                        QuickActionButton(Icons.Default.EmojiEvents, strings.leaderboard, GoldAccent, onNavigateLeaderboard, Modifier.weight(1f))
                        QuickActionButton(Icons.Default.Notifications, strings.notifications, NavyPrimary, onNavigateNotifications, Modifier.weight(1f))
                        if (appConfig.supportEnabled) {
                            QuickActionButton(Icons.Default.SupportAgent, strings.supportCenter, StatusInfo, onNavigateSupport, Modifier.weight(1f))
                        }
                    }
                }
            }

            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, tint = EmeraldPrimary)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.appDownloadTitle,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "${strings.currentVersionLabel}: v${BuildConfig.VERSION_NAME} • Latest: v${appConfig.latestAppVersion}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (resolvedDownloadUrl.isNotBlank()) {
                                Button(
                                    onClick = { AppNotificationHelper.openDownloadOrUpdateUrl(context, resolvedDownloadUrl) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("dash_download_app_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White)
                                ) {
                                    Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(strings.downloadAppButton, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    AppNotificationHelper.shareAppDownload(
                                        context = context,
                                        url = resolvedDownloadUrl.ifBlank { null },
                                        brandName = strings.brandName,
                                        latestVersion = appConfig.latestAppVersion
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("dash_share_app_button"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.shareAppButton, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }

            if (announcements.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = strings.latestNotices,
                        actionText = strings.viewAll,
                        onActionClick = onNavigateNotifications
                    )
                }
                items(announcements.take(2), key = { it.id }) { notice ->
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onNavigateNotifications),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Campaign, contentDescription = null, tint = EmeraldPrimary)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(notice.title(language), style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = notice.message(language),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader(
                    title = strings.recentTransactions,
                    actionText = strings.viewAll,
                    onActionClick = onNavigateWallet
                )
            }
            if (transactions.isEmpty()) {
                item {
                    EmptyStateView(
                        title = strings.emptyTitle,
                        subtitle = strings.emptySubtitle
                    )
                }
            } else {
                items(transactions.take(3), key = { it.id }) { txn ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(txn.title(language), style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "${txn.statusEnum().label.get(language)} • ${formatTimestamp(txn.createdAt, language)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val positive = txn.amountBdt >= 0
                            Text(
                                text = (if (positive) "+" else "") + AdminSecurity.formatBdt(txn.amountBdt),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (positive) EmeraldPrimary else StatusError
                            )
                        }
                    }
                }
            }

            item {
                SectionHeader(
                    title = strings.featuredTasks,
                    actionText = strings.viewAll,
                    onActionClick = onNavigateAllTasks
                )
            }

            when (tasksState) {
                is UiState.Loading -> item { LoadingStateView() }
                is UiState.Error -> item { ErrorStateView(message = tasksState.message, onRetry = onRetry) }
                is UiState.Success -> {
                    val featured = tasks.filter { it.featured }.ifEmpty { tasks.take(3) }
                    if (featured.isEmpty()) {
                        item { EmptyStateView() }
                    } else {
                        items(featured, key = { it.id }) { task ->
                            TaskListCard(
                                task = task,
                                userProofStatus = proofsByTaskId[task.id]?.statusEnum(),
                                language = language,
                                onViewTask = { onOpenTask(task.id) },
                                userVipTier = user.vipTierEnum()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .heightIn(min = 76.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = label, tint = accent)
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
