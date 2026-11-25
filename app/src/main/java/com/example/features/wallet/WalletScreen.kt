package com.example.features.wallet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.security.AdminSecurity
import com.example.domain.model.*
import com.example.ui.components.*
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.*
import kotlinx.coroutines.launch

private enum class WalletFilter { ALL, EARNINGS, WITHDRAWALS, PENDING }

@Composable
fun WalletScreen(
    user: UserAccountDoc,
    language: AppLanguage,
    transactionsState: UiState<List<TransactionDoc>>,
    isSubmitting: Boolean = false,
    onToggleLanguage: () -> Unit,
    onRetry: () -> Unit,
    onNavigateWithdrawal: () -> Unit,
    onOpenNotifications: () -> Unit,
    supportEnabled: Boolean = true,
    onNavigateSupport: () -> Unit = {},
    onSetupPaymentAccounts: (String, String, (Result<Unit>) -> Unit) -> Unit = { _, _, cb -> cb(Result.success(Unit)) }
) {
    val strings = LocalStrings.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var filter by remember { mutableStateOf(WalletFilter.ALL) }
    var showSetupWalletDialog by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            RealRewardTopBar(
                title = strings.walletTitle,
                language = language,
                onToggleLanguage = onToggleLanguage,
                onOpenNotifications = onOpenNotifications
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Brush.linearGradient(colors = listOf(NavyDeep, NavyPrimary, NavySurface)))
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(strings.availableBalance, style = MaterialTheme.typography.labelLarge, color = EmeraldSoft)
                            LivePlatformPill()
                        }
                        Text(
                            text = AdminSecurity.formatBdt(user.availableBalanceBdt),
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontFamily = PlusJakartaSansFontFamily,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = Color.White
                        )
                        Button(
                            onClick = onNavigateWithdrawal,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("wallet_withdraw_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Payments, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(strings.withdrawNow)
                        }
                    }
                }
            }

            item {
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
            }

            item {
                FintechStatCard(
                    title = strings.totalWithdrawals,
                    value = AdminSecurity.formatBdt(user.totalWithdrawalsBdt),
                    icon = Icons.Default.AccountBalance,
                    accentColor = NavyPrimary,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Permanent 2-Wallet Account Card (bKash Personal & Nagad Personal - One-time lock)
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = EmeraldPrimary)
                                Text(
                                    text = strings.paymentAccountSettings,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            if (user.paymentAccountsLocked) {
                                Surface(shape = RoundedCornerShape(50), color = EmeraldSoft) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Lock, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(14.dp))
                                        Text(
                                            text = strings.paymentLockedBadge,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = EmeraldDarkContainer
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(strings.bkashPersonalLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = user.bkashPersonal.ifBlank { "—" },
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(strings.nagadPersonalLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = user.nagadPersonal.ifBlank { "—" },
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                        }

                        if (!user.paymentAccountsLocked) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (user.walletChangeApprovedOnce) EmeraldSoft else GoldSoft,
                                border = BorderStroke(
                                    1.dp,
                                    if (user.walletChangeApprovedOnce) EmeraldPrimary.copy(alpha = 0.45f) else GoldBright.copy(alpha = 0.45f)
                                )
                            ) {
                                Text(
                                    text = if (user.walletChangeApprovedOnce) strings.walletResetApprovedNotice else strings.paymentSetupWarning,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (user.walletChangeApprovedOnce) EmeraldDarkContainer else GoldDarkContainer,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                            Button(
                                onClick = { showSetupWalletDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("wallet_setup_once_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NavyDeep, contentColor = Color.White)
                            ) {
                                Icon(Icons.Default.LockClock, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(strings.setupWalletOnceButton)
                            }
                        } else {
                            Text(
                                text = strings.walletLockedChangeViaSupport,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (supportEnabled) {
                                OutlinedButton(
                                    onClick = onNavigateSupport,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("wallet_request_change_support_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(strings.requestWalletChangeButton)
                                }
                            }
                        }
                    }
                }
            }

            item {
                SectionHeader(title = strings.transactionHistory)
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(
                        listOf(
                            WalletFilter.ALL to strings.filterAll,
                            WalletFilter.EARNINGS to strings.filterEarnings,
                            WalletFilter.WITHDRAWALS to strings.filterWithdrawals,
                            WalletFilter.PENDING to strings.filterPending
                        )
                    ) { (itemFilter, label) ->
                        FilterChip(
                            selected = filter == itemFilter,
                            onClick = { filter = itemFilter },
                            label = { Text(label) }
                        )
                    }
                }
            }

            when (transactionsState) {
                is UiState.Loading -> item { LoadingStateView() }
                is UiState.Error -> item { ErrorStateView(message = transactionsState.message, onRetry = onRetry) }
                is UiState.Success -> {
                    val filtered = transactionsState.data.filter { txn ->
                        when (filter) {
                            WalletFilter.ALL -> true
                            WalletFilter.EARNINGS -> txn.typeEnum() == TransactionType.TASK_EARNING || txn.typeEnum() == TransactionType.BONUS
                            WalletFilter.WITHDRAWALS -> txn.typeEnum() == TransactionType.WITHDRAWAL
                            WalletFilter.PENDING -> txn.statusEnum() == TransactionStatus.PENDING
                        }
                    }
                    if (filtered.isEmpty()) {
                        item { EmptyStateView() }
                    } else {
                        items(filtered, key = { it.id }) { txn ->
                            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(txn.title(language), style = MaterialTheme.typography.titleSmall)
                                        if (txn.typeEnum() == TransactionType.WITHDRAWAL && (txn.feeBdt ?: 0.0) > 0.0) {
                                            val netPart = txn.netAmountBdt?.let { "${AdminSecurity.formatBdt(it)} + " } ?: ""
                                            Text(
                                                text = "$netPart${strings.transactionFeeDeductedLabel}: ${AdminSecurity.formatBdt(txn.feeBdt ?: 0.0)}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = GoldDarkContainer
                                            )
                                        }
                                        Text(
                                            text = "${txn.statusEnum().label.get(language)} • ${formatTimestamp(txn.createdAt, language)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    val pos = txn.amountBdt >= 0
                                    val absFormatted = AdminSecurity.formatBdt(kotlin.math.abs(txn.amountBdt))
                                    Text(
                                        text = if (pos) "+$absFormatted" else "-$absFormatted",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (pos) EmeraldPrimary else StatusError
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSetupWalletDialog && !user.paymentAccountsLocked) {
        var bkash by remember(user.bkashPersonal) { mutableStateOf(user.bkashPersonal) }
        var nagad by remember(user.nagadPersonal) { mutableStateOf(user.nagadPersonal) }
        var dialogError by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showSetupWalletDialog = false },
            title = { Text(strings.setupWalletOnceButton) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (user.walletChangeApprovedOnce) EmeraldSoft else GoldSoft
                    ) {
                        Text(
                            text = if (user.walletChangeApprovedOnce) strings.walletResetApprovedNotice else strings.paymentSetupWarning,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (user.walletChangeApprovedOnce) EmeraldDarkContainer else GoldDarkContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    OutlinedTextField(
                        value = bkash,
                        onValueChange = { bkash = it.filter { c -> c.isDigit() || c == '+' }; dialogError = null },
                        label = { Text(strings.bkashPersonalLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("wallet_dialog_bkash_input")
                    )
                    OutlinedTextField(
                        value = nagad,
                        onValueChange = { nagad = it.filter { c -> c.isDigit() || c == '+' }; dialogError = null },
                        label = { Text(strings.nagadPersonalLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("wallet_dialog_nagad_input")
                    )
                    dialogError?.let { err ->
                        Text(err, style = MaterialTheme.typography.bodySmall, color = StatusError)
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isSubmitting,
                    onClick = {
                        if (!AdminSecurity.isValidBdMobile(bkash) || !AdminSecurity.isValidBdMobile(nagad)) {
                            dialogError = strings.errBdMobile
                            return@Button
                        }
                        onSetupPaymentAccounts(bkash, nagad) { res ->
                            res.fold(
                                onSuccess = {
                                    showSetupWalletDialog = false
                                    scope.launch { snackbarHostState.showSnackbar(strings.paymentSavedSuccess) }
                                },
                                onFailure = { err ->
                                    dialogError = when (err.message) {
                                        "PAYMENT_ACCOUNTS_LOCKED" -> strings.paymentAlreadyLockedError
                                        "DUPLICATE_WALLET_NUMBER" -> strings.walletDuplicateNumberError
                                        else -> err.localizedMessage ?: strings.errorTitle
                                    }
                                }
                            )
                        }
                    },
                    modifier = Modifier.testTag("wallet_dialog_save_button")
                ) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { showSetupWalletDialog = false }) { Text(strings.cancel) }
            }
        )
    }
}
