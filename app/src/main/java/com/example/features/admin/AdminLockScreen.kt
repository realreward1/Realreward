package com.example.features.admin

import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.core.security.AdminSecurity
import com.example.domain.model.AppLanguage
import com.example.ui.components.RealRewardTopBar
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.*
import com.google.firebase.auth.FirebaseUser

@Composable
fun AdminLockScreen(
    firebaseUser: FirebaseUser?,
    language: AppLanguage,
    onToggleLanguage: () -> Unit,
    onBack: () -> Unit,
    onUnlocked: () -> Unit
) {
    val strings = LocalStrings.current
    var passcode by remember { mutableStateOf("") }
    var obscure by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val isOwner = AdminSecurity.isVerifiedOwner(firebaseUser)

    Scaffold(
        topBar = {
            RealRewardTopBar(
                title = strings.adminLockTitle,
                language = language,
                onToggleLanguage = onToggleLanguage,
                onBack = onBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!isOwner) {
                Icon(Icons.Default.GppBad, contentDescription = null, tint = StatusError, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(12.dp))
                Text(strings.adminWrongPasscode, style = MaterialTheme.typography.titleMedium, color = StatusError)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(strings.back)
                }
                return@Column
            }

            ElevatedCard(
                modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth(),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = GoldAccent,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(strings.adminLockTitle, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    Text(
                        text = "${strings.adminLockSubtitle}\n(${firebaseUser?.email.orEmpty()})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = passcode,
                        onValueChange = { passcode = it.filter { c -> c.isDigit() }; errorMsg = null },
                        label = { Text(strings.adminPasscodeLabel) },
                        visualTransformation = if (obscure) PasswordVisualTransformation() else VisualTransformation.None,
                        trailingIcon = {
                            IconButton(onClick = { obscure = !obscure }) {
                                Icon(
                                    imageVector = if (obscure) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("admin_passcode_input")
                    )

                    errorMsg?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = StatusError)
                    }

                    Button(
                        onClick = {
                            if (AdminSecurity.verifyAdminPasscode(passcode)) {
                                passcode = ""
                                onUnlocked()
                            } else {
                                errorMsg = strings.adminWrongPasscode
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                            .testTag("admin_unlock_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyDeep, contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.LockOpen, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.adminUnlockButton)
                    }
                }
            }
        }
    }
}
