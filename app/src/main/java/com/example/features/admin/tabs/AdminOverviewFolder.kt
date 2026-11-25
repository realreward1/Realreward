package com.example.features.admin.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.security.AdminSecurity
import com.example.domain.model.*
import com.example.ui.components.AdminConfirmDialog
import com.example.ui.components.ErrorStateView
import com.example.ui.components.FintechStatCard
import com.example.ui.components.SectionHeader
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.*

private data class OverviewConfirmAction(
    val title: String,
    val message: String,
    val destructive: Boolean,
    val execute: () -> Unit
)

@Composable
fun AdminOverviewFolder(
    usersState: UiState<List<UserAccountDoc>>,
    tasksState: UiState<List<MicroTaskDoc>>,
    proofsState: UiState<List<TaskProofDoc>>,
    withdrawalsState: UiState<List<WithdrawalDoc>>,
    vipRequestsState: UiState<List<VipRequestDoc>>,
    ticketsState: UiState<List<SupportTicketDoc>>,
    fraudAlertsState: UiState<List<FraudAlertDoc>>,
    language: AppLanguage,
    isBusy: Boolean,
    onRetry: () -> Unit,
    onBanUser: (String, String) -> Unit,
    onResolveFraudAlert: (String) -> Unit
) {
    val strings = LocalStrings.current
    val users = (usersState as? UiState.Success)?.data.orEmpty()
    val tasks = (tasksState as? UiState.Success)?.data.orEmpty()
    val fraudAlerts = (fraudAlertsState as? UiState.Success)?.data.orEmpty()
    val unresolvedAlerts = fraudAlerts.filter { !it.resolved }
    val pendingProofs = ((proofsState as? UiState.Success)?.data.orEmpty()).count { it.statusEnum() == ProofStatus.PENDING_REVIEW }
    val pendingWithdrawals = ((withdrawalsState as? UiState.Success)?.data.orEmpty()).count {
        it.statusEnum() == TransactionStatus.PENDING || it.statusEnum() == TransactionStatus.APPROVED
    }
    val pendingVips = ((vipRequestsState as? UiState.Success)?.data.orEmpty()).count { it.statusEnum() == TransactionStatus.PENDING }
    val openTickets = ((ticketsState as? UiState.Success)?.data.orEmpty()).count { it.statusEnum() != TicketStatus.RESOLVED }
    val scamLeaderboard = remember(users) {
        users.filter { it.referralScamCount > 0 }.sortedByDescending { it.referralScamCount }
    }

    var pendingConfirm by remember { mutableStateOf<OverviewConfirmAction?>(null) }

    if (usersState is UiState.Error) {
        ErrorStateView(message = usersState.message, onRetry = onRetry)
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FintechStatCard("Total Users", "${users.size}", Icons.Default.People, NavyPrimary, Modifier.weight(1f))
                FintechStatCard("Active Tasks", "${tasks.count { it.active }}", Icons.Default.TaskAlt, EmeraldPrimary, Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FintechStatCard("Pending Proofs", "$pendingProofs", Icons.Default.FactCheck, GoldAccent, Modifier.weight(1f))
                FintechStatCard("Active Payouts", "$pendingWithdrawals", Icons.Default.Payments, StatusError, Modifier.weight(1f))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FintechStatCard("VIP Requests", "$pendingVips", Icons.Default.WorkspacePremium, GoldBright, Modifier.weight(1f))
                FintechStatCard("Fraud / Open Help", "${unresolvedAlerts.size} / $openTickets", Icons.Default.GppMaybe, StatusError, Modifier.weight(1f))
            }
        }

        if (unresolvedAlerts.isNotEmpty()) {
            item {
                SectionHeader(
                    title = if (language == AppLanguage.BANGLA) {
                        "রিয়েল-টাইম স্ক্যাম ও নিরাপত্তা অ্যালার্ট (${unresolvedAlerts.size})"
                    } else {
                        "Live Scam & Security Alerts (${unresolvedAlerts.size})"
                    }
                )
            }
            items(unresolvedAlerts.take(15), key = { it.id }) { alert ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = StatusErrorSoft,
                    border = BorderStroke(1.dp, StatusError.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (language == AppLanguage.BANGLA) alert.titleBn.ifBlank { alert.titleEn } else alert.titleEn.ifBlank { alert.titleBn },
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = StatusError,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(shape = RoundedCornerShape(50), color = StatusError) {
                                Text(
                                    text = alert.alertType,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Text(
                            text = "@${alert.username} • ${alert.email.orEmpty()} • IP: ${alert.ipAddress ?: "-"}",
                            style = MaterialTheme.typography.labelMedium,
                            color = NavyDeep
                        )
                        if (!alert.locationLabel.isNullOrBlank()) {
                            Text(
                                text = "Location: ${alert.locationLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = NavyPrimary
                            )
                        }
                        Text(alert.details, style = MaterialTheme.typography.bodySmall, color = NavyDeep)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    pendingConfirm = OverviewConfirmAction(
                                        title = strings.adminConfirmTitle,
                                        message = "Permanently ban @${alert.username} for ${alert.alertType}?",
                                        destructive = true
                                    ) {
                                        onBanUser(alert.userId, alert.details.ifBlank { alert.alertType })
                                        onResolveFraudAlert(alert.id)
                                    }
                                },
                                enabled = !isBusy && alert.userId.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = StatusError, contentColor = Color.White),
                                modifier = Modifier.weight(1f)
                            ) { Text(strings.adminBanUser) }
                            OutlinedButton(
                                onClick = {
                                    pendingConfirm = OverviewConfirmAction(
                                        title = strings.adminConfirmTitle,
                                        message = "Mark this fraud alert for @${alert.username} as resolved?",
                                        destructive = false
                                    ) {
                                        onResolveFraudAlert(alert.id)
                                    }
                                },
                                enabled = !isBusy,
                                modifier = Modifier.weight(1f)
                            ) { Text("Resolve") }
                        }
                    }
                }
            }
        }

        if (scamLeaderboard.isNotEmpty()) {
            item {
                SectionHeader(
                    title = if (language == AppLanguage.BANGLA) {
                        "রেফার স্ক্যাম র‍্যাংকিং (${scamLeaderboard.size})"
                    } else {
                        "Referral Scam Ranking (${scamLeaderboard.size})"
                    }
                )
            }
            itemsIndexed(scamLeaderboard.take(15), key = { _, u -> "scam_${u.userId}" }) { idx, u ->
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("#${idx + 1} ${u.fullName} (@${u.username})", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "Scam Tries: ${u.referralScamCount} • Valid Refs: ${u.referralCount} • Loc: ${u.lastLocation.ifBlank { "-" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusError
                            )
                        }
                        if (!u.banned) {
                            OutlinedButton(
                                onClick = {
                                    pendingConfirm = OverviewConfirmAction(
                                        title = strings.adminConfirmTitle,
                                        message = "Permanently ban @${u.username} (Scam attempts: ${u.referralScamCount})?",
                                        destructive = true
                                    ) {
                                        onBanUser(u.userId, "Referral scam threshold (${u.referralScamCount})")
                                    }
                                },
                                enabled = !isBusy,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError)
                            ) { Text(strings.adminBanUser) }
                        } else {
                            Surface(shape = RoundedCornerShape(50), color = StatusErrorSoft) {
                                Text(strings.adminBannedTag, color = StatusError, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }
                    }
                }
            }
        }

        item { SectionHeader(title = strings.leaderboardTitle) }
        itemsIndexed(users.take(10), key = { _, u -> u.userId }) { idx, u ->
            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("#${idx + 1} ${u.fullName} (@${u.username})", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "${u.mobileNumber} • ${u.vipTierEnum().title.get(language)} • Loc: ${u.lastLocation.ifBlank { "-" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${strings.leaderboardLifetimePrefix}: ${AdminSecurity.formatBdt(u.totalEarningsBdt)} • Pend: ${AdminSecurity.formatBdt(u.pendingBalanceBdt)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldAccent
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(AdminSecurity.formatBdt(u.availableBalanceBdt), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = EmeraldPrimary)
                        Text(strings.leaderboardWalletLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    pendingConfirm?.let { action ->
        AdminConfirmDialog(
            title = action.title,
            message = action.message,
            isDestructive = action.destructive,
            onConfirm = action.execute,
            onDismiss = { pendingConfirm = null }
        )
    }
}
