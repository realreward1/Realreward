package com.example.features.auth

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.BuildConfig
import com.example.R
import com.example.core.notifications.AppNotificationHelper
import com.example.data.repository.RealRewardFirestoreRepository
import com.example.domain.model.AppLanguage
import com.example.domain.model.FaqItem
import com.example.ui.components.BrandLogoMark
import com.example.ui.components.LanguageSwitchChip
import com.example.ui.components.LivePlatformPill
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.*

private const val TAG = "AuthLandingScreen"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuthLandingScreen(
    language: AppLanguage,
    onToggleLanguage: () -> Unit,
    onAuthSuccess: () -> Unit,
    onOpenLegal: (String) -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val credentialManager: CredentialManager? = remember(context) {
        try {
            CredentialManager.create(context)
        } catch (e: Exception) {
            Log.w(TAG, "CredentialManager creation failed", e)
            null
        }
    }

    var isSigningIn by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    val faqs: List<FaqItem> = remember {
        try {
            RealRewardFirestoreRepository.getDefaultFaqs()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load default FAQs", e)
            emptyList()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(color = NavyDeep, shadowElevation = 4.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BrandLogoMark(lightText = true, showTagline = true)
                    LanguageSwitchChip(
                        currentLanguage = language,
                        onToggle = onToggleLanguage,
                        darkSurface = true
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(colors = listOf(NavyDeep, NavyPrimary, NavySurface)))
                        .padding(horizontal = 18.dp, vertical = 22.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = 680.dp)
                            .align(Alignment.Center)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LivePlatformPill()
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = GoldBright.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, GoldBright.copy(alpha = 0.65f))
                            ) {
                                Text(
                                    text = strings.signupBonusBadge,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = GoldSoft,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = strings.heroBadge,
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldBright
                        )
                        Text(
                            text = strings.heroTitle,
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = strings.heroSubtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )

                        Image(
                            painter = painterResource(id = R.drawable.img_hero_fintech_1795531052758),
                            contentDescription = strings.heroBadge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(175.dp)
                                .clip(RoundedCornerShape(18.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Button(
                            onClick = {
                                authError = null
                                val manager = credentialManager
                                if (manager == null) {
                                    authError = "Sign-in service is unavailable on this device."
                                    return@Button
                                }
                                isSigningIn = true
                                try {
                                    GoogleAuthHelper.onGoogleSignInClicked(
                                        context = context,
                                        credentialManager = manager,
                                        onAuthSuccess = {
                                            isSigningIn = false
                                            onAuthSuccess()
                                        },
                                        onAuthError = { msg ->
                                            isSigningIn = false
                                            authError = msg
                                        },
                                        scope = scope,
                                        onAuthCancelled = {
                                            isSigningIn = false
                                        }
                                    )
                                } catch (e: Exception) {
                                    Log.e(TAG, "Google sign-in click failed", e)
                                    isSigningIn = false
                                    authError = e.localizedMessage ?: "Failed to start Google sign-in."
                                }
                            },
                            enabled = !isSigningIn,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .testTag("google_sign_in_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = Color.White
                            )
                        ) {
                            if (isSigningIn) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = Color.White,
                                    strokeWidth = 2.5.dp
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(strings.signingInLabel)
                            } else {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = strings.signInWithGoogle,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        authError?.let { err ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = StatusErrorSoft,
                                border = BorderStroke(1.dp, StatusError.copy(alpha = 0.45f))
                            ) {
                                Text(
                                    text = err,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StatusError,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = GoldBright,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = strings.singleAccountRuleShort,
                                style = MaterialTheme.typography.labelSmall,
                                color = GoldSoft
                            )
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(strings.howItWorksTitle, style = MaterialTheme.typography.titleLarge)
                    listOf(
                        Triple(Icons.Default.PersonAdd, strings.howStep1Title, strings.howStep1Desc),
                        Triple(Icons.Default.TaskAlt, strings.howStep2Title, strings.howStep2Desc),
                        Triple(Icons.Default.FactCheck, strings.howStep3Title, strings.howStep3Desc),
                        Triple(Icons.Default.AccountBalanceWallet, strings.howStep4Title, strings.howStep4Desc)
                    ).forEach { (icon, title, desc) ->
                        PublicStepCard(icon = icon, title = title, desc = desc)
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(strings.rulesSectionTitle, style = MaterialTheme.typography.titleLarge)
                    listOf(
                        Triple(Icons.Default.PhonelinkLock, strings.rule1Title, strings.rule1Desc),
                        Triple(Icons.Default.VpnLock, strings.rule2Title, strings.rule2Desc),
                        Triple(Icons.Default.Campaign, strings.rule3Title, strings.rule3Desc),
                        Triple(Icons.Default.Verified, strings.rule4Title, strings.rule4Desc)
                    ).forEach { (icon, title, desc) ->
                        PublicStepCard(icon = icon, title = title, desc = desc, accent = GoldAccent)
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(strings.platformFeaturesTitle, style = MaterialTheme.typography.titleLarge)
                    listOf(
                        Triple(Icons.Default.Payments, strings.feature1Title, strings.feature1Desc),
                        Triple(Icons.Default.PhoneAndroid, strings.feature2Title, strings.feature2Desc),
                        Triple(Icons.Default.Translate, strings.feature3Title, strings.feature3Desc),
                        Triple(Icons.Default.Shield, strings.feature4Title, strings.feature4Desc)
                    ).forEach { (icon, title, desc) ->
                        PublicStepCard(icon = icon, title = title, desc = desc, accent = NavyPrimary)
                    }
                }
            }

            item {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(strings.whyChooseUsTitle, style = MaterialTheme.typography.titleMedium)
                        listOf(strings.why1, strings.why2, strings.why3, strings.why4).forEach { line ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                                Text(line, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            if (faqs.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(strings.faqTitle, style = MaterialTheme.typography.titleLarge)
                        faqs.forEachIndexed { idx, faq ->
                            var expanded by remember { mutableStateOf(idx == 0) }
                            OutlinedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expanded = !expanded },
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = faq.question.get(language),
                                            style = MaterialTheme.typography.titleSmall,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null
                                        )
                                    }
                                    AnimatedVisibility(visible = expanded) {
                                        Text(
                                            text = faq.answer.get(language),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = NavyDeep
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(strings.contactTitle, style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text(strings.contactSubtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                        Text(strings.supportDirectAdminNotice, style = MaterialTheme.typography.labelMedium, color = GoldSoft)
                        HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TextButton(onClick = { onOpenLegal("about") }) {
                                Text(strings.aboutUs, color = EmeraldBright)
                            }
                            TextButton(onClick = { onOpenLegal("terms") }) {
                                Text(strings.termsConditions, color = EmeraldBright)
                            }
                            TextButton(onClick = { onOpenLegal("privacy") }) {
                                Text(strings.privacyPolicy, color = EmeraldBright)
                            }
                            TextButton(
                                onClick = {
                                    try {
                                        AppNotificationHelper.shareAppDownload(
                                            context = context,
                                            url = null,
                                            brandName = strings.brandName,
                                            latestVersion = BuildConfig.VERSION_NAME
                                        )
                                    } catch (e: Exception) {
                                        Log.w(TAG, "shareAppDownload failed", e)
                                    }
                                }
                            ) {
                                Text(strings.shareAppButton, color = GoldSoft)
                            }
                        }
                        Text(strings.footerRights, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.65f))
                    }
                }
            }
        }
    }
}

@Composable
private fun PublicStepCard(
    icon: ImageVector,
    title: String,
    desc: String,
    accent: Color = EmeraldPrimary
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
