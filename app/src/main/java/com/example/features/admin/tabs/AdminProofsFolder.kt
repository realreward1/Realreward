package com.example.features.admin.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.core.security.AdminSecurity
import com.example.domain.model.AppLanguage
import com.example.domain.model.ProofStatus
import com.example.domain.model.TaskProofDoc
import com.example.domain.model.UiState
import com.example.ui.components.*
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusInfo

private data class ProofConfirmAction(
    val title: String,
    val message: String,
    val destructive: Boolean,
    val execute: () -> Unit
)

@Composable
fun AdminProofsFolder(
    proofsState: UiState<List<TaskProofDoc>>,
    language: AppLanguage,
    isBusy: Boolean,
    onRetry: () -> Unit,
    onReviewProof: (TaskProofDoc, Boolean, String) -> Unit
) {
    val strings = LocalStrings.current
    var pendingConfirm by remember { mutableStateOf<ProofConfirmAction?>(null) }

    when (proofsState) {
        is UiState.Loading -> LoadingStateView()
        is UiState.Error -> ErrorStateView(message = proofsState.message, onRetry = onRetry)
        is UiState.Success -> {
            if (proofsState.data.isEmpty()) {
                EmptyStateView()
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(proofsState.data, key = { it.id }) { proof ->
                        ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${proof.taskTitle} (@${proof.username})", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                    ProofStatusBadge(status = proof.statusEnum(), language = language)
                                }
                                Text("Reward: ${AdminSecurity.formatBdt(proof.rewardBdt)}", style = MaterialTheme.typography.labelMedium, color = EmeraldPrimary)
                                Text(proof.proofNote, style = MaterialTheme.typography.bodySmall)
                                proof.attachmentLabel?.let {
                                    Text("Attachment: $it", style = MaterialTheme.typography.labelSmall, color = StatusInfo)
                                }
                                if (proof.statusEnum() == ProofStatus.PENDING_REVIEW) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                pendingConfirm = ProofConfirmAction(
                                                    title = strings.adminConfirmTitle,
                                                    message = "Approve proof for @${proof.username} & credit ${AdminSecurity.formatBdt(proof.rewardBdt)}?",
                                                    destructive = false
                                                ) { onReviewProof(proof, true, "Approved by owner") }
                                            },
                                            enabled = !isBusy,
                                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                                            modifier = Modifier.weight(1f)
                                        ) { Text(strings.adminApprove) }
                                        OutlinedButton(
                                            onClick = {
                                                pendingConfirm = ProofConfirmAction(
                                                    title = strings.adminConfirmTitle,
                                                    message = "Reject proof submitted by @${proof.username}?",
                                                    destructive = true
                                                ) { onReviewProof(proof, false, "Invalid proof") }
                                            },
                                            enabled = !isBusy,
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusError),
                                            modifier = Modifier.weight(1f)
                                        ) { Text(strings.adminReject) }
                                    }
                                }
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
