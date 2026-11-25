package com.example.features.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.*
import com.example.features.admin.tabs.*
import com.example.ui.components.RealRewardTopBar
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.*
import com.example.ui.viewmodel.AdminViewModel
import kotlinx.coroutines.launch

private data class AdminFolderItem(
    val title: String,
    val icon: ImageVector,
    val badgeCount: Int = 0
)

@Composable
fun AdminPanelScreen(
    adminViewModel: AdminViewModel,
    language: AppLanguage,
    onToggleLanguage: () -> Unit,
    onBack: () -> Unit,
    onLockAdmin: () -> Unit
) {
    val strings = LocalStrings.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val usersState by adminViewModel.allUsersState.collectAsStateWithLifecycle()
    val fraudAlertsState by adminViewModel.fraudAlertsState.collectAsStateWithLifecycle()
    val tasksState by adminViewModel.allTasksState.collectAsStateWithLifecycle()
    val proofsState by adminViewModel.allProofsState.collectAsStateWithLifecycle()
    val withdrawalsState by adminViewModel.allWithdrawalsState.collectAsStateWithLifecycle()
    val vipRequestsState by adminViewModel.allVipRequestsState.collectAsStateWithLifecycle()
    val ticketsState by adminViewModel.allSupportTicketsState.collectAsStateWithLifecycle()
    val announcementsState by adminViewModel.announcementsState.collectAsStateWithLifecycle()
    val appConfig by adminViewModel.appConfigState.collectAsStateWithLifecycle()
    val isBusy by adminViewModel.isBusy.collectAsStateWithLifecycle()

    val unresolvedFraudCount = ((fraudAlertsState as? UiState.Success)?.data.orEmpty()).count { !it.resolved }
    val pendingProofsCount = ((proofsState as? UiState.Success)?.data.orEmpty()).count { it.statusEnum() == ProofStatus.PENDING_REVIEW }
    val activeWithdrawalsCount = ((withdrawalsState as? UiState.Success)?.data.orEmpty()).count {
        it.statusEnum() == TransactionStatus.PENDING || it.statusEnum() == TransactionStatus.APPROVED
    }
    val pendingVipCount = ((vipRequestsState as? UiState.Success)?.data.orEmpty()).count { it.statusEnum() == TransactionStatus.PENDING }
    val openTicketCount = ((ticketsState as? UiState.Success)?.data.orEmpty()).count { it.statusEnum() != TicketStatus.RESOLVED }

    var selectedFolder by remember { mutableIntStateOf(0) }
    val folders = listOf(
        AdminFolderItem(strings.adminTabOverview, Icons.Default.Dashboard, unresolvedFraudCount),
        AdminFolderItem(strings.adminTabUsers, Icons.Default.People),
        AdminFolderItem(strings.adminTabTasks, Icons.Default.FolderSpecial),
        AdminFolderItem(strings.adminTabProofs, Icons.Default.FactCheck, pendingProofsCount),
        AdminFolderItem(strings.adminTabFinance, Icons.Default.Payments, activeWithdrawalsCount + pendingVipCount),
        AdminFolderItem(strings.adminTabSupport, Icons.Default.SupportAgent, openTicketCount),
        AdminFolderItem(strings.adminTabNotices, Icons.Default.Campaign),
        AdminFolderItem(strings.adminTabCoreSettings, Icons.Default.SettingsApplications)
    )

    val notifyResult: (Result<Unit>, String) -> Unit = { res, okMsg ->
        scope.launch {
            res.fold(
                onSuccess = { snackbarHostState.showSnackbar(okMsg) },
                onFailure = { snackbarHostState.showSnackbar(it.localizedMessage ?: strings.errorTitle) }
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            RealRewardTopBar(
                title = strings.adminPanelTitle,
                language = language,
                onToggleLanguage = onToggleLanguage,
                onBack = onBack
            )
        },
        floatingActionButton = {
            SmallFloatingActionButton(
                onClick = onLockAdmin,
                modifier = Modifier.testTag("admin_lock_fab")
            ) {
                Icon(Icons.Default.Lock, contentDescription = "Lock Admin")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = folders[selectedFolder].title,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = EmeraldPrimary
                )
                TextButton(onClick = { adminViewModel.retry() }) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(strings.refresh)
                }
            }

            // 8 Categorized Admin Folders Bar
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(folders) { idx, folder ->
                    val selected = selectedFolder == idx
                    Surface(
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clickable { selectedFolder = idx }
                            .testTag("admin_folder_tab_$idx"),
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) NavyDeep else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (selected) GoldBright else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = folder.icon,
                                contentDescription = folder.title,
                                tint = if (selected) GoldBright else EmeraldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = folder.title,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            if (folder.badgeCount > 0) {
                                Badge(containerColor = if (selected) EmeraldPrimary else StatusError) {
                                    Text("${folder.badgeCount}", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            when (selectedFolder) {
                0 -> AdminOverviewFolder(
                    usersState = usersState,
                    tasksState = tasksState,
                    proofsState = proofsState,
                    withdrawalsState = withdrawalsState,
                    vipRequestsState = vipRequestsState,
                    ticketsState = ticketsState,
                    fraudAlertsState = fraudAlertsState,
                    language = language,
                    isBusy = isBusy,
                    onRetry = { adminViewModel.retry() },
                    onBanUser = { uid, reason ->
                        adminViewModel.toggleUserBan(uid, ban = true, permanent = true, reason = reason) {
                            notifyResult(it, "User permanently banned")
                        }
                    },
                    onResolveFraudAlert = { alertId ->
                        adminViewModel.resolveFraudAlert(alertId) { notifyResult(it, "Fraud alert resolved") }
                    }
                )
                1 -> AdminUsersFolder(
                    usersState = usersState,
                    language = language,
                    isBusy = isBusy,
                    onRetry = { adminViewModel.retry() },
                    onToggleBan = { uid, ban, perm, reason ->
                        adminViewModel.toggleUserBan(uid, ban, perm, reason) {
                            notifyResult(it, if (ban) "User permanently banned" else "User unbanned")
                        }
                    },
                    onRemoveAccount = { user, reason ->
                        adminViewModel.permanentlyRemoveUser(user, reason) {
                            notifyResult(it, "Account permanently removed & blocked")
                        }
                    },
                    onSetVip = { uid, tier ->
                        adminViewModel.setUserVipTier(uid, tier) { notifyResult(it, "VIP updated to ${tier.name}") }
                    },
                    onAdjustBalance = { user, delta, reason ->
                        adminViewModel.adjustUserBalance(user, delta, reason) { notifyResult(it, "Balance updated") }
                    },
                    onAllowWalletChangeOnce = { uid ->
                        adminViewModel.allowUserWalletChangeOnce(uid) {
                            notifyResult(it, "1-time wallet change permission granted")
                        }
                    },
                    onLoadUserHistory = { user, cb ->
                        adminViewModel.loadUserFullHistory(user) { res ->
                            res.onFailure { err ->
                                scope.launch { snackbarHostState.showSnackbar(err.localizedMessage ?: strings.errorTitle) }
                            }
                            cb(res)
                        }
                    }
                )
                2 -> AdminTasksAdsFolder(
                    appConfig = appConfig,
                    tasksState = tasksState,
                    language = language,
                    isBusy = isBusy,
                    onRetry = { adminViewModel.retry() },
                    onSaveConfig = { updated ->
                        adminViewModel.saveFullAppConfig(updated) { notifyResult(it, "Video Ads config saved") }
                    },
                    onCreateTask = { titleBn, titleEn, cat, reward, descBn, descEn, instBn, instEn, link, deadline, slots, featured, instantCredit, verificationCode ->
                        adminViewModel.createTask(
                            titleBn, titleEn, cat, reward, descBn, descEn, instBn, instEn, link, deadline, slots, featured, instantCredit, verificationCode
                        ) { notifyResult(it, "Task published") }
                    },
                    onToggleActive = { id, active ->
                        adminViewModel.toggleTaskActive(id, active) { notifyResult(it, "Task status updated") }
                    },
                    onDeleteTask = { id ->
                        adminViewModel.deleteTask(id) { notifyResult(it, "Task deleted") }
                    }
                )
                3 -> AdminProofsFolder(
                    proofsState = proofsState,
                    language = language,
                    isBusy = isBusy,
                    onRetry = { adminViewModel.retry() },
                    onReviewProof = { proof, approve, note ->
                        adminViewModel.reviewProof(proof, approve, note) {
                            notifyResult(it, if (approve) "Proof approved & balance credited" else "Proof rejected")
                        }
                    }
                )
                4 -> AdminWithdrawalsVipFolder(
                    withdrawalsState = withdrawalsState,
                    vipRequestsState = vipRequestsState,
                    language = language,
                    isBusy = isBusy,
                    onRetry = { adminViewModel.retry() },
                    onUpdateWithdrawalStage = { rec, stage, note ->
                        adminViewModel.updateWithdrawalStage(rec, stage, note) {
                            val msg = when (stage) {
                                TransactionStatus.APPROVED -> "Withdrawal approved"
                                TransactionStatus.COMPLETED -> "Marked Payment Success"
                                else -> "Withdrawal rejected & refunded"
                            }
                            notifyResult(it, msg)
                        }
                    },
                    onDeleteWithdrawal = { withdrawalId ->
                        adminViewModel.deleteWithdrawal(withdrawalId) {
                            notifyResult(it, "Withdrawal removed from queue")
                        }
                    },
                    onDeleteCompletedWithdrawals = { ids ->
                        adminViewModel.deleteCompletedWithdrawals(ids) {
                            notifyResult(it, "Paid withdrawals removed from queue")
                        }
                    },
                    onReviewVip = { req, approve ->
                        adminViewModel.reviewVipRequest(req, approve) {
                            notifyResult(it, if (approve) "VIP upgraded" else "VIP request rejected")
                        }
                    }
                )
                5 -> AdminSupportFolder(
                    ticketsState = ticketsState,
                    usersState = usersState,
                    appConfig = appConfig,
                    language = language,
                    isBusy = isBusy,
                    onRetry = { adminViewModel.retry() },
                    onUpdateTicket = { id, status, reply ->
                        adminViewModel.updateSupportTicket(id, status, reply) { notifyResult(it, "Support reply sent") }
                    },
                    onDeleteTickets = { ids ->
                        adminViewModel.deleteSupportTickets(ids) { notifyResult(it, "Selected support messages deleted") }
                    },
                    onBanUser = { uid, reason ->
                        adminViewModel.toggleUserBan(uid, ban = true, permanent = true, reason = reason) {
                            notifyResult(it, "User banned from support chat")
                        }
                    },
                    onAllowWalletChangeOnce = { uid ->
                        adminViewModel.allowUserWalletChangeOnce(uid) {
                            notifyResult(it, "1-time wallet change permission granted")
                        }
                    },
                    onToggleSupportEnabled = { enabled ->
                        adminViewModel.saveFullAppConfig(appConfig.copy(supportEnabled = enabled)) {
                            notifyResult(it, if (enabled) "Support option enabled" else "Support option hidden from Dashboard")
                        }
                    }
                )
                6 -> AdminNoticesFolder(
                    appConfig = appConfig,
                    announcementsState = announcementsState,
                    language = language,
                    isBusy = isBusy,
                    onSaveTopNotice = { updated ->
                        adminViewModel.saveFullAppConfig(updated) { notifyResult(it, "Dashboard Top Notice updated") }
                    },
                    onBroadcast = { cat, tBn, tEn, mBn, mEn, ver, url ->
                        adminViewModel.broadcastAnnouncement(cat, tBn, tEn, mBn, mEn, ver, url) {
                            notifyResult(it, "Announcement broadcasted")
                        }
                    }
                )
                else -> AdminCoreSettingsFolder(
                    appConfig = appConfig,
                    isBusy = isBusy,
                    onSaveConfig = { updated ->
                        adminViewModel.saveFullAppConfig(updated) {
                            notifyResult(it, "Core Settings saved")
                        }
                    }
                )
            }
        }
    }
}
