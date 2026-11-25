package com.example.features.tasks

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.security.AdminSecurity
import com.example.domain.model.*
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ProofStatusBadge
import com.example.ui.components.RealRewardTopBar
import com.example.ui.i18n.LocalStrings
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun TaskDetailsScreen(
    task: MicroTaskDoc?,
    user: UserAccountDoc,
    existingProof: TaskProofDoc?,
    language: AppLanguage,
    isSubmitting: Boolean,
    onToggleLanguage: () -> Unit,
    onBack: () -> Unit,
    onSubmitProof: (MicroTaskDoc, UserAccountDoc, String, String?, (Result<Unit>) -> Unit) -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var proofNote by remember { mutableStateOf("") }
    var attachedLabel by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            attachedLabel = uri.lastPathSegment ?: "screenshot_proof.jpg"
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            RealRewardTopBar(
                title = strings.taskDetailsTitle,
                language = language,
                onToggleLanguage = onToggleLanguage,
                onBack = onBack
            )
        }
    ) { innerPadding ->
        if (task == null) {
            EmptyStateView(modifier = Modifier.padding(innerPadding))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(id = task.categoryEnum().imageRes()),
                contentDescription = task.title(language),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
                    .clip(RoundedCornerShape(18.dp)),
                contentScale = ContentScale.Crop
            )

            val vipTier = user.vipTierEnum()
            val effectiveReward = AdminSecurity.calculateTaskRewardWithVip(task.rewardBdt, vipTier.name)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProofStatusBadge(status = existingProof?.statusEnum(), language = language)
                    if (task.instantCredit) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = GoldSoft,
                            border = BorderStroke(1.dp, GoldBright.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = GoldDarkContainer, modifier = Modifier.size(14.dp))
                                Text(
                                    text = strings.instantCreditBadge,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = GoldDarkContainer
                                )
                            }
                        }
                    }
                }
                Surface(shape = RoundedCornerShape(50), color = EmeraldPrimary) {
                    Text(
                        text = "${strings.rewardLabel}: ${AdminSecurity.formatBdt(effectiveReward)}",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            if (vipTier.bonusPercent > 0) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GoldSoft,
                    border = BorderStroke(1.dp, GoldBright.copy(alpha = 0.55f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${strings.vipBonusBadgePrefix} (${vipTier.title.get(language)} +${vipTier.bonusPercent}%)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = GoldDarkContainer
                        )
                        Text(
                            text = "${strings.vipBaseRateNote} ${AdminSecurity.formatBdt(task.rewardBdt)} → ${AdminSecurity.formatBdt(effectiveReward)}",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = NavyDeep
                        )
                    }
                }
            }

            Text(task.title(language), style = MaterialTheme.typography.headlineSmall)
            Text(task.description(language), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.taskInstructionsTitle, style = MaterialTheme.typography.titleMedium)
                    val steps = task.instructionSteps(language)
                    steps.forEachIndexed { idx, step ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("${idx + 1}.", style = MaterialTheme.typography.titleSmall, color = EmeraldPrimary)
                            Text(step, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            if (task.taskLink.isNotBlank()) {
                OutlinedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(strings.taskLinkTitle, style = MaterialTheme.typography.titleSmall)
                        Text(task.taskLink, style = MaterialTheme.typography.bodySmall, color = StatusInfo)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(task.taskLink))
                                    scope.launch { snackbarHostState.showSnackbar(strings.linkCopied) }
                                },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.copyLink)
                            }
                            Button(
                                onClick = {
                                    runCatching {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(task.taskLink))
                                        context.startActivity(intent)
                                    }
                                },
                                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NavyDeep)
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.openExternalLink)
                            }
                        }
                    }
                }
            }

            if (existingProof != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = GoldSoft,
                    border = BorderStroke(1.dp, GoldBright.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(strings.proofAlreadySubmitted, style = MaterialTheme.typography.titleSmall, color = GoldDarkContainer)
                        Text(existingProof.proofNote, style = MaterialTheme.typography.bodySmall, color = GoldDarkContainer)
                        existingProof.adminNote?.takeIf { it.isNotBlank() }?.let { note ->
                            Text("${strings.adminReplyLabel}: $note", style = MaterialTheme.typography.labelMedium, color = NavyDeep)
                        }
                    }
                }
            } else {
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(strings.submitProofTitle, style = MaterialTheme.typography.titleMedium)
                        if (task.instantCredit && task.verificationCode.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = GoldSoft,
                                border = BorderStroke(1.dp, GoldBright.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = GoldDarkContainer, modifier = Modifier.size(18.dp))
                                    Text(
                                        text = strings.taskVerificationCodeLabel,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = GoldDarkContainer
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = proofNote,
                            onValueChange = { proofNote = it },
                            placeholder = {
                                Text(
                                    if (task.instantCredit && task.verificationCode.isNotBlank()) {
                                        strings.taskVerificationCodeLabel
                                    } else {
                                        strings.proofInputPlaceholder
                                    }
                                )
                            },
                            minLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("task_proof_note_input")
                        )

                        OutlinedButton(
                            onClick = {
                                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                attachedLabel?.let { "${strings.screenshotSelected} $it" }
                                    ?: strings.attachScreenshot
                            )
                        }

                        Button(
                            onClick = {
                                if (task.availableSlots <= 0L) {
                                    scope.launch { snackbarHostState.showSnackbar(strings.errNoSlots) }
                                    return@Button
                                }
                                if (proofNote.isBlank() && attachedLabel == null) {
                                    scope.launch { snackbarHostState.showSnackbar(strings.errProofEmpty) }
                                    return@Button
                                }
                                onSubmitProof(task, user, proofNote, attachedLabel) { res ->
                                    scope.launch {
                                        res.fold(
                                            onSuccess = {
                                                val msg = if (task.instantCredit) {
                                                    strings.proofInstantCreditSuccess
                                                } else {
                                                    strings.proofSubmittedSuccess
                                                }
                                                snackbarHostState.showSnackbar(msg)
                                            },
                                            onFailure = { err ->
                                                val msg = when (err.message) {
                                                    "INVALID_TASK_VERIFICATION" -> strings.errInvalidTaskVerification
                                                    else -> err.localizedMessage ?: strings.errorTitle
                                                }
                                                snackbarHostState.showSnackbar(msg)
                                            }
                                        )
                                    }
                                }
                            },
                            enabled = !isSubmitting && task.availableSlots > 0,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 50.dp)
                                .testTag("submit_proof_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White)
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.CloudUpload, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(strings.submitProofButton)
                            }
                        }
                    }
                }
            }
        }
    }
}
