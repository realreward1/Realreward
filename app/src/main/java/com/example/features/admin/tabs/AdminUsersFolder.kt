package com.example.features.admin.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.security.AdminSecurity
import com.example.domain.model.*
import com.example.ui.components.AdminConfirmDialog
import com.example.ui.components.ErrorStateView
import com.example.ui.components.LoadingStateView
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.*

private data class UserConfirmAction(
    val title: String,
    val message: String,
    val destructive: Boolean,
    val execute: () -> Unit
)

@Composable
fun AdminUsersFolder(
    usersState: UiState<List<UserAccountDoc>>,
    language: AppLanguage,
    isBusy: Boolean,
    onRetry: () -> Unit,
    onToggleBan: (String, Boolean, Boolean, String?) -> Unit,
    onRemoveAccount: (UserAccountDoc, String) -> Unit,
    onSetVip: (String, VipTierCode) -> Unit,
    onAdjustBalance: (UserAccountDoc, Double, String) -> Unit,
    onAllowWalletChangeOnce: (String) -> Unit,
    onLoadUserHistory: (UserAccountDoc, (Result<UserFullHistory>) -> Unit) -> Unit
) {
    val strings = LocalStrings.current
    var search by remember { mutableStateOf("") }
    var adjustTarget by remember { mutableStateOf<UserAccountDoc?>(null) }
    var removeTarget by remember { mutableStateOf<UserAccountDoc?>(null) }
    var historyData by remember { mutableStateOf<UserFullHistory?>(null) }
    var historyLoading by remember { mutableStateOf(false) }
    var pendingConfirm by remember { mutableStateOf<UserConfirmAction?>(null) }

    when (usersState) {
        is UiState.Loading -> LoadingStateView()
        is UiState.Error -> ErrorStateView(message = usersState.message, onRetry = onRetry)
        is UiState.Success -> {
            val filtered = usersState.data.filter { u ->
                val q = search.trim().lowercase()
                q.isEmpty() || u.fullName.lowercase().contains(q) || u.username.lowercase().contains(q) ||
                    u.email.lowercase().contains(q) || u.mobileNumber.contains(q) ||
                    u.accountId.lowercase().contains(q) || u.lastIpAddress.lowercase().contains(q) ||
                    u.lastLocation.lowercase().contains(q)
            }
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = { Text(strings.searchPlaceholder) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                items(filtered, key = { it.userId }) { u ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("${u.fullName} (@${u.username})", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                    Text("${u.accountId} • ${u.mobileNumber} • ${u.email}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = "Location: ${u.lastLocation.ifBlank { "Resolving..." }} | IP: ${u.lastIpAddress.ifBlank { "-" }} (${u.lastCountryCode.ifBlank { "?" }}) | VPN: ${if (u.vpnDetected) "YES" else "NO"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (u.vpnDetected) StatusError else StatusInfo
                                    )
                                    Text(
                                        text = "Referrals: ${u.referralCount} (${AdminSecurity.formatBdt(u.referralEarningsBdt)}) • Scam Tries: ${u.referralScamCount}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (u.referralScamCount > 0) StatusError else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    u.banReason?.takeIf { it.isNotBlank() }?.let { reason ->
                                        Text("Ban Reason: $reason", style = MaterialTheme.typography.labelSmall, color = StatusError)
                                    }
                                }
                                if (u.banned) {
                                    Surface(shape = RoundedCornerShape(50), color = StatusErrorSoft) {
                                        Text(
                                            text = strings.adminBannedTag,
                                            color = StatusError,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Avail: ${AdminSecurity.formatBdt(u.availableBalanceBdt)} • Pend: ${AdminSecurity.formatBdt(u.pendingBalanceBdt)} • Total: ${AdminSecurity.formatBdt(u.totalEarningsBdt)} • VIP: ${u.vipTier}",
                                style = MaterialTheme.typography.labelMedium,
                                color = EmeraldDarkContainer
                            )
                            Text(
                                text = "bKash: ${u.bkashPersonal.ifBlank { "-" }} • Nagad: ${u.nagadPersonal.ifBlank { "-" }} • Locked: ${u.paymentAccountsLocked} (Resets: ${u.walletResetCount})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(VipTierCode.entries) { tier ->
                                    FilterChip(
                                        selected = u.vipTierEnum() == tier,
                                        onClick = {
                                            if (!isBusy && u.vipTierEnum() != tier) {
                                                pendingConfirm = UserConfirmAction(
                                                    title = strings.adminConfirmTitle,
                                                    message = "Change @${u.username} VIP tier to ${tier.name}?",
                                                    destructive = false
                                                ) { onSetVip(u.userId, tier) }
                                            }
                                        },
                                        label = { Text(tier.name) }
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        historyLoading = true
                                        onLoadUserHistory(u) { res ->
                                            historyLoading = false
                                            res.onSuccess { historyData = it }
                                        }
                                    },
                                    enabled = !isBusy && !historyLoading,
                                    modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                                ) {
                                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(strings.adminViewHistory, maxLines = 1)
                                }
                                OutlinedButton(
                                    onClick = { adjustTarget = u },
                                    enabled = !isBusy,
                                    modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                                ) {
                                    Text(strings.adminAdjustBalance, maxLines = 1)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        val ban = !u.banned
                                        pendingConfirm = UserConfirmAction(
                                            title = strings.adminConfirmTitle,
                                            message = if (ban) {
                                                "Permanently ban ${u.fullName} (@${u.username})?"
                                            } else {
                                                "Unban ${u.fullName} (@${u.username})?"
                                            },
                                            destructive = ban
                                        ) {
                                            onToggleBan(u.userId, ban, ban, if (ban) "Permanent ban by admin" else null)
                                        }
                                    },
                                    enabled = !isBusy,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (u.banned) EmeraldPrimary else StatusError,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                                ) {
                                    Text(if (u.banned) strings.adminUnbanUser else strings.adminBanUser, maxLines = 1)
                                }
                                OutlinedButton(
                                    onClick = { removeTarget = u },
                                    enabled = !isBusy,
                                    border = BorderStroke(1.dp, StatusError),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                                    modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                                ) {
                                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(strings.adminRemoveAccount, maxLines = 1)
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    pendingConfirm = UserConfirmAction(
                                        title = strings.adminConfirmTitle,
                                        message = "Allow ${u.fullName} (@${u.username}) to change bKash & Nagad wallet numbers once? Wallet will re-lock automatically after saving.",
                                        destructive = false
                                    ) {
                                        onAllowWalletChangeOnce(u.userId)
                                    }
                                },
                                enabled = !isBusy && u.paymentAccountsLocked,
                                border = BorderStroke(1.dp, if (u.paymentAccountsLocked) GoldAccent else EmeraldPrimary),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (u.paymentAccountsLocked) GoldDarkContainer else EmeraldPrimary
                                ),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)
                            ) {
                                Icon(
                                    imageVector = if (u.paymentAccountsLocked) Icons.Default.LockOpen else Icons.Default.Verified,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (u.paymentAccountsLocked) strings.adminAllowWalletChangeOnce else strings.adminWalletAlreadyUnlockedTag,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    adjustTarget?.let { target ->
        var deltaText by remember { mutableStateOf("50") }
        var reason by remember { mutableStateOf("Admin bonus") }
        AlertDialog(
            onDismissRequest = { adjustTarget = null },
            title = { Text("${strings.adminAdjustBalance}: @${target.username}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = deltaText,
                        onValueChange = { deltaText = it },
                        label = { Text("Amount BDT (use - to deduct)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Reason") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val delta = deltaText.toDoubleOrNull() ?: 0.0
                    adjustTarget = null
                    if (delta != 0.0) {
                        pendingConfirm = UserConfirmAction(
                            title = strings.adminConfirmTitle,
                            message = "Adjust @${target.username}'s balance by ${AdminSecurity.formatBdt(delta)} ($reason)?",
                            destructive = delta < 0
                        ) { onAdjustBalance(target, delta, reason) }
                    }
                }) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { adjustTarget = null }) { Text(strings.cancel) }
            }
        )
    }

    removeTarget?.let { target ->
        var removeReason by remember { mutableStateOf("Removed permanently for fraud/abuse") }
        AlertDialog(
            onDismissRequest = { removeTarget = null },
            title = { Text("${strings.adminRemoveAccount}: @${target.username}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "This will delete @${target.username} and permanently block UID (${target.userId}) & Gmail (${target.email}) from ever logging in again.",
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusError
                    )
                    OutlinedTextField(
                        value = removeReason,
                        onValueChange = { removeReason = it },
                        label = { Text("Removal Reason") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        removeTarget = null
                        pendingConfirm = UserConfirmAction(
                            title = strings.adminConfirmTitle,
                            message = "FINAL CONFIRMATION: Permanently remove ${target.fullName} (@${target.username}) & block Gmail forever?",
                            destructive = true
                        ) { onRemoveAccount(target, removeReason) }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusError, contentColor = Color.White)
                ) { Text(strings.adminRemoveAccount) }
            },
            dismissButton = {
                TextButton(onClick = { removeTarget = null }) { Text(strings.cancel) }
            }
        )
    }

    historyData?.let { hist ->
        AlertDialog(
            onDismissRequest = { historyData = null },
            title = { Text("${hist.user.fullName} (@${hist.user.username})") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(shape = RoundedCornerShape(10.dp), color = StatusInfoSoft) {
                        Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Live Location: ${hist.user.lastLocation.ifBlank { "Unknown" }}", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = NavyDeep)
                            Text("IP: ${hist.user.lastIpAddress} (${hist.user.lastCountryCode}) • Device: ${hist.user.deviceId}", style = MaterialTheme.typography.labelSmall)
                            Text("Gmail: ${hist.user.email} • Mobile: ${hist.user.mobileNumber}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Text("Transactions (${hist.transactions.size})", style = MaterialTheme.typography.titleSmall, color = EmeraldPrimary)
                    if (hist.transactions.isEmpty()) Text("No transactions", style = MaterialTheme.typography.bodySmall)
                    hist.transactions.take(10).forEach { txn ->
                        Text("• ${txn.title(language)}: ${AdminSecurity.formatBdt(txn.amountBdt)} [${txn.status}]", style = MaterialTheme.typography.bodySmall)
                    }
                    HorizontalDivider()
                    Text("Withdrawals (${hist.withdrawals.size})", style = MaterialTheme.typography.titleSmall, color = NavyPrimary)
                    if (hist.withdrawals.isEmpty()) Text("No withdrawals", style = MaterialTheme.typography.bodySmall)
                    hist.withdrawals.take(10).forEach { w ->
                        Text("• ${w.methodEnum().shortName} ${w.accountNumber}: ${AdminSecurity.formatBdt(w.amountBdt)} [${w.status}]", style = MaterialTheme.typography.bodySmall)
                    }
                    HorizontalDivider()
                    Text("Task Proofs (${hist.proofs.size})", style = MaterialTheme.typography.titleSmall, color = GoldAccent)
                    if (hist.proofs.isEmpty()) Text("No proofs submitted", style = MaterialTheme.typography.bodySmall)
                    hist.proofs.take(10).forEach { p ->
                        Text("• ${p.taskTitle}: ${AdminSecurity.formatBdt(p.rewardBdt)} [${p.status}]", style = MaterialTheme.typography.bodySmall)
                    }
                    HorizontalDivider()
                    Text("Referrals (${hist.referrals.size})", style = MaterialTheme.typography.titleSmall, color = EmeraldDarkContainer)
                    if (hist.referrals.isEmpty()) Text("No referrals", style = MaterialTheme.typography.bodySmall)
                    hist.referrals.take(10).forEach { r ->
                        Text("• @${r.referredUsername} (${r.status}) +${AdminSecurity.formatBdt(r.bonusBdt)} ${r.scamReason ?: ""}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { historyData = null }) { Text(strings.okConfirm) }
            }
        )
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
