package com.example.features.notifications

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.core.notifications.AppNotificationHelper
import com.example.domain.model.*
import com.example.ui.components.*
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.*

@Composable
fun NotificationsScreen(
    language: AppLanguage,
    announcementsState: UiState<List<AnnouncementDoc>>,
    appConfig: AppConfigDoc,
    notificationSoundEnabled: Boolean,
    onToggleNotificationSound: (Boolean) -> Unit,
    onToggleLanguage: () -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf(NotificationCategory.ALL) }
    var permissionGranted by remember {
        mutableStateOf(AppNotificationHelper.hasNotificationPermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted || AppNotificationHelper.hasNotificationPermission(context)
    }

    val announcementsList = (announcementsState as? UiState.Success)?.data.orEmpty()
    val filteredList = announcementsList.filter {
        selectedFilter == NotificationCategory.ALL || it.categoryEnum() == selectedFilter
    }
    val downloadUrl = appConfig.appDownloadUrl.trim().ifBlank {
        announcementsList.firstOrNull { !it.updateUrl.isNullOrBlank() }?.updateUrl.orEmpty()
    }

    Scaffold(
        topBar = {
            RealRewardTopBar(
                title = strings.notificationsTitle,
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Notification System & Alert Sound Settings Card
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (notificationSoundEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                                    contentDescription = null,
                                    tint = if (notificationSoundEnabled) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = strings.notificationSoundLabel,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (permissionGranted) EmeraldSoft else GoldSoft
                            ) {
                                Text(
                                    text = if (permissionGranted) strings.notificationsEnabledTag else strings.enableNotificationsButton,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (permissionGranted) EmeraldDarkContainer else GoldDarkContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.notificationSoundDesc,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f).padding(end = 12.dp)
                            )
                            Switch(
                                checked = notificationSoundEnabled,
                                onCheckedChange = { enabled ->
                                    onToggleNotificationSound(enabled)
                                    if (enabled) {
                                        AppNotificationHelper.playNotificationSound(context)
                                    }
                                },
                                modifier = Modifier.testTag("notification_sound_switch")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (!permissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                OutlinedButton(
                                    onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("enable_notifications_button"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(strings.enableNotificationsButton, maxLines = 1)
                                }
                            }
                            Button(
                                onClick = {
                                    if (!permissionGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    AppNotificationHelper.showSystemNotification(
                                        context = context,
                                        notificationId = 9001,
                                        title = strings.testNotificationTitle,
                                        message = strings.testNotificationBody,
                                        withSound = notificationSoundEnabled
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("test_notification_sound_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldPrimary,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.testNotificationSoundButton, maxLines = 1)
                            }
                        }
                    }
                }
            }

            // 2. App Download & Version Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = EmeraldSoft,
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.DownloadForOffline, contentDescription = null, tint = EmeraldDarkContainer)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = strings.appDownloadTitle,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = EmeraldDarkContainer
                                )
                                Text(
                                    text = "${strings.currentVersionLabel}: v${BuildConfig.VERSION_NAME} • Latest: v${appConfig.latestAppVersion}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = EmeraldDarkContainer.copy(alpha = 0.85f)
                                )
                            }
                        }

                        Text(
                            text = if (downloadUrl.isNotBlank()) strings.appDownloadSubtitle else strings.downloadLinkUnavailable,
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldDarkContainer.copy(alpha = 0.9f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (downloadUrl.isNotBlank()) {
                                Button(
                                    onClick = { AppNotificationHelper.openDownloadOrUpdateUrl(context, downloadUrl) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .testTag("notifications_download_app_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = NavyDeep,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(strings.downloadAppButton, maxLines = 1)
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    AppNotificationHelper.shareAppDownload(
                                        context = context,
                                        url = downloadUrl.ifBlank { null },
                                        brandName = strings.brandName,
                                        latestVersion = appConfig.latestAppVersion
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("notifications_share_app_button"),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, EmeraldDarkContainer.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = EmeraldDarkContainer, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.shareAppButton, color = EmeraldDarkContainer, maxLines = 1)
                            }
                        }
                    }
                }
            }

            // 3. Filter Chips
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(NotificationCategory.entries) { cat ->
                        FilterChip(
                            selected = selectedFilter == cat,
                            onClick = { selectedFilter = cat },
                            label = { Text(cat.title.get(language)) }
                        )
                    }
                }
            }

            // 4. Announcements Stream
            when (announcementsState) {
                is UiState.Loading -> item { LoadingStateView() }
                is UiState.Error -> item { ErrorStateView(message = announcementsState.message, onRetry = onRetry) }
                is UiState.Success -> {
                    if (filteredList.isEmpty()) {
                        item { EmptyStateView() }
                    } else {
                        items(filteredList, key = { it.id }) { item ->
                            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Campaign, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                                            Text(item.categoryEnum().title.get(language), style = MaterialTheme.typography.labelMedium, color = EmeraldPrimary)
                                        }
                                        Text(formatTimestamp(item.createdAt, language), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(item.title(language), style = MaterialTheme.typography.titleMedium)
                                    Text(item.message(language), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (!item.appVersion.isNullOrBlank()) {
                                        Text("Version: ${item.appVersion}", style = MaterialTheme.typography.labelSmall, color = GoldAccent)
                                    }
                                    if (!item.updateUrl.isNullOrBlank()) {
                                        Button(
                                            onClick = { AppNotificationHelper.openDownloadOrUpdateUrl(context, item.updateUrl) },
                                            modifier = Modifier.heightIn(min = 42.dp),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = StatusInfoSoft, contentColor = StatusInfo)
                                        ) {
                                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text(strings.downloadAppButton)
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
