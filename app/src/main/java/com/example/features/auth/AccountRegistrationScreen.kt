package com.example.features.auth

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.core.security.AdminSecurity
import com.example.domain.model.AppLanguage
import com.example.ui.components.BrandLogoMark
import com.example.ui.components.LanguageSwitchChip
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.*
import com.google.firebase.auth.FirebaseUser

@Composable
fun AccountRegistrationScreen(
    firebaseUser: FirebaseUser,
    language: AppLanguage,
    isSubmitting: Boolean,
    onToggleLanguage: () -> Unit,
    onRegisterSubmit: (context: Context, fullName: String, username: String, mobile: String, deviceId: String, referralCode: String, password: String, onResult: (Result<Unit>) -> Unit) -> Unit,
    onOpenTerms: () -> Unit,
    onSwitchGoogleAccount: () -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val deviceId = remember(context) { AdminSecurity.getDeviceRegistryId(context) }

    var fullName by remember(firebaseUser.uid) { mutableStateOf(firebaseUser.displayName.orEmpty()) }
    var username by remember(firebaseUser.uid) {
        val suggested = firebaseUser.email?.substringBefore("@")
            ?.replace(Regex("[^a-zA-Z0-9_]"), "_")
            ?.take(20)
            .orEmpty()
        mutableStateOf(suggested)
    }
    var mobile by remember { mutableStateOf("") }
    var referralCode by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var acceptedTerms by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BrandLogoMark()
                LanguageSwitchChip(currentLanguage = language, onToggle = onToggleLanguage)
            }
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
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = EmeraldSoft,
                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CardGiftcard, contentDescription = null, tint = EmeraldPrimary)
                    Text(
                        text = strings.registerBonusHighlight,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = EmeraldDarkContainer
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = GoldSoft,
                border = BorderStroke(1.dp, GoldBright.copy(alpha = 0.55f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PhonelinkLock, contentDescription = null, tint = GoldDarkContainer)
                    Text(
                        text = strings.registerSingleAccountNotice,
                        style = MaterialTheme.typography.bodySmall,
                        color = GoldDarkContainer
                    )
                }
            }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(strings.registerTitle, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = "${strings.registerSubtitle} (${firebaseUser.email.orEmpty()})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it; errorMsg = null },
                        label = { Text(strings.fullNameLabel) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reg_fullname_input")
                    )

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it.replace(" ", ""); errorMsg = null },
                        label = { Text(strings.usernameLabel) },
                        leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reg_username_input")
                    )

                    OutlinedTextField(
                        value = mobile,
                        onValueChange = { mobile = it.filter { c -> c.isDigit() || c == '+' }; errorMsg = null },
                        label = { Text(strings.mobileLabel) },
                        leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reg_mobile_input")
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMsg = null },
                        label = { Text(strings.passwordLabel) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reg_password_input")
                    )

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; errorMsg = null },
                        label = { Text(strings.confirmPasswordLabel) },
                        leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reg_confirm_password_input")
                    )

                    OutlinedTextField(
                        value = referralCode,
                        onValueChange = { referralCode = it.uppercase().replace(" ", ""); errorMsg = null },
                        label = { Text(strings.referralCodeInputLabel) },
                        leadingIcon = { Icon(Icons.Default.GroupAdd, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reg_referral_input")
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = acceptedTerms,
                            onCheckedChange = { acceptedTerms = it; errorMsg = null },
                            modifier = Modifier.testTag("reg_terms_checkbox")
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(strings.acceptTermsLabel, style = MaterialTheme.typography.bodySmall)
                            TextButton(
                                onClick = onOpenTerms,
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(strings.viewTerms, color = EmeraldPrimary)
                            }
                        }
                    }

                    errorMsg?.let { msg ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = StatusErrorSoft
                        ) {
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusError,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val cleanName = fullName.trim()
                            val cleanUser = username.trim()
                            val cleanReferral = referralCode.trim().uppercase()
                            val cleanPassword = password.trim()
                            val cleanConfirm = confirmPassword.trim()
                            when {
                                cleanName.length < 2 -> errorMsg = strings.errFullName
                                cleanUser.length < 3 -> errorMsg = strings.errUsername
                                !AdminSecurity.isValidBdMobile(mobile) -> errorMsg = strings.errBdMobile
                                cleanPassword.length < 6 -> errorMsg = strings.errPasswordLength
                                cleanPassword != cleanConfirm -> errorMsg = strings.errPasswordMismatch
                                !acceptedTerms -> errorMsg = strings.errAcceptTerms
                                else -> {
                                    onRegisterSubmit(context, cleanName, cleanUser, mobile, deviceId, cleanReferral, cleanPassword) { result ->
                                        result.onFailure { err ->
                                            errorMsg = when (err.message) {
                                                "DUPLICATE_MOBILE" -> strings.errDuplicateMobile
                                                "DUPLICATE_DEVICE" -> strings.errDuplicateDevice
                                                "INVALID_REFERRAL_CODE" -> strings.errInvalidReferralCode
                                                "ACCOUNT_REMOVED_PERMANENTLY" -> strings.accountRemovedBody
                                                else -> err.localizedMessage ?: strings.errorTitle
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .testTag("reg_submit_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.HowToReg, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(strings.completeRegistration)
                        }
                    }

                    TextButton(
                        onClick = onSwitchGoogleAccount,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .heightIn(min = 48.dp)
                    ) {
                        Text(strings.switchGoogleAccount)
                    }
                }
            }
        }
    }
}
