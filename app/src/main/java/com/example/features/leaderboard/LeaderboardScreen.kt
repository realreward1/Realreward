package com.example.features.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.security.AdminSecurity
import com.example.domain.model.*
import com.example.ui.components.*
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.*

@Composable
fun LeaderboardScreen(
    language: AppLanguage,
    leaderboardState: UiState<List<LeaderboardPublicDoc>>,
    onToggleLanguage: () -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit
) {
    val strings = LocalStrings.current

    Scaffold(
        topBar = {
            RealRewardTopBar(
                title = strings.leaderboardTitle,
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = strings.leaderboardSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            when (leaderboardState) {
                is UiState.Loading -> LoadingStateView()
                is UiState.Error -> ErrorStateView(message = leaderboardState.message, onRetry = onRetry)
                is UiState.Success -> {
                    if (leaderboardState.data.isEmpty()) {
                        EmptyStateView()
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            itemsIndexed(leaderboardState.data, key = { _, item -> item.userId }) { index, entry ->
                                val rank = index + 1
                                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        when (rank) {
                                                            1 -> GoldBright
                                                            2 -> NavyMuted
                                                            3 -> GoldAccent
                                                            else -> EmeraldSoft
                                                        }
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (rank <= 3) {
                                                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                                } else {
                                                    Text("#$rank", style = MaterialTheme.typography.labelLarge, color = EmeraldDarkContainer)
                                                }
                                            }
                                            Column {
                                                Text(entry.displayName, style = MaterialTheme.typography.titleSmall)
                                                Text(
                                                    text = "@${entry.username} • ${VipTierCode.fromRaw(entry.vipTier).title.get(language)} • ${entry.tasksCompleted} ${strings.tasksDoneShort}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = "${strings.leaderboardLifetimePrefix}: ${AdminSecurity.formatBdt(entry.earningsBdt)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = GoldAccent
                                                )
                                            }
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = AdminSecurity.formatBdt(entry.availableBalanceBdt ?: entry.earningsBdt),
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = EmeraldPrimary
                                            )
                                            Text(
                                                text = strings.leaderboardWalletLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
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
