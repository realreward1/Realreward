package com.example.features.admin.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
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
import com.example.domain.model.*
import com.example.ui.components.*
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.*

private data class SupportConfirmAction(
    val title: String,
    val message: String,
    val destructive: Boolean,
    val execute: () -> Unit
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminSupportFolder(
    ticketsState: UiState<List<SupportTicketDoc>>,
    usersState: UiState<List<UserAccountDoc>>,
    appConfig: AppConfigDoc,
    language: AppLanguage,
    isBusy: Boolean,
    onRetry: () -> Unit,
    onUpdateTicket: (String, TicketStatus, String) -> Unit,
    onDeleteTickets: (List<String>) -> Unit,
    onBanUser: (String, String) -> Unit,
    onAllowWalletChangeOnce: (String) -> Unit,
    onToggleSupportEnabled: (Boolean) -> Unit
) {
    val strings = LocalStrings.current
    val tickets = (ticketsState as? UiState.Success)?.data.orEmpty()
    val usersById = remember(usersState) {
        ((usersState as? UiState.Success)?.data.orEmpty()).associateBy { it.userId }
    }

    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    LaunchedEffect(tickets) {
        val validIds = tickets.map { it.id }.toSet()
        if (selectedIds.any { it !in validIds }) {
            selectedIds = selectedIds.intersect(validIds)
        }
    }

    var replyTarget by remember { mutableStateOf<SupportTicketDoc?>(null) }
    var showCustomSelectDialog by remember { mutableStateOf(false) }
    var pendingConfirm by remember { mutableStateOf<SupportConfirmAction?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Master Support Option Visibility Toggle Card (Hides Support on Dashboard & App when OFF)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(14.dp),
            color = if (appConfig.supportEnabled) EmeraldSoft else StatusErrorSoft,
            border = BorderStroke(
                1.dp,
                if (appConfig.supportEnabled) EmeraldPrimary.copy(alpha = 0.45f) else StatusError.copy(alpha = 0.45f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (appConfig.supportEnabled) Icons.Default.SupportAgent else Icons.Default.CommentsDisabled,
                        contentDescription = null,
                        tint = if (appConfig.supportEnabled) EmeraldDarkContainer else StatusError
                    )
                    Text(
                        text = strings.adminSupportOptionToggle,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (appConfig.supportEnabled) EmeraldDarkContainer else StatusError
                    )
                }
                Switch(
                    checked = appConfig.supportEnabled,
                    enabled = !isBusy,
                    onCheckedChange = { next ->
                        pendingConfirm = SupportConfirmAction(
                            title = strings.adminConfirmTitle,
                            message = if (next) {
                                "Turn ON Support Messaging option on Dashboard & App?"
                            } else {
                                "Turn OFF Support Messaging? Regular users will no longer see the Support option on Dashboard."
                            },
                            destructive = !next
                        ) {
                            onToggleSupportEnabled(next)
                        }
                    },
                    modifier = Modifier.testTag("admin_support_enabled_switch")
                )
            }
        }

        when (ticketsState) {
            is UiState.Loading -> LoadingStateView()
            is UiState.Error -> ErrorStateView(message = ticketsState.message, onRetry = onRetry)
            is UiState.Success -> {
                if (tickets.isEmpty()) {
                    EmptyStateView()
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SectionHeader(title = "${strings.adminTabSupport} (${tickets.size})")
                                    if (selectedIds.isNotEmpty()) {
                                        Surface(shape = RoundedCornerShape(50), color = NavyDeep) {
                                            Text(
                                                text = "${strings.adminSelectedCountLabel}: ${selectedIds.size}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = GoldSoft,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                // Select / Select All / Custom Select / Delete Toolbar
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val allSelected = selectedIds.size == tickets.size && tickets.isNotEmpty()
                                    OutlinedButton(
                                        onClick = {
                                            selectedIds = if (allSelected) emptySet() else tickets.map { it.id }.toSet()
                                        },
                                        enabled = !isBusy,
                                        modifier = Modifier.heightIn(min = 48.dp).testTag("admin_support_select_all_btn")
                                    ) {
                                        Icon(
                                            imageVector = if (allSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(if (allSelected) strings.adminDeselectAll else strings.adminSelectAll)
                                    }

                                    OutlinedButton(
                                        onClick = { showCustomSelectDialog = true },
                                        enabled = !isBusy,
                                        modifier = Modifier.heightIn(min = 48.dp).testTag("admin_support_custom_select_btn")
                                    ) {
                                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(strings.adminCustomSelect)
                                    }

                                    Button(
                                        onClick = {
                                            val toDelete = selectedIds.toList()
                                            pendingConfirm = SupportConfirmAction(
                                                title = strings.adminConfirmTitle,
                                                message = "Delete ${toDelete.size} selected support message(s) permanently?",
                                                destructive = true
                                            ) {
                                                onDeleteTickets(toDelete)
                                                selectedIds = emptySet()
                                            }
                                        },
                                        enabled = !isBusy && selectedIds.isNotEmpty(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = StatusError,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier.heightIn(min = 48.dp).testTag("admin_support_delete_selected_btn")
                                    ) {
                                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("${strings.adminDeleteSelected} (${selectedIds.size})")
                                    }
                                }
                            }
                        }

                        items(tickets, key = { it.id }) { ticket ->
                            val senderUser = usersById[ticket.userId]
                            val senderFullName = ticket.fullName.ifBlank { senderUser?.fullName ?: ticket.username }
                            val alreadyBanned = senderUser?.banned == true
                            val walletLocked = senderUser?.paymentAccountsLocked == true
                            val isSelected = ticket.id in selectedIds

                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = if (isSelected) EmeraldSoft.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                                )
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
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { checked ->
                                                selectedIds = if (checked) selectedIds + ticket.id else selectedIds - ticket.id
                                            },
                                            modifier = Modifier.testTag("support_checkbox_${ticket.id}")
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "$senderFullName (@${ticket.username})",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = "${ticket.subject} • ${ticket.categoryEnum().title.get(language)} • ${formatTimestamp(ticket.createdAt, language)}",
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

                                    // Sender message chat bubble
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = StatusInfoSoft,
                                        border = BorderStroke(1.dp, StatusInfo.copy(alpha = 0.25f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(ticket.message, style = MaterialTheme.typography.bodyMedium, color = NavyDeep)
                                            ticket.attachmentLabel?.takeIf { it.isNotBlank() }?.let { file ->
                                                Text("Attachment: $file", style = MaterialTheme.typography.labelSmall, color = StatusInfo)
                                            }
                                        }
                                    }

                                    // Admin Reply bubble if sent
                                    ticket.adminReply?.takeIf { it.isNotBlank() }?.let { reply ->
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            color = GoldSoft,
                                            border = BorderStroke(1.dp, GoldBright.copy(alpha = 0.45f))
                                        ) {
                                            Text(
                                                text = "${strings.adminReplyLabel}: $reply",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = GoldDarkContainer,
                                                modifier = Modifier.padding(10.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { replyTarget = ticket },
                                            enabled = !isBusy,
                                            colors = ButtonDefaults.buttonColors(containerColor = NavyDeep, contentColor = Color.White),
                                            modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(strings.adminReplyTicket, maxLines = 1)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                pendingConfirm = SupportConfirmAction(
                                                    title = strings.adminConfirmTitle,
                                                    message = "Permanently ban $senderFullName (@${ticket.username}) for abusive support message?",
                                                    destructive = true
                                                ) {
                                                    onBanUser(ticket.userId, "Abusive support message: ${ticket.subject}")
                                                }
                                            },
                                            enabled = !isBusy && !alreadyBanned,
                                            border = BorderStroke(1.dp, if (alreadyBanned) MaterialTheme.colorScheme.outline else StatusError),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = if (alreadyBanned) MaterialTheme.colorScheme.onSurfaceVariant else StatusError
                                            ),
                                            modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                                        ) {
                                            Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = if (alreadyBanned) strings.adminBannedTag else strings.adminBanFromSupport,
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    // One-Time Wallet Re-Setup Approval Button
                                    if (senderUser != null) {
                                        OutlinedButton(
                                            onClick = {
                                                pendingConfirm = SupportConfirmAction(
                                                    title = strings.adminConfirmTitle,
                                                    message = "Grant $senderFullName (@${ticket.username}) 1-time permission to re-add/change bKash & Nagad numbers? It will permanently lock again after 1 save.",
                                                    destructive = false
                                                ) {
                                                    onAllowWalletChangeOnce(ticket.userId)
                                                }
                                            },
                                            enabled = !isBusy && walletLocked,
                                            border = BorderStroke(1.dp, if (walletLocked) GoldAccent else EmeraldPrimary),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = if (walletLocked) GoldDarkContainer else EmeraldPrimary
                                            ),
                                            modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (walletLocked) Icons.Default.LockOpen else Icons.Default.Verified,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = if (walletLocked) {
                                                    strings.adminAllowWalletChangeOnce
                                                } else {
                                                    strings.adminWalletAlreadyUnlockedTag
                                                }
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

    if (showCustomSelectDialog) {
        var filterStatus by remember { mutableStateOf<TicketStatus?>(null) }
        var filterCategory by remember { mutableStateOf<SupportCategory?>(null) }
        var keyword by remember { mutableStateOf("") }
        var countText by remember { mutableStateOf("") }
        var fromOldest by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showCustomSelectDialog = false },
            title = { Text(strings.adminCustomSelect) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Filter by Status:", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = filterStatus == null,
                            onClick = { filterStatus = null },
                            label = { Text(strings.filterAll) }
                        )
                        TicketStatus.entries.forEach { st ->
                            FilterChip(
                                selected = filterStatus == st,
                                onClick = { filterStatus = st },
                                label = { Text(st.label.get(language)) }
                            )
                        }
                    }
                    Text("Filter by Category:", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = filterCategory == null,
                            onClick = { filterCategory = null },
                            label = { Text(strings.filterAll) }
                        )
                        SupportCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = filterCategory == cat,
                                onClick = { filterCategory = cat },
                                label = { Text(cat.title.get(language)) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = keyword,
                        onValueChange = { keyword = it },
                        label = { Text("Username / Subject Keyword (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = countText,
                        onValueChange = { countText = it.filter { c -> c.isDigit() } },
                        label = { Text("Limit Count (optional, e.g. 10)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = fromOldest,
                            onClick = { fromOldest = true },
                            label = { Text("Oldest First") }
                        )
                        FilterChip(
                            selected = !fromOldest,
                            onClick = { fromOldest = false },
                            label = { Text("Newest First") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val q = keyword.trim().lowercase()
                    var matched = tickets.filter { t ->
                        (filterStatus == null || t.statusEnum() == filterStatus) &&
                            (filterCategory == null || t.categoryEnum() == filterCategory) &&
                            (q.isEmpty() || t.username.lowercase().contains(q) ||
                                t.fullName.lowercase().contains(q) ||
                                t.subject.lowercase().contains(q) ||
                                t.message.lowercase().contains(q))
                    }
                    if (fromOldest) matched = matched.reversed()
                    val limit = countText.toIntOrNull()
                    if (limit != null && limit > 0) matched = matched.take(limit)
                    selectedIds = matched.map { it.id }.toSet()
                    showCustomSelectDialog = false
                }) { Text(strings.okConfirm) }
            },
            dismissButton = {
                TextButton(onClick = { showCustomSelectDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    replyTarget?.let { ticket ->
        var replyText by remember { mutableStateOf(ticket.adminReply.orEmpty()) }
        var status by remember { mutableStateOf(TicketStatus.RESOLVED) }
        AlertDialog(
            onDismissRequest = { replyTarget = null },
            title = { Text("${strings.adminReplyTicket}: ${ticket.fullName.ifBlank { ticket.username }}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TicketStatus.entries.forEach { st ->
                            FilterChip(selected = status == st, onClick = { status = st }, label = { Text(st.name) })
                        }
                    }
                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        label = { Text(strings.adminReplyLabel) },
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    replyTarget = null
                    pendingConfirm = SupportConfirmAction(
                        title = strings.adminConfirmTitle,
                        message = "Send reply to @${ticket.username} and set status to ${status.name}?",
                        destructive = false
                    ) { onUpdateTicket(ticket.id, status, replyText.trim()) }
                }) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { replyTarget = null }) { Text(strings.cancel) }
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
