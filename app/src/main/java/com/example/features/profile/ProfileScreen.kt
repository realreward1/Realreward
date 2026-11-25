package com.example.features.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.core.notifications.AppNotificationHelper
import com.example.core.security.AdminSecurity
import com.example.domain.model.AppConfigDoc
import com.example.domain.model.AppLanguage
import com.example.domain.model.UserAccountDoc
import com.example.ui.components.RealRewardTopBar
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.*
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    firebaseUser: FirebaseUser,
    user: UserAccountDoc,
    appConfig: AppConfigDoc,
    language: AppLanguage,
    notificationSoundEnabled: Boolean,
    isSubmitting: Boolean,
    onToggleNotificationSound: (Boolean) -> Unit,
    onToggleLanguage: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenLegal: (String) -> Unit,
    onUpdateProfile: (String, String, (Result<Unit>) -> Unit) -> Unit,
    onUpdatePaymentAccounts: (String, String, (Result<Unit>) -> Unit) -> Unit,
    onChangePassword: (String, String, (Result<Unit>) -> Unit) -> Unit,
    onUpdateSecurityToggles: (Boolean, Boolean, (Result<Unit>) -> Unit) -> Unit,
    onOpenOwnerAdmin: () -> Unit,
    onLogout: () -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val isOwner = AdminSecurity.isVerifiedOwner(firebaseUser)

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showSecurityDialog by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            RealRewardTopBar(
                title = strings.profileTitle,
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_user_avatar_1795531056978),
                        contentDescription = user.fullName,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .border(2.dp, EmeraldPrimary, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(user.fullName, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Text("@${user.username} • ${user.mobileNumber}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${strings.accountIdLabel}: ${user.accountId}", style = MaterialTheme.typography.labelMedium, color = EmeraldPrimary)
                        Surface(shape = RoundedCornerShape(50), color = GoldSoft) {
                            Text(
                                text = user.vipTierEnum().title.get(language),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = GoldDarkContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            if (isOwner) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenOwnerAdmin)
                        .testTag("profile_owner_admin_card"),
                    shape = RoundedCornerShape(16.dp),
                    color = GoldSoft,
                    border = BorderStroke(1.dp, GoldBright)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = GoldDarkContainer)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.ownerAdminEntryTitle, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = GoldDarkContainer)
                            Text(strings.ownerAdminEntrySubtitle, style = MaterialTheme.typography.bodySmall, color = GoldDarkContainer.copy(alpha = 0.85f))
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = GoldDarkContainer)
                    }
                }
            }

            ProfileMenuItem(Icons.Default.Edit, strings.editProfile) { showEditProfileDialog = true }
            ProfileMenuItem(Icons.Default.Password, strings.changePasswordTitle) { showChangePasswordDialog = true }
            ProfileMenuItem(
                icon = Icons.Default.AccountBalanceWallet,
                title = strings.paymentAccountSettings,
                subtitle = if (user.paymentAccountsLocked) strings.paymentLockedBadge else strings.setupWalletOnceButton
            ) { showPaymentDialog = true }
            ProfileMenuItem(Icons.Default.Security, strings.securitySettings) { showSecurityDialog = true }
            ProfileMenuItem(Icons.Default.Translate, strings.languageSettings, onClick = onToggleLanguage)

            // Notification Sound Switch Card
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenNotifications),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (notificationSoundEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                            contentDescription = null,
                            tint = EmeraldPrimary
                        )
                        Column {
                            Text(strings.notificationSoundLabel, style = MaterialTheme.typography.titleSmall)
                            Text(strings.notificationsTitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = notificationSoundEnabled,
                        onCheckedChange = { enabled ->
                            onToggleNotificationSound(enabled)
                            if (enabled) AppNotificationHelper.playNotificationSound(context)
                        },
                        modifier = Modifier.testTag("profile_notification_sound_switch")
                    )
                }
            }

            // App Download & Share Card
            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DownloadForOffline, contentDescription = null, tint = EmeraldPrimary)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.appDownloadTitle, style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "${strings.currentVersionLabel}: v${BuildConfig.VERSION_NAME} • Latest: v${appConfig.latestAppVersion}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        if (appConfig.appDownloadUrl.isNotBlank()) {
                            Button(
                                onClick = { AppNotificationHelper.openDownloadOrUpdateUrl(context, appConfig.appDownloadUrl) },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("profile_download_app_button"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White)
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
                                    url = appConfig.appDownloadUrl.ifBlank { null },
                                    brandName = strings.brandName,
                                    latestVersion = appConfig.latestAppVersion
                                )
                            },
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("profile_share_app_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.shareAppButton, maxLines = 1)
                        }
                    }
                }
            }

            if (appConfig.supportEnabled) {
                ProfileMenuItem(Icons.Default.SupportAgent, strings.supportTitle, onClick = onOpenSupport)
            }
            ProfileMenuItem(Icons.Default.Policy, strings.legalTitle) { onOpenLegal("about") }

            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .testTag("profile_logout_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = StatusErrorSoft, contentColor = StatusError)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(strings.logout, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }
        }
    }

    if (showEditProfileDialog) {
        var name by remember { mutableStateOf(user.fullName) }
        var username by remember { mutableStateOf(user.username) }
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text(strings.editProfile) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(strings.fullNameLabel) }, singleLine = true)
                    OutlinedTextField(value = username, onValueChange = { username = it.replace(" ", "") }, label = { Text(strings.usernameLabel) }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    enabled = !isSubmitting,
                    onClick = {
                        onUpdateProfile(name, username) { res ->
                            showEditProfileDialog = false
                            scope.launch {
                                res.fold(
                                    onSuccess = { snackbarHostState.showSnackbar(strings.profileUpdatedSuccess) },
                                    onFailure = { snackbarHostState.showSnackbar(it.localizedMessage ?: strings.errorTitle) }
                                )
                            }
                        }
                    }
                ) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    if (showPaymentDialog) {
        var bkash by remember(user.bkashPersonal, user.paymentAccountsLocked) { mutableStateOf(user.bkashPersonal) }
        var nagad by remember(user.nagadPersonal, user.paymentAccountsLocked) { mutableStateOf(user.nagadPersonal) }
        var dialogError by remember { mutableStateOf<String?>(null) }
        val isLocked = user.paymentAccountsLocked
        AlertDialog(
            onDismissRequest = { showPaymentDialog = false },
            title = { Text(strings.paymentAccountSettings) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isLocked || user.walletChangeApprovedOnce) EmeraldSoft else GoldSoft
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = if (isLocked || user.walletChangeApprovedOnce) EmeraldPrimary else GoldDarkContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = when {
                                    isLocked -> strings.walletLockedChangeViaSupport
                                    user.walletChangeApprovedOnce -> strings.walletResetApprovedNotice
                                    else -> strings.paymentSetupWarning
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isLocked || user.walletChangeApprovedOnce) EmeraldDarkContainer else GoldDarkContainer
                            )
                        }
                    }
                    OutlinedTextField(
                        value = bkash,
                        onValueChange = { if (!isLocked) { bkash = it.filter { c -> c.isDigit() || c == '+' }; dialogError = null } },
                        label = { Text(strings.bkashPersonalLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        readOnly = isLocked,
                        enabled = !isLocked,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("profile_bkash_input")
                    )
                    OutlinedTextField(
                        value = nagad,
                        onValueChange = { if (!isLocked) { nagad = it.filter { c -> c.isDigit() || c == '+' }; dialogError = null } },
                        label = { Text(strings.nagadPersonalLabel) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        readOnly = isLocked,
                        enabled = !isLocked,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("profile_nagad_input")
                    )
                    if (isLocked && appConfig.supportEnabled) {
                        OutlinedButton(
                            onClick = {
                                showPaymentDialog = false
                                onOpenSupport()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(strings.requestWalletChangeButton)
                        }
                    }
                    dialogError?.let { err ->
                        Text(err, style = MaterialTheme.typography.bodySmall, color = StatusError)
                    }
                }
            },
            confirmButton = {
                if (!isLocked) {
                    Button(
                        enabled = !isSubmitting,
                        onClick = {
                            if (!AdminSecurity.isValidBdMobile(bkash) || !AdminSecurity.isValidBdMobile(nagad)) {
                                dialogError = strings.errBdMobile
                                return@Button
                            }
                            onUpdatePaymentAccounts(bkash, nagad) { res ->
                                res.fold(
                                    onSuccess = {
                                        showPaymentDialog = false
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
                        modifier = Modifier.testTag("profile_save_payment_button")
                    ) { Text(strings.save) }
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    if (showChangePasswordDialog) {
        val hasExistingPassword = user.securityPasswordHash.isNotBlank()
        var oldPw by remember { mutableStateOf("") }
        var newPw by remember { mutableStateOf("") }
        var confirmPw by remember { mutableStateOf("") }
        var pwError by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            title = { Text(strings.changePasswordTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (hasExistingPassword) {
                        OutlinedTextField(
                            value = oldPw,
                            onValueChange = { oldPw = it; pwError = null },
                            label = { Text(strings.oldPasswordLabel) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("profile_old_password_input")
                        )
                    }
                    OutlinedTextField(
                        value = newPw,
                        onValueChange = { newPw = it; pwError = null },
                        label = { Text(strings.newPasswordLabel) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("profile_new_password_input")
                    )
                    OutlinedTextField(
                        value = confirmPw,
                        onValueChange = { confirmPw = it; pwError = null },
                        label = { Text(strings.confirmPasswordLabel) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("profile_confirm_password_input")
                    )
                    pwError?.let { err ->
                        Text(err, style = MaterialTheme.typography.bodySmall, color = StatusError)
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = !isSubmitting,
                    onClick = {
                        val cleanOld = oldPw.trim()
                        val cleanNew = newPw.trim()
                        val cleanConfirm = confirmPw.trim()
                        when {
                            hasExistingPassword && cleanOld.isBlank() -> pwError = strings.errWrongOldPassword
                            cleanNew.length < 6 -> pwError = strings.errPasswordLength
                            cleanNew != cleanConfirm -> pwError = strings.errPasswordMismatch
                            else -> {
                                onChangePassword(cleanOld, cleanNew) { res ->
                                    res.fold(
                                        onSuccess = {
                                            showChangePasswordDialog = false
                                            scope.launch { snackbarHostState.showSnackbar(strings.passwordUpdatedSuccess) }
                                        },
                                        onFailure = { err ->
                                            pwError = when (err.message) {
                                                "WRONG_OLD_PASSWORD" -> strings.errWrongOldPassword
                                                "PASSWORD_TOO_SHORT" -> strings.errPasswordLength
                                                else -> err.localizedMessage ?: strings.errorTitle
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier.testTag("profile_save_password_button")
                ) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) { Text(strings.cancel) }
            }
        )
    }

    if (showSecurityDialog) {
        var twoFa by remember { mutableStateOf(user.twoFactorEnabled) }
        var alerts by remember { mutableStateOf(user.loginAlertsEnabled) }
        AlertDialog(
            onDismissRequest = { showSecurityDialog = false },
            title = { Text(strings.securitySettings) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(strings.twoFactorAuth, modifier = Modifier.weight(1f))
                        Switch(checked = twoFa, onCheckedChange = { twoFa = it })
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(strings.loginAlerts, modifier = Modifier.weight(1f))
                        Switch(checked = alerts, onCheckedChange = { alerts = it })
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateSecurityToggles(twoFa, alerts) {
                        showSecurityDialog = false
                    }
                }) { Text(strings.save) }
            },
            dismissButton = {
                TextButton(onClick = { showSecurityDialog = false }) { Text(strings.cancel) }
            }
        )
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = EmeraldPrimary)
                Column {
                    Text(title, style = MaterialTheme.typography.titleSmall)
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldPrimary
                        )
                    }
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
