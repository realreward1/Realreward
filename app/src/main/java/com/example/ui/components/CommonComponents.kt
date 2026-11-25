package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.security.AdminSecurity
import com.example.domain.model.*
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.*
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

fun formatTimestamp(timestamp: Timestamp?, language: AppLanguage): String {
    val ts = timestamp ?: return if (language == AppLanguage.BANGLA) "এইমাত্র" else "Just now"
    val locale = if (language == AppLanguage.BANGLA) Locale("bn", "BD") else Locale.US
    return SimpleDateFormat("dd MMM, hh:mm a", locale).format(ts.toDate())
}

@Composable
fun BrandLogoMark(
    modifier: Modifier = Modifier,
    lightText: Boolean = false,
    showTagline: Boolean = true
) {
    val strings = LocalStrings.current
    val logoGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF00E5A3), Color(0xFFFBBF24), Color(0xFF38BDF8))
    )
    val wordmark = buildAnnotatedString {
        withStyle(
            SpanStyle(
                color = if (lightText) Color(0xFF34D399) else Color(0xFF047857),
                fontWeight = FontWeight.ExtraBold
            )
        ) {
            append("REAL ")
        }
        withStyle(
            SpanStyle(
                color = if (lightText) Color(0xFFFBBF24) else Color(0xFFD97706),
                fontWeight = FontWeight.ExtraBold
            )
        ) {
            append("REWARD")
        }
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(logoGradient)
                .padding(2.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_brand_emblem_1795531052528),
                contentDescription = strings.brandName,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(11.dp)),
                contentScale = ContentScale.Crop
            )
        }
        Column {
            Text(
                text = wordmark,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = PlusJakartaSansFontFamily,
                    letterSpacing = 1.0.sp
                )
            )
            if (showTagline) {
                Text(
                    text = strings.tagline,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp
                    ),
                    color = if (lightText) Color(0xFF7DD3FC) else NavyPrimary
                )
            }
        }
    }
}

@Composable
fun LanguageSwitchChip(
    currentLanguage: AppLanguage,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    darkSurface: Boolean = false
) {
    val label = if (currentLanguage == AppLanguage.BANGLA) "বাংলা | EN" else "EN | বাংলা"
    Surface(
        onClick = onToggle,
        modifier = modifier
            .heightIn(min = 48.dp)
            .testTag("language_switch_button")
            .semantics { contentDescription = "Switch language Bangla or English" },
        shape = RoundedCornerShape(24.dp),
        color = if (darkSurface) NavySurface else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (darkSurface) GoldBright.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Translate,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (darkSurface) GoldBright else EmeraldPrimary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = if (darkSurface) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun LivePlatformPill(
    modifier: Modifier = Modifier,
    text: String? = null
) {
    val strings = LocalStrings.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = EmeraldSoft,
        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(EmeraldPrimary)
            )
            Text(
                text = text ?: strings.liveBadge,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = EmeraldDarkContainer
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealRewardTopBar(
    title: String,
    language: AppLanguage,
    onToggleLanguage: () -> Unit,
    onBack: (() -> Unit)? = null,
    noticeCount: Int = 0,
    onOpenNotifications: (() -> Unit)? = null
) {
    val strings = LocalStrings.current
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("top_bar_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.back
                    )
                }
            }
        },
        actions = {
            if (onOpenNotifications != null) {
                IconButton(
                    onClick = onOpenNotifications,
                    modifier = Modifier.testTag("top_bar_notifications_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (noticeCount > 0) {
                                Badge(containerColor = EmeraldPrimary) {
                                    Text(noticeCount.coerceAtMost(99).toString(), color = Color.White)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = strings.notifications
                        )
                    }
                }
            }
            LanguageSwitchChip(
                currentLanguage = language,
                onToggle = onToggleLanguage,
                modifier = Modifier.padding(end = 8.dp)
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground
        )
    )
}

@Composable
fun FintechStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                LivePlatformPill()
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = PlusJakartaSansFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor
                )
            }
        }
    }
}

@Composable
fun TaskListCard(
    task: MicroTaskDoc,
    userProofStatus: ProofStatus?,
    language: AppLanguage,
    onViewTask: () -> Unit,
    modifier: Modifier = Modifier,
    userVipTier: VipTierCode = VipTierCode.REGULAR
) {
    val strings = LocalStrings.current
    val slotsFraction = if (task.totalSlots > 0) {
        (task.availableSlots.toFloat() / task.totalSlots.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val categoryEnum = task.categoryEnum()
    val effectiveReward = AdminSecurity.calculateTaskRewardWithVip(task.rewardBdt, userVipTier.name)
    val bonusPct = userVipTier.bonusPercent

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onViewTask)
            .testTag("task_card_${task.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(144.dp)
            ) {
                Image(
                    painter = painterResource(id = categoryEnum.imageRes()),
                    contentDescription = task.title(language),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, NavyDeep.copy(alpha = 0.78f))
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = NavyDeep.copy(alpha = 0.88f),
                        border = BorderStroke(1.dp, EmeraldBright.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = categoryEnum.title.get(language),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = EmeraldPrimary
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = AdminSecurity.formatBdt(effectiveReward),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = PlusJakartaSansFontFamily,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = Color.White
                            )
                            if (bonusPct > 0) {
                                Text(
                                    text = "+${bonusPct}% VIP",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = GoldBright
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProofStatusBadge(
                        status = userProofStatus,
                        language = language
                    )
                    if (task.instantCredit) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = GoldSoft
                        ) {
                            Text(
                                text = strings.instantCreditBadge,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = GoldDarkContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = task.title(language),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = task.description(language),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = GoldAccent
                        )
                        Text(
                            text = "${strings.deadlineLabel}: ${task.deadlineText.ifBlank { "-" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${task.availableSlots}/${task.totalSlots} ${strings.slotsLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldPrimary
                    )
                }

                LinearProgressIndicator(
                    progress = { slotsFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = EmeraldPrimary,
                    trackColor = EmeraldSoft
                )

                Spacer(modifier = Modifier.height(2.dp))

                Button(
                    onClick = onViewTask,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("view_task_btn_${task.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NavyDeep,
                        contentColor = Color.White
                    )
                ) {
                    Text(strings.viewTask, style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProofStatusBadge(
    status: ProofStatus?,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val (bg, fg, label) = when (status) {
        null -> Triple(EmeraldSoft, EmeraldDarkContainer, strings.taskAvailableLabel)
        ProofStatus.PENDING_REVIEW -> Triple(GoldSoft, GoldDarkContainer, status.label.get(language))
        ProofStatus.APPROVED -> Triple(EmeraldPrimary, Color.White, status.label.get(language))
        ProofStatus.REJECTED -> Triple(StatusErrorSoft, StatusError, status.label.get(language))
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = bg
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = fg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (actionText != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelLarge,
                    color = EmeraldPrimary
                )
            }
        }
    }
}

@Composable
fun LoadingStateView(
    message: String? = null,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator(color = EmeraldPrimary)
        Text(
            text = message ?: strings.loadingLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun EmptyStateView(
    title: String? = null,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(EmeraldSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = null,
                tint = EmeraldPrimary,
                modifier = Modifier.size(32.dp)
            )
        }
        Text(
            text = title ?: strings.emptyTitle,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Text(
            text = subtitle ?: strings.emptySubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            OutlinedButton(onClick = onAction, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun ErrorStateView(
    message: String? = null,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(StatusErrorSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = null,
                tint = StatusError,
                modifier = Modifier.size(32.dp)
            )
        }
        Text(
            text = strings.errorTitle,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Text(
            text = message ?: strings.errorSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = onRetry,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag("error_retry_button"),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(strings.retry)
        }
    }
}

@Composable
fun BannedAccountScreen(
    banReason: String? = null,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    val reasonLabel = when {
        banReason == "VPN_DETECTED" -> strings.banReasonVpn
        banReason?.startsWith("FOREIGN_IP") == true -> "${strings.banReasonForeignIp} (${banReason.removePrefix("FOREIGN_IP_")})"
        !banReason.isNullOrBlank() -> strings.banReasonAdmin
        else -> null
    }
    Surface(
        modifier = modifier.fillMaxSize(),
        color = NavyDeep
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(StatusErrorSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = strings.errAccountBannedTitle,
                    tint = StatusError,
                    modifier = Modifier.size(44.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = strings.errAccountBannedTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = strings.errAccountBannedBody,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            if (reasonLabel != null) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = StatusError.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, StatusError.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = reasonLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = StatusErrorSoft,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onSignOut,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("banned_signout_button"),
                colors = ButtonDefaults.buttonColors(containerColor = StatusError)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(strings.logout)
            }
        }
    }
}

@Composable
fun DeviceAlreadyBoundScreen(
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    Surface(
        modifier = modifier.fillMaxSize(),
        color = NavyDeep
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(GoldSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhonelinkLock,
                    contentDescription = strings.deviceLockedTitle,
                    tint = GoldDarkContainer,
                    modifier = Modifier.size(42.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = strings.deviceLockedTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = strings.deviceLockedBody,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.82f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onSignOut,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("device_locked_signout_button"),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White)
            ) {
                Icon(Icons.Default.SwapHoriz, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(strings.switchGoogleAccount)
            }
        }
    }
}

@Composable
fun AccountRemovedScreen(
    reason: String? = null,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    Surface(
        modifier = modifier.fillMaxSize(),
        color = NavyDeep
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(StatusErrorSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = strings.accountRemovedTitle,
                    tint = StatusError,
                    modifier = Modifier.size(46.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = strings.accountRemovedTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = strings.accountRemovedBody,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.82f),
                textAlign = TextAlign.Center
            )
            if (!reason.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = StatusError.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, StatusError.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.labelMedium,
                        color = StatusErrorSoft,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onSignOut,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("removed_signout_button"),
                colors = ButtonDefaults.buttonColors(containerColor = StatusError)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(strings.logout)
            }
        }
    }
}

private fun toBanglaDigits(value: Long): String {
    val map = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    return value.toString().map { ch ->
        if (ch in '0'..'9') map[ch - '0'] else ch
    }.joinToString("")
}

fun formatRemainingCountdown(endEpochMillis: Long, nowMillis: Long): String {
    if (endEpochMillis <= 0L) return "--:--:--"
    val diffSec = ((endEpochMillis - nowMillis) / 1000L).coerceAtLeast(0L)
    val hrs = diffSec / 3600L
    val mins = (diffSec % 3600L) / 60L
    val secs = diffSec % 60L
    return "%02d:%02d:%02d".format(hrs, mins, secs)
}

fun formatMaintenanceWaitSentence(
    endEpochMillis: Long,
    nowMillis: Long,
    language: AppLanguage
): String {
    if (endEpochMillis <= 0L) {
        return if (language == AppLanguage.BANGLA) {
            "অ্যাপসের আপডেটের কাজ চলমান। আপডেট শেষ না হওয়া পর্যন্ত অনুগ্রহ করে অপেক্ষা করুন।"
        } else {
            "App update is in progress. Please wait until the update completes."
        }
    }
    val diffSec = ((endEpochMillis - nowMillis) / 1000L).coerceAtLeast(0L)
    val hrs = diffSec / 3600L
    val mins = (diffSec % 3600L) / 60L
    val secs = diffSec % 60L

    return if (language == AppLanguage.BANGLA) {
        val durationPart = when {
            hrs > 0L -> buildString {
                append("${toBanglaDigits(hrs)} ঘণ্টা")
                if (mins > 0L) append(" ${toBanglaDigits(mins)} মিনিট")
                if (secs > 0L) append(" ${toBanglaDigits(secs)} সেকেন্ড")
            }
            mins > 0L -> buildString {
                append("${toBanglaDigits(mins)} মিনিট")
                if (secs > 0L) append(" ${toBanglaDigits(secs)} সেকেন্ড")
            }
            else -> "${toBanglaDigits(secs)} সেকেন্ড"
        }
        "দয়া করে $durationPart অপেক্ষা করুন"
    } else {
        val durationPart = when {
            hrs > 0L -> buildString {
                append("$hrs hr")
                if (mins > 0L) append(" $mins min")
                if (secs > 0L) append(" $secs sec")
            }
            mins > 0L -> buildString {
                append("$mins min")
                if (secs > 0L) append(" $secs sec")
            }
            else -> "$secs sec"
        }
        "Please wait $durationPart"
    }
}

@Composable
fun AppMaintenanceFloatingOverlay(
    appConfig: AppConfigDoc,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val strings = LocalStrings.current
    var nowMillis by remember(appConfig.stopEndEpochMillis, appConfig.appStopped) {
        mutableLongStateOf(System.currentTimeMillis())
    }
    LaunchedEffect(appConfig.stopEndEpochMillis, appConfig.appStopped) {
        nowMillis = System.currentTimeMillis()
        while (true) {
            kotlinx.coroutines.delay(1000L)
            nowMillis = System.currentTimeMillis()
        }
    }

    val customMessage = if (language == AppLanguage.BANGLA) {
        appConfig.stopMessageBn?.takeIf { it.isNotBlank() }
            ?: appConfig.stopMessageEn?.takeIf { it.isNotBlank() }
    } else {
        appConfig.stopMessageEn?.takeIf { it.isNotBlank() }
            ?: appConfig.stopMessageBn?.takeIf { it.isNotBlank() }
    }

    BackHandler(enabled = true) {
        // App is paused for maintenance; back navigation is disabled.
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NavyDeep.copy(alpha = 0.78f))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    }
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .testTag("maintenance_floating_banner"),
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 14.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = NavySurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, GoldBright.copy(alpha = 0.85f), RoundedCornerShape(28.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(colors = listOf(NavyDeep, EmeraldDarkContainer)))
                        .padding(horizontal = 24.dp, vertical = 22.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(74.dp)
                                .clip(CircleShape)
                                .background(GoldSoft)
                                .border(2.dp, GoldBright, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SystemUpdate,
                                contentDescription = strings.maintenanceTitle,
                                tint = GoldDarkContainer,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                        Text(
                            text = strings.maintenanceTitle,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, EmeraldBright.copy(alpha = 0.45f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = customMessage ?: strings.maintenanceDefaultMsg,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    if (appConfig.stopEndEpochMillis > 0L) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = NavyDeep,
                            border = BorderStroke(1.5.dp, GoldBright.copy(alpha = 0.8f))
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = formatMaintenanceWaitSentence(
                                        endEpochMillis = appConfig.stopEndEpochMillis,
                                        nowMillis = nowMillis,
                                        language = language
                                    ),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = GoldBright,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = formatRemainingCountdown(appConfig.stopEndEpochMillis, nowMillis),
                                    style = MaterialTheme.typography.displaySmall.copy(
                                        fontFamily = PlusJakartaSansFontFamily,
                                        fontWeight = FontWeight.ExtraBold
                                    ),
                                    color = EmeraldBright
                                )
                            }
                        }
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = NavyDeep,
                            border = BorderStroke(1.dp, GoldBright.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = formatMaintenanceWaitSentence(
                                    endEpochMillis = 0L,
                                    nowMillis = nowMillis,
                                    language = language
                                ),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = GoldBright,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminConfirmDialog(
    title: String,
    message: String,
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (isDestructive) Icons.Default.WarningAmber else Icons.Default.VerifiedUser,
                contentDescription = null,
                tint = if (isDestructive) StatusError else EmeraldPrimary
            )
        },
        title = { Text(title) },
        text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDestructive) StatusError else EmeraldPrimary,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("admin_confirm_ok_button")
            ) {
                Text(strings.okConfirm)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("admin_confirm_cancel_button")
            ) {
                Text(strings.cancel)
            }
        }
    )
}

