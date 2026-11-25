package com.example.features.admin.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.*
import com.example.ui.components.AdminConfirmDialog
import com.example.ui.components.SectionHeader
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.*

private data class NoticeConfirmAction(
    val title: String,
    val message: String,
    val execute: () -> Unit
)

@Composable
fun AdminNoticesFolder(
    appConfig: AppConfigDoc,
    announcementsState: UiState<List<AnnouncementDoc>>,
    language: AppLanguage,
    isBusy: Boolean,
    onSaveTopNotice: (AppConfigDoc) -> Unit,
    onBroadcast: (NotificationCategory, String, String, String, String, String?, String?) -> Unit
) {
    val strings = LocalStrings.current

    // Extra Dashboard Top Notice state
    var topNoticeActive by remember(appConfig.dashboardTopNoticeActive) { mutableStateOf(appConfig.dashboardTopNoticeActive) }
    var topNoticeBn by remember(appConfig.dashboardTopNoticeBn) { mutableStateOf(appConfig.dashboardTopNoticeBn.orEmpty()) }
    var topNoticeEn by remember(appConfig.dashboardTopNoticeEn) { mutableStateOf(appConfig.dashboardTopNoticeEn.orEmpty()) }

    // Push Announcement Form
    var category by remember { mutableStateOf(NotificationCategory.ANNOUNCEMENT) }
    var titleBn by remember { mutableStateOf("") }
    var titleEn by remember { mutableStateOf("") }
    var noticeBn by remember { mutableStateOf("") }
    var noticeEn by remember { mutableStateOf("") }
    var appVersion by remember { mutableStateOf("") }
    var updateUrl by remember { mutableStateOf("") }

    var pendingConfirm by remember { mutableStateOf<NoticeConfirmAction?>(null) }
    val announcements = (announcementsState as? UiState.Success)?.data.orEmpty()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Folder A: Extra Dashboard Top Notice Banner
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Campaign, contentDescription = null, tint = GoldAccent)
                        Text(
                            text = "Extra Dashboard Top Notice Banner",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Show Top Notice on User Dashboard", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = topNoticeActive, onCheckedChange = { topNoticeActive = it })
                    }
                    OutlinedTextField(
                        value = topNoticeBn,
                        onValueChange = { topNoticeBn = it },
                        label = { Text("Dashboard Top Notice (Bangla)") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = topNoticeEn,
                        onValueChange = { topNoticeEn = it },
                        label = { Text("Dashboard Top Notice (English)") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            pendingConfirm = NoticeConfirmAction(
                                title = strings.adminConfirmTitle,
                                message = if (topNoticeActive) {
                                    "Publish this Extra Top Notice onto every user's Dashboard?"
                                } else {
                                    "Hide the Extra Top Notice from Dashboard?"
                                }
                            ) {
                                onSaveTopNotice(
                                    appConfig.copy(
                                        dashboardTopNoticeActive = topNoticeActive,
                                        dashboardTopNoticeBn = topNoticeBn.trim().ifBlank { null },
                                        dashboardTopNoticeEn = topNoticeEn.trim().ifBlank { null }
                                    )
                                )
                            }
                        },
                        enabled = !isBusy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent, contentColor = Color.White)
                    ) {
                        Text("Save Dashboard Top Notice")
                    }
                }
            }
        }

        // Folder B: Broadcast Notifications / App Updates
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = EmeraldPrimary)
                        Text(strings.adminSendUpdateNotice, style = MaterialTheme.typography.titleMedium)
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(NotificationCategory.entries.filter { it != NotificationCategory.ALL }) { cat ->
                            FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat.title.get(language)) })
                        }
                    }
                    OutlinedTextField(value = titleBn, onValueChange = { titleBn = it }, label = { Text("Title (Bangla)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = titleEn, onValueChange = { titleEn = it }, label = { Text("Title (English)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = noticeBn, onValueChange = { noticeBn = it }, label = { Text("Message (Bangla)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = noticeEn, onValueChange = { noticeEn = it }, label = { Text("Message (English)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = appVersion, onValueChange = { appVersion = it }, label = { Text("App Version (opt)") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = updateUrl, onValueChange = { updateUrl = it }, label = { Text("Update URL (opt)") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    Button(
                        onClick = {
                            val tBn = titleBn.trim().ifBlank { titleEn.trim() }
                            val tEn = titleEn.trim().ifBlank { titleBn.trim() }
                            val mBn = noticeBn.trim().ifBlank { noticeEn.trim() }
                            val mEn = noticeEn.trim().ifBlank { noticeBn.trim() }
                            if (tBn.isNotBlank() && mBn.isNotBlank()) {
                                pendingConfirm = NoticeConfirmAction(
                                    title = strings.adminConfirmTitle,
                                    message = "Broadcast notice '$tBn' with push sound alert to all users?"
                                ) {
                                    onBroadcast(category, tBn, tEn, mBn, mEn, appVersion, updateUrl)
                                    titleBn = ""; titleEn = ""; noticeBn = ""; noticeEn = ""
                                }
                            }
                        },
                        enabled = !isBusy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyDeep, contentColor = Color.White)
                    ) { Text(strings.adminSendUpdateNotice) }
                }
            }
        }

        item { SectionHeader(title = "Recent Broadcasts (${announcements.size})") }
        items(announcements.take(12), key = { it.id }) { a ->
            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(a.title(language), style = MaterialTheme.typography.titleSmall)
                    Text(a.message(language), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    pendingConfirm?.let { action ->
        AdminConfirmDialog(
            title = action.title,
            message = action.message,
            isDestructive = false,
            onConfirm = action.execute,
            onDismiss = { pendingConfirm = null }
        )
    }
}
