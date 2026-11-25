package com.example.features.wallet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@Composable
fun WithdrawalScreen(
    user: UserAccountDoc,
    language: AppLanguage,
    withdrawalsState: UiState<List<WithdrawalDoc>>,
    isSubmitting: Boolean,
    onToggleLanguage: () -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onGoToProfileSetup: () -> Unit = {},
    onSubmitWithdrawal: (UserAccountDoc, WithdrawalMethod, String, Double, (Result<Unit>) -> Unit) -> Unit
) {
    val strings = LocalStrings.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var selectedMethod by remember { mutableStateOf(WithdrawalMethod.BKASH_PERSONAL) }
    val isWalletLocked = user.paymentAccountsLocked
    val boundNumberForMethod = remember(selectedMethod, user.bkashPersonal, user.nagadPersonal) {
        if (selectedMethod == WithdrawalMethod.BKASH_PERSONAL) user.bkashPersonal else user.nagadPersonal
    }
    var accountNumber by remember(user.uidOrMobile(), selectedMethod) {
        mutableStateOf(
            if (selectedMethod == WithdrawalMethod.BKASH_PERSONAL) user.bkashPersonal else user.nagadPersonal
        )
    }
    var amountText by remember { mutableStateOf("300") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
    val feeBdt = AdminSecurity.calculateWithdrawalFeeBdt(parsedAmount)
    val totalDeduction = if (parsedAmount > 0) parsedAmount + feeBdt else 0.0

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            RealRewardTopBar(
                title = strings.withdrawalTitle,
                language = language,
                onToggleLanguage = onToggleLanguage,
                onBack = onBack
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
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = GoldSoft,
                    border = BorderStroke(1.dp, GoldBright.copy(alpha = 0.55f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = GoldDarkContainer)
                        Column {
                            Text(
                                text = strings.minWithdrawalNotice,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = GoldDarkContainer
                            )
                            Text(
                                text = strings.withdrawalFeeBreakdown,
                                style = MaterialTheme.typography.bodySmall,
                                color = GoldDarkContainer
                            )
                        }
                    }
                }
            }

            if (!isWalletLocked) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = StatusErrorSoft,
                        border = BorderStroke(1.dp, StatusError.copy(alpha = 0.45f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LockOpen, contentDescription = null, tint = StatusError)
                                Text(
                                    text = strings.setupWalletPromptForWithdraw,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = StatusError
                                )
                            }
                            Button(
                                onClick = onGoToProfileSetup,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("withdrawal_setup_wallet_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NavyDeep, contentColor = Color.White)
                            ) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(strings.setupWalletOnceButton)
                            }
                        }
                    }
                }
            }

            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "${strings.availableBalance}: ${AdminSecurity.formatBdt(user.availableBalanceBdt)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldPrimary
                        )

                        Text(strings.selectMethod, style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            WithdrawalMethod.entries.forEach { method ->
                                val selected = selectedMethod == method
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        selectedMethod = method
                                        accountNumber = if (method == WithdrawalMethod.BKASH_PERSONAL) {
                                            user.bkashPersonal
                                        } else {
                                            user.nagadPersonal
                                        }
                                        errorMsg = null
                                    },
                                    label = { Text(method.title.get(language)) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = if (isWalletLocked) boundNumberForMethod else accountNumber,
                            onValueChange = { },
                            readOnly = true,
                            enabled = isWalletLocked,
                            label = { Text(strings.accountNumberLabel) },
                            trailingIcon = {
                                Icon(
                                    imageVector = if (isWalletLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = null,
                                    tint = if (isWalletLocked) EmeraldPrimary else StatusError
                                )
                            },
                            supportingText = {
                                Text(
                                    text = if (isWalletLocked) strings.paymentLockedBadge else strings.setupWalletPromptForWithdraw,
                                    color = if (isWalletLocked) EmeraldPrimary else StatusError
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("withdrawal_account_input")
                        )

                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' }; errorMsg = null },
                            label = { Text(strings.withdrawalAmountLabel) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("withdrawal_amount_input")
                        )

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(strings.withdrawalFeeLabel, style = MaterialTheme.typography.bodySmall)
                                    Text(AdminSecurity.formatBdt(feeBdt), style = MaterialTheme.typography.labelLarge, color = GoldAccent)
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(strings.withdrawalTotalDeductionLabel, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = AdminSecurity.formatBdt(totalDeduction),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = NavyDeep
                                    )
                                }
                            }
                        }

                        errorMsg?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = StatusError)
                        }

                        Button(
                            onClick = {
                                val targetAccount = if (isWalletLocked) boundNumberForMethod else accountNumber
                                val amount = amountText.toDoubleOrNull() ?: 0.0
                                val reqFee = AdminSecurity.calculateWithdrawalFeeBdt(amount)
                                when {
                                    !isWalletLocked -> errorMsg = strings.setupWalletPromptForWithdraw
                                    !AdminSecurity.isValidBdMobile(targetAccount) -> errorMsg = strings.errBdMobile
                                    amount < 300.0 -> errorMsg = strings.errWithdrawalMin
                                    user.availableBalanceBdt < (amount + reqFee) -> errorMsg = "${strings.errWithdrawalInsufficientWithFee} (${AdminSecurity.formatBdt(amount + reqFee)})"
                                    else -> {
                                        onSubmitWithdrawal(user, selectedMethod, targetAccount, amount) { res ->
                                            scope.launch {
                                                res.fold(
                                                    onSuccess = {
                                                        snackbarHostState.showSnackbar(strings.withdrawalSubmittedSuccess)
                                                    },
                                                    onFailure = { err ->
                                                        errorMsg = err.localizedMessage ?: strings.errorTitle
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = !isSubmitting && isWalletLocked,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 50.dp)
                                .testTag("submit_withdrawal_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White)
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text(strings.submitWithdrawalRequest)
                            }
                        }
                    }
                }
            }

            item { SectionHeader(title = strings.withdrawalHistory) }

            when (withdrawalsState) {
                is UiState.Loading -> item { LoadingStateView() }
                is UiState.Error -> item { ErrorStateView(message = withdrawalsState.message, onRetry = onRetry) }
                is UiState.Success -> {
                    if (withdrawalsState.data.isEmpty()) {
                        item { EmptyStateView() }
                    } else {
                        items(withdrawalsState.data, key = { it.id }) { item ->
                            val st = item.statusEnum()
                            val (badgeBg, badgeFg) = when (st) {
                                TransactionStatus.PENDING -> GoldSoft to GoldDarkContainer
                                TransactionStatus.APPROVED -> StatusInfoSoft to StatusInfo
                                TransactionStatus.COMPLETED -> EmeraldSoft to EmeraldDarkContainer
                                TransactionStatus.FAILED -> StatusErrorSoft to StatusError
                            }
                            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "${item.methodEnum().title.get(language)} • ${item.accountNumber}",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(shape = RoundedCornerShape(50), color = badgeBg) {
                                                Text(
                                                    text = st.label.get(language),
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = badgeFg,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                            Text(
                                                text = formatTimestamp(item.createdAt, language),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        item.adminNote?.takeIf { it.isNotBlank() }?.let {
                                            Text(it, style = MaterialTheme.typography.labelSmall, color = NavyPrimary)
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = AdminSecurity.formatBdt(item.amountBdt),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = EmeraldPrimary
                                        )
                                        Text(
                                            text = "${strings.transactionFeeDeductedLabel}: ${AdminSecurity.formatBdt(item.feeBdt)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = GoldDarkContainer
                                        )
                                        Text(
                                            text = "-${AdminSecurity.formatBdt(item.totalDeductedBdt)}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = StatusError
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun UserAccountDoc.uidOrMobile(): String = "${userId}_${bkashPersonal}_${nagadPersonal}"
