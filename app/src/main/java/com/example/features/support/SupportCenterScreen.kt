package com.example.features.support

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.*
import com.example.ui.components.*
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SupportCenterScreen(
    user: UserAccountDoc,
    language: AppLanguage,
    ticketsState: UiState<List<SupportTicketDoc>>,
    isSubmitting: Boolean,
    supportEnabled: Boolean = true,
    onToggleLanguage: () -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSubmitTicket: (UserAccountDoc, SupportCategory, String, String, String?, (Result<Unit>) -> Unit) -> Unit
) {
    val strings = LocalStrings.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var selectedCategory by remember { mutableStateOf(SupportCategory.TASK_ISSUE) }
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var attachedFile by remember { mutableStateOf<String?>(null) }
    var formError by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) attachedFile = uri.lastPathSegment ?: "support_image.jpg"
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            RealRewardTopBar(
                title = strings.supportTitle,
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
            // Direct In-App Support Banner (No Gmail)
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = StatusInfoSoft,
                    border = BorderStroke(1.dp, StatusInfo.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.SupportAgent, contentDescription = null, tint = StatusInfo)
                        Text(
                            text = strings.supportDirectAdminNotice,
                            style = MaterialTheme.typography.bodySmall,
                            color = NavyDeep
                        )
                    }
                }
            }

            if (!supportEnabled) {
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth().testTag("support_disabled_card"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = StatusErrorSoft)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CommentsDisabled, contentDescription = null, tint = StatusError)
                                Text(
                                    text = strings.supportDisabledTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = StatusError
                                )
                            }
                            Text(
                                text = strings.supportDisabledBody,
                                style = MaterialTheme.typography.bodyMedium,
                                color = NavyDeep
                            )
                        }
                    }
                }
            } else {
                item {
                    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(strings.createTicketTitle, style = MaterialTheme.typography.titleMedium)
                            Text(strings.issueCategoryLabel, style = MaterialTheme.typography.labelLarge)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(SupportCategory.entries) { cat ->
                                    FilterChip(
                                        selected = selectedCategory == cat,
                                        onClick = { selectedCategory = cat },
                                        label = { Text(cat.title.get(language)) }
                                    )
                                }
                            }

                            if (user.paymentAccountsLocked) {
                                OutlinedButton(
                                    onClick = {
                                        selectedCategory = SupportCategory.WALLET_WITHDRAWAL
                                        subject = strings.supportWalletChangePreset
                                        formError = null
                                    },
                                    modifier = Modifier.fillMaxWidth().testTag("support_wallet_change_preset_btn"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(strings.supportWalletChangePreset)
                                }
                            }

                            OutlinedTextField(
                                value = subject,
                                onValueChange = { subject = it; formError = null },
                                label = { Text(strings.subjectLabel) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("support_subject_input")
                            )

                            OutlinedTextField(
                                value = message,
                                onValueChange = { message = it; formError = null },
                                label = { Text(strings.messageLabel) },
                                minLines = 3,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("support_message_input")
                            )

                            OutlinedButton(
                                onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Default.AttachFile, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(attachedFile ?: strings.attachFileButton)
                            }

                            formError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = StatusError) }

                            Button(
                                onClick = {
                                    when {
                                        subject.trim().length < 3 -> formError = strings.errTicketSubject
                                        message.trim().length < 5 -> formError = strings.errTicketMessage
                                        else -> {
                                            onSubmitTicket(user, selectedCategory, subject, message, attachedFile) { res ->
                                                scope.launch {
                                                    res.fold(
                                                        onSuccess = {
                                                            subject = ""
                                                            message = ""
                                                            attachedFile = null
                                                            snackbarHostState.showSnackbar(strings.ticketSubmittedSuccess)
                                                        },
                                                        onFailure = { formError = it.localizedMessage ?: strings.errorTitle }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                },
                                enabled = !isSubmitting,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 50.dp)
                                    .testTag("submit_ticket_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(strings.submitTicketButton)
                            }
                        }
                    }
                }
            }

            item { SectionHeader(title = strings.supportHistoryTitle) }

            when (ticketsState) {
                is UiState.Loading -> item { LoadingStateView() }
                is UiState.Error -> item { ErrorStateView(message = ticketsState.message, onRetry = onRetry) }
                is UiState.Success -> {
                    if (ticketsState.data.isEmpty()) {
                        item { EmptyStateView() }
                    } else {
                        items(ticketsState.data, key = { it.id }) { ticket ->
                            val senderName = ticket.fullName.ifBlank { user.fullName }
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(ticket.subject, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                            Text(
                                                text = "${ticket.categoryEnum().title.get(language)} • ${formatTimestamp(ticket.createdAt, language)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Surface(shape = RoundedCornerShape(50), color = EmeraldSoft) {
                                            Text(
                                                text = ticket.statusEnum().label.get(language),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = EmeraldDarkContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    // User Chat Bubble (Right aligned)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(
                                                topStart = 14.dp,
                                                topEnd = 4.dp,
                                                bottomStart = 14.dp,
                                                bottomEnd = 14.dp
                                            ),
                                            color = EmeraldSoft,
                                            border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.25f)),
                                            modifier = Modifier.fillMaxWidth(0.9f)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "$senderName (@${ticket.username})",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = EmeraldDarkContainer
                                                )
                                                Text(
                                                    text = ticket.message,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = NavyDeep
                                                )
                                                ticket.attachmentLabel?.takeIf { it.isNotBlank() }?.let { file ->
                                                    Text(
                                                        text = "Attachment: $file",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = StatusInfo
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Admin Reply Chat Bubble (Left aligned)
                                    ticket.adminReply?.takeIf { it.isNotBlank() }?.let { reply ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.Start
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(
                                                    topStart = 4.dp,
                                                    topEnd = 14.dp,
                                                    bottomStart = 14.dp,
                                                    bottomEnd = 14.dp
                                                ),
                                                color = GoldSoft,
                                                border = BorderStroke(1.dp, GoldBright.copy(alpha = 0.5f)),
                                                modifier = Modifier.fillMaxWidth(0.9f)
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(12.dp),
                                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Text(
                                                        text = strings.adminReplyLabel,
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = GoldDarkContainer
                                                    )
                                                    Text(
                                                        text = reply,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = NavyDeep
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
        }
    }
}
