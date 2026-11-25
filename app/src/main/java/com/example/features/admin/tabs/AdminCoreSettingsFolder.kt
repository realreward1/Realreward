package com.example.features.admin.tabs

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.core.security.AdminSecurity
import com.example.domain.model.AppConfigDoc
import com.example.domain.model.AppLanguage
import com.example.ui.components.AdminConfirmDialog
import com.example.ui.components.formatMaintenanceWaitSentence
import com.example.ui.components.formatRemainingCountdown
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.*

private data class CoreConfirmAction(
    val title: String,
    val message: String,
    val destructive: Boolean,
    val execute: () -> Unit
)

@Composable
fun AdminCoreSettingsFolder(
    appConfig: AppConfigDoc,
    isBusy: Boolean,
    onSaveConfig: (AppConfigDoc) -> Unit
) {
    val strings = LocalStrings.current
    var isCoreUnlocked by remember { mutableStateOf(false) }
    var corePasswordInput by remember { mutableStateOf("") }
    var coreAuthError by remember { mutableStateOf<String?>(null) }

    if (!isCoreUnlocked) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(44.dp))
                    Text(strings.adminCoreLockTitle, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    Text(
                        text = strings.adminCoreLockSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = corePasswordInput,
                        onValueChange = { corePasswordInput = it; coreAuthError = null },
                        label = { Text(strings.adminPasscodeLabel) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("core_settings_password_input")
                    )
                    coreAuthError?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = StatusError)
                    }
                    Button(
                        onClick = {
                            if (AdminSecurity.verifyCoreSettingsPassword(corePasswordInput)) {
                                isCoreUnlocked = true
                                corePasswordInput = ""
                            } else {
                                coreAuthError = strings.adminWrongPasscode
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("core_settings_unlock_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyDeep, contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.LockOpen, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.adminUnlockButton)
                    }
                }
            }
        }
        return
    }

    // Maintenance / App Stop State
    var stopMessageBn by remember(appConfig.stopMessageBn) { mutableStateOf(appConfig.stopMessageBn.orEmpty()) }
    var stopMessageEn by remember(appConfig.stopMessageEn) { mutableStateOf(appConfig.stopMessageEn.orEmpty()) }
    var stopHoursText by remember { mutableStateOf("0") }
    var stopMinutesText by remember { mutableStateOf("30") }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(appConfig.appStopped, appConfig.stopEndEpochMillis) {
        nowMillis = System.currentTimeMillis()
        if (appConfig.appStopped && appConfig.stopEndEpochMillis > 0L) {
            while (true) {
                kotlinx.coroutines.delay(1000L)
                nowMillis = System.currentTimeMillis()
            }
        }
    }
    val isLiveMaintenanceActive = appConfig.appStopped &&
        (appConfig.stopEndEpochMillis == 0L || nowMillis < appConfig.stopEndEpochMillis)

    // App Download & Security Shields
    var version by remember(appConfig.latestAppVersion) { mutableStateOf(appConfig.latestAppVersion) }
    var downloadUrl by remember(appConfig.appDownloadUrl) { mutableStateOf(appConfig.appDownloadUrl) }
    var updateMsgBn by remember(appConfig.updateMessageBn) { mutableStateOf(appConfig.updateMessageBn.orEmpty()) }
    var updateMsgEn by remember(appConfig.updateMessageEn) { mutableStateOf(appConfig.updateMessageEn.orEmpty()) }
    var vpnAutoBan by remember(appConfig.vpnAutoBanEnabled) { mutableStateOf(appConfig.vpnAutoBanEnabled) }
    var foreignIpAutoBan by remember(appConfig.foreignIpAutoBanEnabled) { mutableStateOf(appConfig.foreignIpAutoBanEnabled) }
    var supportEnabled by remember(appConfig.supportEnabled) { mutableStateOf(appConfig.supportEnabled) }

    // Payment & VIP Pricing
    var bkash by remember(appConfig.bkashNumber) { mutableStateOf(appConfig.bkashNumber) }
    var nagad by remember(appConfig.nagadNumber) { mutableStateOf(appConfig.nagadNumber) }
    var bronze by remember(appConfig.vipBronzePriceBdt) { mutableStateOf(appConfig.vipBronzePriceBdt.toInt().toString()) }
    var silver by remember(appConfig.vipSilverPriceBdt) { mutableStateOf(appConfig.vipSilverPriceBdt.toInt().toString()) }
    var gold by remember(appConfig.vipGoldPriceBdt) { mutableStateOf(appConfig.vipGoldPriceBdt.toInt().toString()) }
    var diamond by remember(appConfig.vipDiamondPriceBdt) { mutableStateOf(appConfig.vipDiamondPriceBdt.toInt().toString()) }

    var pendingConfirm by remember { mutableStateOf<CoreConfirmAction?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = RoundedCornerShape(50), color = EmeraldSoft) {
                    Text(
                        text = "Core Settings Unlocked",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = EmeraldDarkContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                OutlinedButton(onClick = { isCoreUnlocked = false }) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Lock Core")
                }
            }
        }

        // Folder 1: App Stop / Maintenance Mode + Countdown Timer
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Engineering, contentDescription = null, tint = if (isLiveMaintenanceActive) StatusError else EmeraldPrimary)
                            Text("App Stop / Maintenance & Countdown", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isLiveMaintenanceActive) StatusErrorSoft else EmeraldSoft
                        ) {
                            Text(
                                text = if (isLiveMaintenanceActive) "STOPPED" else "LIVE",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = if (isLiveMaintenanceActive) StatusError else EmeraldDarkContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (isLiveMaintenanceActive) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = StatusErrorSoft,
                            border = BorderStroke(1.dp, StatusError.copy(alpha = 0.5f))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (appConfig.stopEndEpochMillis > 0L) {
                                        "অ্যাপ এখন বন্ধ আছে • বাকি সময়: ${formatRemainingCountdown(appConfig.stopEndEpochMillis, nowMillis)}"
                                    } else {
                                        "অ্যাপ এখন বন্ধ আছে (সময়সীমাহীন / Manual)"
                                    },
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = StatusError
                                )
                                Text(
                                    text = formatMaintenanceWaitSentence(appConfig.stopEndEpochMillis, nowMillis, AppLanguage.BANGLA),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = NavyDeep
                                )
                                Button(
                                    onClick = {
                                        pendingConfirm = CoreConfirmAction(
                                            title = strings.adminConfirmTitle,
                                            message = "Resume app immediately and remove the maintenance banner for all users?",
                                            destructive = false
                                        ) {
                                            onSaveConfig(
                                                appConfig.copy(
                                                    appStopped = false,
                                                    stopEndEpochMillis = 0L
                                                )
                                            )
                                        }
                                    },
                                    enabled = !isBusy,
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("resume_app_now_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("এখনই অ্যাপ পুনরায় চালু করুন (Resume App)")
                                }
                            }
                        }
                    }

                    Text("দ্রুত সময় নির্বাচন করুন (Quick Duration):", style = MaterialTheme.typography.labelMedium)
                    val presets = listOf(
                        Triple(0, 15, "15 মিনিট"),
                        Triple(0, 30, "30 মিনিট"),
                        Triple(1, 0, "1 ঘণ্টা"),
                        Triple(2, 0, "2 ঘণ্টা"),
                        Triple(6, 0, "6 ঘণ্টা"),
                        Triple(24, 0, "24 ঘণ্টা"),
                        Triple(0, 0, "টাইমার ছাড়া")
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(presets) { (h, m, label) ->
                            val selected = (stopHoursText.toIntOrNull() ?: 0) == h && (stopMinutesText.toIntOrNull() ?: 0) == m
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    stopHoursText = h.toString()
                                    stopMinutesText = m.toString()
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = stopHoursText,
                            onValueChange = { stopHoursText = it.filter { c -> c.isDigit() }.take(3) },
                            label = { Text("ঘণ্টা (Hours)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("stop_hours_input")
                        )
                        OutlinedTextField(
                            value = stopMinutesText,
                            onValueChange = { stopMinutesText = it.filter { c -> c.isDigit() }.take(4) },
                            label = { Text("মিনিট (Minutes)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("stop_minutes_input")
                        )
                    }
                    OutlinedTextField(
                        value = stopMessageBn,
                        onValueChange = { stopMessageBn = it },
                        label = { Text("ভাসমান ব্যানার মেসেজ (Bangla)") },
                        placeholder = { Text("অ্যাপসের আপডেটের কাজ চলমান। দয়া করে অপেক্ষা করুন।") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth().testTag("stop_message_bn_input")
                    )
                    OutlinedTextField(
                        value = stopMessageEn,
                        onValueChange = { stopMessageEn = it },
                        label = { Text("Floating Banner Message (English)") },
                        placeholder = { Text("App update is in progress. Please wait.") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth().testTag("stop_message_en_input")
                    )
                    val totalDurationMins = ((stopHoursText.toLongOrNull() ?: 0L) * 60L) +
                        (stopMinutesText.toLongOrNull() ?: 0L)
                    Button(
                        onClick = {
                            pendingConfirm = CoreConfirmAction(
                                title = strings.adminConfirmTitle,
                                message = if (totalDurationMins > 0L) {
                                    "এখনই অ্যাপ স্টপ করে $totalDurationMins মিনিটের কাউন্টডাউন ব্যানার চালু করবেন? সময় শেষ না হওয়া পর্যন্ত ইউজাররা অ্যাপে কোনো কাজ করতে পারবে না।"
                                } else {
                                    "টাইমার ছাড়া ম্যানুয়ালি অ্যাপ স্টপ করবেন? আপনি পুনরায় চালু না করা পর্যন্ত সব ইউজার লক থাকবে।"
                                },
                                destructive = true
                            ) {
                                val targetEndEpoch = if (totalDurationMins > 0L) {
                                    System.currentTimeMillis() + totalDurationMins * 60_000L
                                } else 0L
                                onSaveConfig(
                                    appConfig.copy(
                                        appStopped = true,
                                        stopEndEpochMillis = targetEndEpoch,
                                        stopMessageBn = stopMessageBn.trim().ifBlank { null },
                                        stopMessageEn = stopMessageEn.trim().ifBlank { null }
                                    )
                                )
                            }
                        },
                        enabled = !isBusy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("start_maintenance_stop_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StatusError,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.PauseCircle, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (totalDurationMins > 0L) {
                                "কাউন্টডাউন চালু করে অ্যাপ স্টপ করুন ($totalDurationMins মিনিট)"
                            } else {
                                "এখনই অ্যাপ স্টপ করুন (ম্যানুয়াল)"
                            }
                        )
                    }
                }
            }
        }

        // Folder 2: App Download URL & Anti-Fraud Shields
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = EmeraldPrimary)
                        Text("App Download URL & Security Shield", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    OutlinedTextField(value = version, onValueChange = { version = it }, label = { Text("Latest App Version") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = downloadUrl, onValueChange = { downloadUrl = it }, label = { Text("App Download / APK URL (https://...)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = updateMsgBn, onValueChange = { updateMsgBn = it }, label = { Text("Update Banner (Bangla)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = updateMsgEn, onValueChange = { updateMsgEn = it }, label = { Text("Update Banner (English)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Auto-Ban Active VPN Users", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Switch(checked = vpnAutoBan, onCheckedChange = { vpnAutoBan = it })
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Auto-Ban Non-Bangladesh IP", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Switch(checked = foreignIpAutoBan, onCheckedChange = { foreignIpAutoBan = it })
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(strings.adminSupportOptionToggle, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Switch(checked = supportEnabled, onCheckedChange = { supportEnabled = it })
                    }
                    Button(
                        onClick = {
                            pendingConfirm = CoreConfirmAction(
                                title = strings.adminConfirmTitle,
                                message = "Save App Download URL (v$version), Support Toggle & Auto-Ban shields?",
                                destructive = false
                            ) {
                                onSaveConfig(
                                    appConfig.copy(
                                        latestAppVersion = version.trim().ifBlank { "1.0.0" },
                                        appDownloadUrl = downloadUrl.trim(),
                                        updateMessageBn = updateMsgBn.trim().ifBlank { null },
                                        updateMessageEn = updateMsgEn.trim().ifBlank { null },
                                        vpnAutoBanEnabled = vpnAutoBan,
                                        foreignIpAutoBanEnabled = foreignIpAutoBan,
                                        supportEnabled = supportEnabled
                                    )
                                )
                            }
                        },
                        enabled = !isBusy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyDeep, contentColor = Color.White)
                    ) { Text("Save App Download & Security") }
                }
            }
        }

        // Folder 3: bKash/Nagad Personal Numbers & VIP Tier Pricing
        item {
            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("bKash / Nagad Personal & VIP Tier Pricing", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = bkash,
                        onValueChange = { bkash = it.filter { c -> c.isDigit() } },
                        label = { Text("Admin bKash Personal") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = nagad,
                        onValueChange = { nagad = it.filter { c -> c.isDigit() } },
                        label = { Text("Admin Nagad Personal") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = bronze, onValueChange = { bronze = it.filter { c -> c.isDigit() } }, label = { Text("Bronze ৳") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = silver, onValueChange = { silver = it.filter { c -> c.isDigit() } }, label = { Text("Silver ৳") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = gold, onValueChange = { gold = it.filter { c -> c.isDigit() } }, label = { Text("Gold ৳") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = diamond, onValueChange = { diamond = it.filter { c -> c.isDigit() } }, label = { Text("Diamond ৳") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                    }
                    Button(
                        onClick = {
                            pendingConfirm = CoreConfirmAction(
                                title = strings.adminConfirmTitle,
                                message = "Update Admin bKash ($bkash), Nagad ($nagad) & VIP pricing?",
                                destructive = false
                            ) {
                                onSaveConfig(
                                    appConfig.copy(
                                        bkashNumber = bkash,
                                        nagadNumber = nagad,
                                        vipBronzePriceBdt = bronze.toDoubleOrNull() ?: 0.0,
                                        vipSilverPriceBdt = silver.toDoubleOrNull() ?: 350.0,
                                        vipGoldPriceBdt = gold.toDoubleOrNull() ?: 750.0,
                                        vipDiamondPriceBdt = diamond.toDoubleOrNull() ?: 1500.0
                                    )
                                )
                            }
                        },
                        enabled = !isBusy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White)
                    ) { Text(strings.adminSavePaymentConfig) }
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
