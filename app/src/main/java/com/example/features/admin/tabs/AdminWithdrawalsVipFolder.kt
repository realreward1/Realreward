package com.example.features.admin.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.security.AdminSecurity
import com.example.domain.model.*
import com.example.ui.components.AdminConfirmDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorStateView
import com.example.ui.components.SectionHeader
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.*

private data class FinanceConfirmAction(
    val title: String,
    val message: String,
    val destructive: Boolean,
    val execute: () -> Unit
)

@Composable
fun AdminWithdrawalsVipFolder(
    withdrawalsState: UiState<List<WithdrawalDoc>>,
    vipRequestsState: UiState<List<VipRequestDoc>>,
    language: AppLanguage,
    isBusy: Boolean,
    onRetry: () -> Unit,
    onUpdateWithdrawalStage: (WithdrawalDoc, TransactionStatus, String) -> Unit,
    onDeleteWithdrawal: (String) -> Unit,
    onDeleteCompletedWithdrawals: (List<String>) -> Unit,
    onReviewVip: (VipRequestDoc, Boolean) -> Unit
) {
    val strings = LocalStrings.current
    val withdrawals = (withdrawalsState as? UiState.Success)?.data.orEmpty()
    val completedIds = remember(withdrawals) {
        withdrawals.filter { it.statusEnum() == TransactionStatus.COMPLETED }.map { it.id }
    }
    val vips = (vipRequestsState as? UiState.Success)?.data.orEmpty()
    var pendingConfirm by remember { mutableStateOf<FinanceConfirmAction?>(null) }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    SectionHeader(title = "Withdrawals Queue (${withdrawals.size})")
                }
                if (completedIds.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            pendingConfirm = FinanceConfirmAction(
                                title = strings.adminConfirmTitle,
                                message = "${strings.adminRemoveAllPaid} (${completedIds.size})?",
                                destructive = true
                            ) { onDeleteCompletedWithdrawals(completedIds) }
                        },
                        enabled = !isBusy,
                        colors = ButtonDefaults.textButtonColors(contentColor = StatusError),
                        modifier = Modifier.testTag("admin_remove_all_paid_btn")
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.adminRemoveAllPaid)
                    }
                }
            }
        }
        if (withdrawalsState is UiState.Error) {
            item { ErrorStateView(message = withdrawalsState.message, onRetry = onRetry) }
        } else if (withdrawals.isEmpty()) {
            item { EmptyStateView() }
        } else {
            items(withdrawals, key = { it.id }) { w ->
                val st = w.statusEnum()
                val (badgeBg, badgeFg) = when (st) {
                    TransactionStatus.PENDING -> GoldSoft to GoldDarkContainer
                    TransactionStatus.APPROVED -> StatusInfoSoft to StatusInfo
                    TransactionStatus.COMPLETED -> EmeraldSoft to EmeraldDarkContainer
                    TransactionStatus.FAILED -> StatusErrorSoft to StatusError
                }
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                text = "@${w.username} • ${w.methodEnum().title.get(language)}: ${w.accountNumber}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.weight(1f)
                            )
                            Surface(shape = RoundedCornerShape(50), color = badgeBg) {
                                Text(
                                    text = st.label.get(language),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = badgeFg,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Text(
                            text = "Payout: ${AdminSecurity.formatBdt(w.amountBdt)} (+${AdminSecurity.formatBdt(w.feeBdt)} Fee = ${AdminSecurity.formatBdt(w.totalDeductedBdt)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldPrimary
                        )
                        when (st) {
                            TransactionStatus.PENDING -> {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            pendingConfirm = FinanceConfirmAction(
                                                title = strings.adminConfirmTitle,
                                                message = "Approve ${w.methodEnum().shortName} ${AdminSecurity.formatBdt(w.amountBdt)} for @${w.username}?",
                                                destructive = false
                                            ) { onUpdateWithdrawalStage(w, TransactionStatus.APPROVED, "Approved by owner") }
                                        },
                                        enabled = !isBusy,
                                        colors = ButtonDefaults.buttonColors(containerColor = StatusInfo, contentColor = Color.White),
                                        modifier = Modifier.weight(1f)
                                    ) { Text(strings.adminApprove) }
                                    OutlinedButton(
                                        onClick = {
                                            pendingConfirm = FinanceConfirmAction(
                                                title = strings.adminConfirmTitle,
                                                message = "Reject & refund ${AdminSecurity.formatBdt(w.totalDeductedBdt)} to @${w.username}?",
                                                destructive = true
                                            ) { onUpdateWithdrawalStage(w, TransactionStatus.FAILED, "Rejected & refunded") }
                                        },
                                        enabled = !isBusy,
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                                        modifier = Modifier.weight(1f)
                                    ) { Text(strings.adminReject) }
                                }
                            }
                            TransactionStatus.APPROVED -> {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            pendingConfirm = FinanceConfirmAction(
                                                title = strings.adminConfirmTitle,
                                                message = "Confirm ${w.methodEnum().shortName} payout ${AdminSecurity.formatBdt(w.amountBdt)} has been sent to ${w.accountNumber}?",
                                                destructive = false
                                            ) { onUpdateWithdrawalStage(w, TransactionStatus.COMPLETED, "Payment Success") }
                                        },
                                        enabled = !isBusy,
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                                        modifier = Modifier.weight(1f)
                                    ) { Text(strings.adminPaymentSuccessButton) }
                                    OutlinedButton(
                                        onClick = {
                                            pendingConfirm = FinanceConfirmAction(
                                                title = strings.adminConfirmTitle,
                                                message = "Cancel payout & refund @${w.username}?",
                                                destructive = true
                                            ) { onUpdateWithdrawalStage(w, TransactionStatus.FAILED, "Rejected & refunded") }
                                        },
                                        enabled = !isBusy,
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                                        modifier = Modifier.weight(1f)
                                    ) { Text(strings.adminReject) }
                                }
                            }
                            TransactionStatus.COMPLETED, TransactionStatus.FAILED -> {
                                OutlinedButton(
                                    onClick = {
                                        pendingConfirm = FinanceConfirmAction(
                                            title = strings.adminConfirmTitle,
                                            message = "${strings.adminRemoveWithdrawal}: @${w.username} (${AdminSecurity.formatBdt(w.amountBdt)})?",
                                            destructive = true
                                        ) { onDeleteWithdrawal(w.id) }
                                    },
                                    enabled = !isBusy,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                                    modifier = Modifier.fillMaxWidth().testTag("admin_remove_wdr_${w.id}")
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(strings.adminRemoveWithdrawal)
                                }
                            }
                        }
                    }
                }
            }
        }

        item { SectionHeader(title = "VIP Upgrade Requests (${vips.size})") }
        if (vips.isEmpty()) {
            item { EmptyStateView() }
        } else {
            items(vips, key = { it.id }) { req ->
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("@${req.username} -> ${req.targetTier} (${AdminSecurity.formatBdt(req.amountBdt)})", style = MaterialTheme.typography.titleSmall)
                        Text("${req.methodEnum().title.get(language)} • Sender: ${req.senderNumber} • TrxID: ${req.transactionId}", style = MaterialTheme.typography.bodySmall)
                        Text("Status: ${req.statusEnum().label.get(language)}", style = MaterialTheme.typography.labelMedium, color = GoldAccent)
                        if (req.statusEnum() == TransactionStatus.PENDING) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        pendingConfirm = FinanceConfirmAction(
                                            title = strings.adminConfirmTitle,
                                            message = "Activate ${req.targetTier} VIP for @${req.username} (TrxID: ${req.transactionId})?",
                                            destructive = false
                                        ) { onReviewVip(req, true) }
                                    },
                                    enabled = !isBusy,
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                                    modifier = Modifier.weight(1f)
                                ) { Text(strings.adminApprove) }
                                OutlinedButton(
                                    onClick = {
                                        pendingConfirm = FinanceConfirmAction(
                                            title = strings.adminConfirmTitle,
                                            message = "Reject VIP request from @${req.username}?",
                                            destructive = true
                                        ) { onReviewVip(req, false) }
                                    },
                                    enabled = !isBusy,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                                    modifier = Modifier.weight(1f)
                                ) { Text(strings.adminReject) }
                            }
                        }
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
