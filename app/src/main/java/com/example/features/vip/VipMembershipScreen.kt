package com.example.features.vip

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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
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
fun VipMembershipScreen(
    user: UserAccountDoc,
    appConfig: AppConfigDoc,
    vipRequestsState: UiState<List<VipRequestDoc>>,
    language: AppLanguage,
    isSubmitting: Boolean,
    onToggleLanguage: () -> Unit,
    onRetry: () -> Unit,
    onOpenNotifications: () -> Unit,
    onSubmitVipRequest: (UserAccountDoc, VipTierCode, WithdrawalMethod, String, String, Double, (Result<Unit>) -> Unit) -> Unit
) {
    val strings = LocalStrings.current
    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var selectedTier by remember { mutableStateOf(VipTierCode.SILVER) }
    var selectedMethod by remember { mutableStateOf(WithdrawalMethod.BKASH_PERSONAL) }
    var senderNumber by remember(user.mobileNumber) { mutableStateOf(user.mobileNumber) }
    var trxId by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            RealRewardTopBar(
                title = strings.vipTitle,
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
                val activeTier = user.vipTierEnum()
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = NavyDeep
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(strings.currentTierLabel, style = MaterialTheme.typography.labelLarge, color = GoldSoft)
                            Surface(shape = RoundedCornerShape(50), color = GoldBright) {
                                Text(
                                    text = activeTier.title.get(language),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = NavyDeep,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }
                        if (activeTier.bonusPercent > 0) {
                            Text(
                                text = "${strings.vipTaskBonusRateLabel}: +${activeTier.bonusPercent}%",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = EmeraldBright
                            )
                        }
                        Text(strings.vipSubtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
                    }
                }
            }

            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = GoldSoft)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = strings.vipRateComparisonTitle,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = GoldDarkContainer
                        )
                        Text(
                            text = strings.vipRateComparisonDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = GoldDarkContainer.copy(alpha = 0.9f)
                        )
                        VipTierCode.entries.forEach { tierRow ->
                            val sampleTotal = AdminSecurity.calculateTaskRewardWithVip(100.0, tierRow.name)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${tierRow.title.get(language)} (+${tierRow.bonusPercent}%)",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = NavyDeep
                                )
                                Text(
                                    text = "৳100 → ${AdminSecurity.formatBdt(sampleTotal)}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = EmeraldDarkContainer
                                )
                            }
                        }
                    }
                }
            }

            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(strings.adminPaymentNumbersTitle, style = MaterialTheme.typography.titleMedium)
                        AdminNumberRow(
                            label = WithdrawalMethod.BKASH_PERSONAL.title.get(language),
                            number = appConfig.bkashNumber,
                            copyLabel = strings.copyNumber,
                            onCopy = {
                                clipboard.setText(AnnotatedString(appConfig.bkashNumber))
                                scope.launch { snackbarHostState.showSnackbar(strings.numberCopied) }
                            }
                        )
                        AdminNumberRow(
                            label = WithdrawalMethod.NAGAD_PERSONAL.title.get(language),
                            number = appConfig.nagadNumber,
                            copyLabel = strings.copyNumber,
                            onCopy = {
                                clipboard.setText(AnnotatedString(appConfig.nagadNumber))
                                scope.launch { snackbarHostState.showSnackbar(strings.numberCopied) }
                            }
                        )
                    }
                }
            }

            items(VipTierCode.paidTiers()) { tier ->
                val price = appConfig.priceForTier(tier)
                val isSelected = selectedTier == tier
                val sampleOn100 = AdminSecurity.calculateTaskRewardWithVip(100.0, tier.name)
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.outline
                    ),
                    onClick = { selectedTier = tier }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(tier.title.get(language), style = MaterialTheme.typography.titleMedium)
                                Surface(shape = RoundedCornerShape(50), color = EmeraldSoft) {
                                    Text(
                                        text = "+${tier.bonusPercent}%",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = EmeraldDarkContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(tier.duration.get(language), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "${strings.vipTaskBonusRateLabel}: +${tier.bonusPercent}% (৳100 → ${AdminSecurity.formatBdt(sampleOn100)})",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = EmeraldPrimary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = AdminSecurity.formatBdt(price),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = EmeraldPrimary
                            )
                            RadioButton(selected = isSelected, onClick = { selectedTier = tier })
                        }
                    }
                }
            }

            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "${strings.upgradeVipTitle} (${selectedTier.title.get(language)} • ${AdminSecurity.formatBdt(appConfig.priceForTier(selectedTier))})",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            WithdrawalMethod.entries.forEach { method ->
                                FilterChip(
                                    selected = selectedMethod == method,
                                    onClick = { selectedMethod = method },
                                    label = { Text(method.title.get(language)) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = senderNumber,
                            onValueChange = { senderNumber = it.filter { c -> c.isDigit() || c == '+' }; formError = null },
                            label = { Text(strings.senderNumberLabel) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("vip_sender_input")
                        )

                        OutlinedTextField(
                            value = trxId,
                            onValueChange = { trxId = it; formError = null },
                            label = { Text(strings.trxIdLabel) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("vip_trxid_input")
                        )

                        formError?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, color = StatusError)
                        }

                        Button(
                            onClick = {
                                when {
                                    !AdminSecurity.isValidBdMobile(senderNumber) -> formError = strings.errBdMobile
                                    trxId.trim().length < 4 -> formError = strings.errTrxId
                                    else -> {
                                        val amount = appConfig.priceForTier(selectedTier)
                                        onSubmitVipRequest(user, selectedTier, selectedMethod, senderNumber, trxId, amount) { res ->
                                            scope.launch {
                                                res.fold(
                                                    onSuccess = {
                                                        trxId = ""
                                                        snackbarHostState.showSnackbar(strings.vipRequestSubmitted)
                                                    },
                                                    onFailure = { err ->
                                                        formError = err.localizedMessage ?: strings.errorTitle
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = !isSubmitting,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("submit_vip_request_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White)
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text(strings.submitVipRequest)
                            }
                        }
                    }
                }
            }

            item { SectionHeader(title = strings.vipRequestHistory) }

            when (vipRequestsState) {
                is UiState.Loading -> item { LoadingStateView() }
                is UiState.Error -> item { ErrorStateView(message = vipRequestsState.message, onRetry = onRetry) }
                is UiState.Success -> {
                    if (vipRequestsState.data.isEmpty()) {
                        item { EmptyStateView() }
                    } else {
                        items(vipRequestsState.data, key = { it.id }) { req ->
                            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${req.tierEnum().title.get(language)} • TrxID: ${req.transactionId}", style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            text = "${req.methodEnum().shortName} (${req.senderNumber}) • ${req.statusEnum().label.get(language)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(AdminSecurity.formatBdt(req.amountBdt), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = EmeraldPrimary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminNumberRow(
    label: String,
    number: String,
    copyLabel: String,
    onCopy: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = EmeraldSoft.copy(alpha = 0.55f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(label, style = MaterialTheme.typography.labelMedium, color = EmeraldDarkContainer)
                Text(number, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = NavyDeep)
            }
            OutlinedButton(onClick = onCopy, modifier = Modifier.heightIn(min = 42.dp)) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(copyLabel)
            }
        }
    }
}
