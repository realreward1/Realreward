package com.example.features.admin.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.OndemandVideo
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

private data class TaskConfirmAction(
    val title: String,
    val message: String,
    val destructive: Boolean,
    val execute: () -> Unit
)

@Composable
fun AdminTasksAdsFolder(
    appConfig: AppConfigDoc,
    tasksState: UiState<List<MicroTaskDoc>>,
    language: AppLanguage,
    isBusy: Boolean,
    onRetry: () -> Unit,
    onSaveConfig: (AppConfigDoc) -> Unit,
    onCreateTask: (String, String, TaskCategory, Double, String, String, String, String, String, String, Long, Boolean, Boolean, String) -> Unit,
    onToggleActive: (String, Boolean) -> Unit,
    onDeleteTask: (String) -> Unit
) {
    val strings = LocalStrings.current

    var adsEnabled by remember(appConfig.videoAdsEnabled) { mutableStateOf(appConfig.videoAdsEnabled) }
    var adsComingSoon by remember(appConfig.videoAdsComingSoon) { mutableStateOf(appConfig.videoAdsComingSoon) }
    var adsRewardText by remember(appConfig.videoAdsRewardBdt) { mutableStateOf(appConfig.videoAdsRewardBdt.toString()) }
    var adsTitleBn by remember(appConfig.videoAdsTitleBn) { mutableStateOf(appConfig.videoAdsTitleBn.orEmpty()) }
    var adsTitleEn by remember(appConfig.videoAdsTitleEn) { mutableStateOf(appConfig.videoAdsTitleEn.orEmpty()) }
    var adsUrl by remember(appConfig.videoAdsUrl) { mutableStateOf(appConfig.videoAdsUrl.orEmpty()) }

    var titleBn by remember { mutableStateOf("") }
    var titleEn by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(TaskCategory.SURVEY) }
    var rewardText by remember { mutableStateOf("25") }
    var descBn by remember { mutableStateOf("") }
    var descEn by remember { mutableStateOf("") }
    var instBn by remember { mutableStateOf("১. লিংকে প্রবেশ করুন\n২. কাজটি সম্পন্ন করে স্ক্রিনশট দিন") }
    var instEn by remember { mutableStateOf("1. Open task link\n2. Complete steps and upload proof") }
    var taskLink by remember { mutableStateOf("https://realreward.bd") }
    var deadline by remember { mutableStateOf("Today 11:59 PM") }
    var slotsText by remember { mutableStateOf("100") }
    var featured by remember { mutableStateOf(true) }
    var instantCredit by remember { mutableStateOf(true) }
    var verificationCode by remember { mutableStateOf("") }

    var pendingConfirm by remember { mutableStateOf<TaskConfirmAction?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.OndemandVideo, contentDescription = null, tint = GoldAccent)
                        Text(
                            text = "Lifetime Video Ads Control (Task Folder)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Video Ads Active (ON / OFF)", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = adsEnabled, onCheckedChange = { adsEnabled = it })
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Show 'Coming Soon' Status", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = adsComingSoon, onCheckedChange = { adsComingSoon = it })
                    }
                    OutlinedTextField(
                        value = adsRewardText,
                        onValueChange = { adsRewardText = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Reward per Video Ad (৳)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = adsTitleBn,
                        onValueChange = { adsTitleBn = it },
                        label = { Text("Video Ads Folder Title (Bangla, optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = adsTitleEn,
                        onValueChange = { adsTitleEn = it },
                        label = { Text("Video Ads Folder Title (English, optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = adsUrl,
                        onValueChange = { adsUrl = it },
                        label = { Text("Video Ad Network / Rewarded Link URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            pendingConfirm = TaskConfirmAction(
                                title = strings.adminConfirmTitle,
                                message = "Save Lifetime Video Ads configuration (Active=$adsEnabled, ComingSoon=$adsComingSoon)?",
                                destructive = false
                            ) {
                                onSaveConfig(
                                    appConfig.copy(
                                        videoAdsEnabled = adsEnabled,
                                        videoAdsComingSoon = adsComingSoon,
                                        videoAdsRewardBdt = adsRewardText.toDoubleOrNull() ?: appConfig.videoAdsRewardBdt,
                                        videoAdsTitleBn = adsTitleBn.trim().ifBlank { null },
                                        videoAdsTitleEn = adsTitleEn.trim().ifBlank { null },
                                        videoAdsUrl = adsUrl.trim().ifBlank { null }
                                    )
                                )
                            }
                        },
                        enabled = !isBusy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyDeep, contentColor = Color.White)
                    ) {
                        Text("Save Video Ads Settings")
                    }
                }
            }
        }

        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(strings.adminCreateTask, style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(value = titleBn, onValueChange = { titleBn = it }, label = { Text("Task Title (Bangla)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = titleEn, onValueChange = { titleEn = it }, label = { Text("Task Title (English)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(TaskCategory.entries) { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat.title.get(language)) }
                            )
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = rewardText,
                            onValueChange = { rewardText = it.filter { c -> c.isDigit() || c == '.' } },
                            label = { Text("Reward (৳)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = slotsText,
                            onValueChange = { slotsText = it.filter { c -> c.isDigit() } },
                            label = { Text("Slots") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(value = descBn, onValueChange = { descBn = it }, label = { Text("Description (Bangla)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = descEn, onValueChange = { descEn = it }, label = { Text("Description (English)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = instBn, onValueChange = { instBn = it }, label = { Text("Instructions (Bangla)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = instEn, onValueChange = { instEn = it }, label = { Text("Instructions (English)") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = taskLink, onValueChange = { taskLink = it }, label = { Text("Task URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = deadline, onValueChange = { deadline = it }, label = { Text("Deadline Text") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Featured on Dashboard")
                        Switch(checked = featured, onCheckedChange = { featured = it })
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Instant Auto-Credit (Fast Direct Pay)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Text(
                                text = "When ON, completing accurately credits wallet balance immediately",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = instantCredit, onCheckedChange = { instantCredit = it })
                    }
                    if (instantCredit) {
                        OutlinedTextField(
                            value = verificationCode,
                            onValueChange = { verificationCode = it },
                            label = { Text("Verification Code / Answer (Optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Button(
                        onClick = {
                            val reward = rewardText.toDoubleOrNull() ?: 0.0
                            val slots = slotsText.toLongOrNull() ?: 0L
                            val cleanBn = titleBn.trim().ifBlank { titleEn.trim() }
                            val cleanEn = titleEn.trim().ifBlank { titleBn.trim() }
                            if (cleanBn.length >= 2 && reward > 0 && slots >= 1) {
                                pendingConfirm = TaskConfirmAction(
                                    title = strings.adminConfirmTitle,
                                    message = "Publish task '$cleanBn' with reward ${AdminSecurity.formatBdt(reward)} ($slots slots, instantCredit=$instantCredit)?",
                                    destructive = false
                                ) {
                                    onCreateTask(
                                        cleanBn, cleanEn, category, reward,
                                        descBn.ifBlank { cleanBn }, descEn.ifBlank { cleanEn },
                                        instBn, instEn, taskLink, deadline, slots, featured,
                                        instantCredit, verificationCode.trim()
                                    )
                                    titleBn = ""; titleEn = ""; descBn = ""; descEn = ""; verificationCode = ""
                                }
                            }
                        },
                        enabled = !isBusy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.AddTask, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.adminCreateTask)
                    }
                }
            }
        }

        when (tasksState) {
            is UiState.Loading -> item { LoadingStateView() }
            is UiState.Error -> item { ErrorStateView(message = tasksState.message, onRetry = onRetry) }
            is UiState.Success -> {
                items(tasksState.data, key = { it.id }) { task ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(task.title(language), style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = "${task.categoryEnum().title.get(language)} • ${AdminSecurity.formatBdt(task.rewardBdt)} • ${task.availableSlots}/${task.totalSlots} slots",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = EmeraldPrimary
                                    )
                                }
                                Switch(
                                    checked = task.active,
                                    onCheckedChange = { nextActive ->
                                        if (!isBusy) {
                                            pendingConfirm = TaskConfirmAction(
                                                title = strings.adminConfirmTitle,
                                                message = "${if (nextActive) "Activate" else "Pause"} task '${task.title(language)}'?",
                                                destructive = !nextActive
                                            ) { onToggleActive(task.id, nextActive) }
                                        }
                                    }
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    pendingConfirm = TaskConfirmAction(
                                        title = strings.adminConfirmTitle,
                                        message = "Permanently delete task '${task.title(language)}'?",
                                        destructive = true
                                    ) { onDeleteTask(task.id) }
                                },
                                enabled = !isBusy,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.adminDeleteTask)
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
