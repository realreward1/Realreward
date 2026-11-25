package com.example.features.tasks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.notifications.AppNotificationHelper
import com.example.core.security.AdminSecurity
import com.example.domain.model.*
import com.example.ui.components.*
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.*

@Composable
fun TasksScreen(
    language: AppLanguage,
    tasksState: UiState<List<MicroTaskDoc>>,
    proofsState: UiState<List<TaskProofDoc>>,
    appConfig: AppConfigDoc,
    onToggleLanguage: () -> Unit,
    onRetry: () -> Unit,
    onOpenTask: (String) -> Unit,
    onOpenNotifications: () -> Unit,
    userVipTier: VipTierCode = VipTierCode.REGULAR
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf<TaskCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val proofsByTaskId = ((proofsState as? UiState.Success)?.data.orEmpty()).associateBy { it.taskId }

    Scaffold(
        topBar = {
            RealRewardTopBar(
                title = strings.tasksPageTitle,
                language = language,
                onToggleLanguage = onToggleLanguage,
                onOpenNotifications = onOpenNotifications
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(strings.searchPlaceholder) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("tasks_search_input")
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text(strings.allCategories) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EmeraldPrimary.copy(alpha = 0.18f))
                    )
                }
                items(TaskCategory.entries) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category.title.get(language)) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = EmeraldPrimary.copy(alpha = 0.18f))
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Lifetime Video Ads Folder (Admin-controlled: Coming Soon / On / Off)
                item {
                    LifetimeVideoAdsFolderCard(
                        appConfig = appConfig,
                        language = language,
                        onWatchAdClick = {
                            val adUrl = appConfig.videoAdsUrl.orEmpty()
                            if (adUrl.isNotBlank()) {
                                AppNotificationHelper.openDownloadOrUpdateUrl(context, adUrl)
                            }
                        }
                    )
                }

                when (tasksState) {
                    is UiState.Loading -> item { LoadingStateView(modifier = Modifier.padding(top = 12.dp)) }
                    is UiState.Error -> item { ErrorStateView(message = tasksState.message, onRetry = onRetry) }
                    is UiState.Success -> {
                        val filtered = tasksState.data.filter { task ->
                            val catMatch = selectedCategory == null || task.categoryEnum() == selectedCategory
                            val q = searchQuery.trim().lowercase()
                            val textMatch = q.isEmpty() ||
                                task.titleBn.lowercase().contains(q) ||
                                task.titleEn.lowercase().contains(q) ||
                                task.descriptionBn.lowercase().contains(q) ||
                                task.descriptionEn.lowercase().contains(q)
                            catMatch && textMatch
                        }

                        if (filtered.isEmpty()) {
                            item {
                                EmptyStateView(
                                    actionLabel = if (selectedCategory != null || searchQuery.isNotBlank()) strings.allCategories else null,
                                    onAction = {
                                        selectedCategory = null
                                        searchQuery = ""
                                    }
                                )
                            }
                        } else {
                            items(filtered, key = { it.id }) { task ->
                                TaskListCard(
                                    task = task,
                                    userProofStatus = proofsByTaskId[task.id]?.statusEnum(),
                                    language = language,
                                    onViewTask = { onOpenTask(task.id) },
                                    userVipTier = userVipTier
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LifetimeVideoAdsFolderCard(
    appConfig: AppConfigDoc,
    language: AppLanguage,
    onWatchAdClick: () -> Unit
) {
    val strings = LocalStrings.current
    val isLive = appConfig.videoAdsEnabled && !appConfig.videoAdsComingSoon
    val isComingSoon = appConfig.videoAdsComingSoon

    val statusText = when {
        isComingSoon -> strings.videoAdsComingSoonTag
        isLive -> strings.videoAdsLiveTag
        else -> strings.videoAdsPausedTag
    }
    val statusBg = when {
        isComingSoon -> GoldSoft
        isLive -> EmeraldSoft
        else -> StatusErrorSoft
    }
    val statusFg = when {
        isComingSoon -> GoldDarkContainer
        isLive -> EmeraldDarkContainer
        else -> StatusError
    }

    val title = when {
        language == AppLanguage.BANGLA && !appConfig.videoAdsTitleBn.isNullOrBlank() -> appConfig.videoAdsTitleBn
        language == AppLanguage.ENGLISH && !appConfig.videoAdsTitleEn.isNullOrBlank() -> appConfig.videoAdsTitleEn
        else -> strings.videoAdsFolderTitle
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(colors = listOf(NavyDeep, NavyPrimary, NavySurface)))
            .padding(16.dp)
            .testTag("video_ads_lifetime_folder_card")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(GoldBright.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayCircleFilled, contentDescription = null, tint = GoldBright, modifier = Modifier.size(28.dp))
                    }
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "${strings.rewardLabel}: ${AdminSecurity.formatBdt(appConfig.videoAdsRewardBdt)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldBright
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = statusBg,
                    border = BorderStroke(1.dp, statusFg.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = statusFg,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Text(
                text = strings.videoAdsFolderSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.82f)
            )

            if (isLive && !appConfig.videoAdsUrl.isNullOrBlank()) {
                Button(
                    onClick = onWatchAdClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("video_ads_watch_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White)
                ) {
                    Icon(Icons.Default.OndemandVideo, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(strings.videoAdsWatchButton)
                }
            }
        }
    }
}
