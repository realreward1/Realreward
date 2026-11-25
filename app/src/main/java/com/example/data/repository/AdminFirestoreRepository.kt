package com.example.data.repository

import android.content.Context
import com.example.core.firebase.FirestoreProvider
import com.example.core.firebase.OperationType
import com.example.core.firebase.handleFirestoreError
import com.example.core.security.AdminSecurity
import com.example.domain.model.*
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlin.math.roundToLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Owner-only Admin Repository. All Firestore mutations are additionally enforced server-side
 * by isOwnerAdmin() in firestore.rules.
 */
class AdminFirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(FirestoreProvider.getDatabase(context))

    private fun roundBdt(value: Double): Double = (value * 100.0).roundToLong() / 100.0

    private fun requireOwnerUid(): String {
        val user = auth.currentUser
        check(AdminSecurity.isVerifiedOwner(user)) {
            "Owner-only access required."
        }
        return user!!.uid
    }

    fun observeAllUsers(): Flow<List<UserAccountDoc>> = flow {
        val uid = requireOwnerUid()
        emitAll(
            db.collection("users")
                .limit(250)
                .snapshots(MetadataChanges.INCLUDE)
                .map { snap ->
                    snap.toObjects(UserAccountDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.availableBalanceBdt }
                }
                .catch { e ->
                    logIfActive(e, OperationType.LIST, "users", uid)
                    throw e
                }
        )
    }

    fun observeFraudAlerts(): Flow<List<FraudAlertDoc>> = flow {
        val uid = requireOwnerUid()
        emitAll(
            db.collection("fraud_alerts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100)
                .snapshots(MetadataChanges.INCLUDE)
                .map { snap ->
                    snap.toObjects(FraudAlertDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { e ->
                    logIfActive(e, OperationType.LIST, "fraud_alerts", uid)
                    throw e
                }
        )
    }

    fun observeAllReferrals(): Flow<List<ReferralRecordDoc>> = flow {
        val uid = requireOwnerUid()
        emitAll(
            db.collection("referrals")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(200)
                .snapshots(MetadataChanges.INCLUDE)
                .map { snap ->
                    snap.toObjects(ReferralRecordDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { e ->
                    logIfActive(e, OperationType.LIST, "referrals", uid)
                    throw e
                }
        )
    }

    fun observeAllTasks(): Flow<List<MicroTaskDoc>> = flow {
        val uid = requireOwnerUid()
        emitAll(
            db.collection("tasks")
                .limit(200)
                .snapshots(MetadataChanges.INCLUDE)
                .map { snap ->
                    snap.toObjects(MicroTaskDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { e ->
                    logIfActive(e, OperationType.LIST, "tasks", uid)
                    throw e
                }
        )
    }

    fun observeAllProofs(): Flow<List<TaskProofDoc>> = flow {
        val uid = requireOwnerUid()
        emitAll(
            db.collection("task_proofs")
                .limit(200)
                .snapshots(MetadataChanges.INCLUDE)
                .map { snap ->
                    snap.toObjects(TaskProofDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { e ->
                    logIfActive(e, OperationType.LIST, "task_proofs", uid)
                    throw e
                }
        )
    }

    fun observeAllWithdrawals(): Flow<List<WithdrawalDoc>> = flow {
        val uid = requireOwnerUid()
        emitAll(
            db.collection("withdrawals")
                .limit(200)
                .snapshots(MetadataChanges.INCLUDE)
                .map { snap ->
                    snap.toObjects(WithdrawalDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { e ->
                    logIfActive(e, OperationType.LIST, "withdrawals", uid)
                    throw e
                }
        )
    }

    fun observeAllVipRequests(): Flow<List<VipRequestDoc>> = flow {
        val uid = requireOwnerUid()
        emitAll(
            db.collection("vip_requests")
                .limit(200)
                .snapshots(MetadataChanges.INCLUDE)
                .map { snap ->
                    snap.toObjects(VipRequestDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { e ->
                    logIfActive(e, OperationType.LIST, "vip_requests", uid)
                    throw e
                }
        )
    }

    fun observeAllSupportTickets(): Flow<List<SupportTicketDoc>> = flow {
        val uid = requireOwnerUid()
        emitAll(
            db.collection("support_tickets")
                .limit(200)
                .snapshots(MetadataChanges.INCLUDE)
                .map { snap ->
                    snap.toObjects(SupportTicketDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { e ->
                    logIfActive(e, OperationType.LIST, "support_tickets", uid)
                    throw e
                }
        )
    }

    fun observeAnnouncements(): Flow<List<AnnouncementDoc>> = flow {
        val uid = requireOwnerUid()
        emitAll(
            db.collection("announcements")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .snapshots(MetadataChanges.INCLUDE)
                .map { snap ->
                    snap.toObjects(AnnouncementDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { e ->
                    logIfActive(e, OperationType.LIST, "announcements", uid)
                    throw e
                }
        )
    }

    fun observeAppConfig(): Flow<AppConfigDoc> = flow {
        val uid = requireOwnerUid()
        emitAll(
            db.collection("app_config").document("payment_settings")
                .snapshots(MetadataChanges.INCLUDE)
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(AppConfigDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            ?: AppConfigDoc()
                    } else AppConfigDoc()
                }
                .catch { e ->
                    logIfActive(e, OperationType.GET, "app_config/payment_settings", uid)
                    throw e
                }
        )
    }

    // --- USER MANAGEMENT (Permanent Ban, Account Removal, History Inspection, VIP, Balance) ---

    suspend fun toggleUserBan(
        userId: String,
        ban: Boolean,
        permanent: Boolean = true,
        reason: String? = null
    ): Result<Unit> {
        return try {
            requireOwnerUid()
            val updates = mutableMapOf<String, Any?>(
                "banned" to ban,
                "permanentlyBanned" to (ban && permanent),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (ban) {
                updates["banReason"] = (reason ?: "PERMANENT_ADMIN_BAN").take(160)
            } else {
                updates["banReason"] = null
                updates["vpnDetected"] = false
            }
            db.collection("users").document(userId).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$userId")
            Result.failure(e)
        }
    }

    suspend fun allowUserWalletChangeOnce(userId: String): Result<Unit> {
        return try {
            requireOwnerUid()
            db.collection("users").document(userId).update(
                mapOf(
                    "paymentAccountsLocked" to false,
                    "walletChangeApprovedOnce" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$userId")
            Result.failure(e)
        }
    }

    suspend fun permanentlyRemoveUserAccount(user: UserAccountDoc, reason: String): Result<Unit> {
        return try {
            requireOwnerUid()
            val cleanReason = reason.trim().ifBlank { "PERMANENT_ADMIN_REMOVAL" }.take(300)
            val batch = db.batch()
            val removedPayload = mutableMapOf<String, Any>(
                "userId" to user.userId,
                "reason" to cleanReason,
                "removedAt" to FieldValue.serverTimestamp()
            )
            if (user.email.isNotBlank()) {
                removedPayload["email"] = user.email.take(200)
                val emailKey = AdminSecurity.sanitizeEmailKey(user.email)
                if (emailKey.isNotBlank()) {
                    batch.set(db.collection("removed_emails").document(emailKey), removedPayload)
                }
            }
            batch.set(db.collection("removed_accounts").document(user.userId), removedPayload)
            batch.delete(db.collection("users").document(user.userId))
            batch.delete(db.collection("leaderboard_public").document(user.userId))
            if (user.referralCode.isNotBlank()) {
                batch.delete(db.collection("referral_codes").document(user.referralCode))
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "users/${user.userId}")
            Result.failure(e)
        }
    }

    suspend fun loadUserFullHistory(user: UserAccountDoc): Result<UserFullHistory> {
        return try {
            requireOwnerUid()
            val uid = user.userId
            val txns = db.collection("transactions").whereEqualTo("userId", uid).limit(60).get().await()
                .toObjects(TransactionDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedByDescending { it.createdAt?.seconds ?: 0L }
            val withdrawals = db.collection("withdrawals").whereEqualTo("userId", uid).limit(40).get().await()
                .toObjects(WithdrawalDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedByDescending { it.createdAt?.seconds ?: 0L }
            val proofs = db.collection("task_proofs").whereEqualTo("userId", uid).limit(40).get().await()
                .toObjects(TaskProofDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedByDescending { it.createdAt?.seconds ?: 0L }
            val referrals = db.collection("referrals").whereEqualTo("referrerId", uid).limit(40).get().await()
                .toObjects(ReferralRecordDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedByDescending { it.createdAt?.seconds ?: 0L }

            Result.success(
                UserFullHistory(
                    user = user,
                    transactions = txns,
                    withdrawals = withdrawals,
                    proofs = proofs,
                    referrals = referrals
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "users/${user.userId}/history")
            Result.failure(e)
        }
    }

    suspend fun resolveFraudAlert(alertId: String): Result<Unit> {
        return try {
            requireOwnerUid()
            db.collection("fraud_alerts").document(alertId).update(
                mapOf("resolved" to true)
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "fraud_alerts/$alertId")
            Result.failure(e)
        }
    }

    suspend fun setUserVipTier(userId: String, tier: VipTierCode): Result<Unit> {
        return try {
            requireOwnerUid()
            val batch = db.batch()
            batch.update(
                db.collection("users").document(userId),
                mapOf("vipTier" to tier.name, "updatedAt" to FieldValue.serverTimestamp())
            )
            batch.update(
                db.collection("leaderboard_public").document(userId),
                mapOf("vipTier" to tier.name, "updatedAt" to FieldValue.serverTimestamp())
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$userId")
            Result.failure(e)
        }
    }

    suspend fun adjustUserBalance(user: UserAccountDoc, deltaBdt: Double, reason: String): Result<Unit> {
        return try {
            requireOwnerUid()
            val userRef = db.collection("users").document(user.userId)
            val lbRef = db.collection("leaderboard_public").document(user.userId)
            val txnId = "adm_adj_${user.userId}_${System.currentTimeMillis()}"

            db.runTransaction { transaction ->
                val userSnap = transaction.get(userRef)
                val lbSnap = transaction.get(lbRef)
                val freshAvail = userSnap.getDouble("availableBalanceBdt") ?: user.availableBalanceBdt
                val freshTotal = userSnap.getDouble("totalEarningsBdt") ?: user.totalEarningsBdt

                val nextAvail = roundBdt((freshAvail + deltaBdt).coerceAtLeast(0.0))
                val nextTotal = if (deltaBdt > 0) roundBdt(freshTotal + deltaBdt) else freshTotal

                transaction.update(
                    userRef,
                    mapOf(
                        "availableBalanceBdt" to nextAvail,
                        "totalEarningsBdt" to nextTotal,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                if (lbSnap.exists()) {
                    transaction.update(
                        lbRef,
                        mapOf(
                            "earningsBdt" to nextTotal,
                            "availableBalanceBdt" to nextAvail,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                transaction.set(
                    db.collection("transactions").document(txnId),
                    mapOf(
                        "userId" to user.userId,
                        "titleBn" to "অ্যাডমিন সমন্বয়: ${reason.ifBlank { "ব্যালেন্স আপডেট" }}",
                        "titleEn" to "Admin Adjustment: ${reason.ifBlank { "Balance Update" }}",
                        "type" to TransactionType.ADMIN_ADJUST.name,
                        "amountBdt" to deltaBdt,
                        "status" to TransactionStatus.COMPLETED.name,
                        "referenceId" to txnId,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/${user.userId}")
            Result.failure(e)
        }
    }

    // --- TASK MANAGEMENT ---

    suspend fun createTask(
        titleBn: String,
        titleEn: String,
        category: TaskCategory,
        rewardBdt: Double,
        descriptionBn: String,
        descriptionEn: String,
        instructionsBn: String,
        instructionsEn: String,
        taskLink: String,
        deadlineText: String,
        totalSlots: Long,
        featured: Boolean,
        instantCredit: Boolean = true,
        verificationCode: String = ""
    ): Result<Unit> {
        val taskId = "tsk_${System.currentTimeMillis()}"
        return try {
            val ownerUid = requireOwnerUid()
            require(titleBn.trim().length >= 2 && titleEn.trim().length >= 2)
            require(rewardBdt > 0.0 && totalSlots >= 1L)
            db.collection("tasks").document(taskId).set(
                mapOf(
                    "titleBn" to titleBn.trim().take(160),
                    "titleEn" to titleEn.trim().take(160),
                    "category" to category.name,
                    "rewardBdt" to rewardBdt,
                    "descriptionBn" to descriptionBn.trim().take(1000),
                    "descriptionEn" to descriptionEn.trim().take(1000),
                    "instructionsBn" to instructionsBn.trim().take(3000),
                    "instructionsEn" to instructionsEn.trim().take(3000),
                    "taskLink" to taskLink.trim().take(500),
                    "deadlineText" to deadlineText.trim().take(80),
                    "totalSlots" to totalSlots,
                    "availableSlots" to totalSlots,
                    "featured" to featured,
                    "active" to true,
                    "instantCredit" to instantCredit,
                    "verificationCode" to verificationCode.trim().take(120),
                    "createdBy" to ownerUid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "tasks/$taskId")
            Result.failure(e)
        }
    }

    suspend fun toggleTaskActive(taskId: String, active: Boolean): Result<Unit> {
        return try {
            requireOwnerUid()
            db.collection("tasks").document(taskId).update(
                mapOf("active" to active, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "tasks/$taskId")
            Result.failure(e)
        }
    }

    suspend fun deleteTask(taskId: String): Result<Unit> {
        return try {
            requireOwnerUid()
            db.collection("tasks").document(taskId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "tasks/$taskId")
            Result.failure(e)
        }
    }

    // --- PROOF REVIEW ---

    suspend fun reviewProof(proof: TaskProofDoc, approve: Boolean, adminNote: String): Result<Unit> {
        return try {
            requireOwnerUid()
            val proofRef = db.collection("task_proofs").document(proof.id)
            val userRef = db.collection("users").document(proof.userId)
            val lbRef = db.collection("leaderboard_public").document(proof.userId)
            val txnId = "apr_${proof.id}_${System.currentTimeMillis()}"

            db.runTransaction { transaction ->
                val proofSnap = transaction.get(proofRef)
                val userSnap = transaction.get(userRef)
                val lbSnap = transaction.get(lbRef)
                if (!userSnap.exists()) {
                    throw IllegalStateException("User not found")
                }
                if (proofSnap.getString("status") != ProofStatus.PENDING_REVIEW.name) {
                    throw IllegalStateException("Proof already reviewed.")
                }

                val curPending = userSnap.getDouble("pendingBalanceBdt") ?: 0.0
                val curAvail = userSnap.getDouble("availableBalanceBdt") ?: 0.0
                val curTotal = userSnap.getDouble("totalEarningsBdt") ?: 0.0
                val curDaily = userSnap.getDouble("dailyEarningsBdt") ?: 0.0
                val curCount = userSnap.getLong("completedTasksCount") ?: 0L

                val newStatus = if (approve) ProofStatus.APPROVED else ProofStatus.REJECTED
                val newPending = roundBdt((curPending - proof.rewardBdt).coerceAtLeast(0.0))

                transaction.update(
                    proofRef,
                    mapOf(
                        "status" to newStatus.name,
                        "adminNote" to adminNote.trim().ifBlank { newStatus.name }.take(500),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                if (approve) {
                    val newAvail = roundBdt(curAvail + proof.rewardBdt)
                    val newTotal = roundBdt(curTotal + proof.rewardBdt)
                    val newDaily = roundBdt(curDaily + proof.rewardBdt)
                    val newCount = curCount + 1L
                    transaction.update(
                        userRef,
                        mapOf(
                            "availableBalanceBdt" to newAvail,
                            "pendingBalanceBdt" to newPending,
                            "totalEarningsBdt" to newTotal,
                            "dailyEarningsBdt" to newDaily,
                            "completedTasksCount" to newCount,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    if (lbSnap.exists()) {
                        transaction.update(
                            lbRef,
                            mapOf(
                                "earningsBdt" to newTotal,
                                "availableBalanceBdt" to newAvail,
                                "tasksCompleted" to newCount,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                    transaction.set(
                        db.collection("transactions").document(txnId),
                        mapOf(
                            "userId" to proof.userId,
                            "titleBn" to "${proof.taskTitle} (অনুমোদিত)",
                            "titleEn" to "${proof.taskTitle} (Approved)",
                            "type" to TransactionType.TASK_EARNING.name,
                            "amountBdt" to proof.rewardBdt,
                            "status" to TransactionStatus.COMPLETED.name,
                            "referenceId" to proof.id,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    transaction.update(
                        userRef,
                        mapOf(
                            "pendingBalanceBdt" to newPending,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "task_proofs/${proof.id}")
            Result.failure(e)
        }
    }

    // --- WITHDRAWAL & VIP REQUEST REVIEW ---

    suspend fun updateWithdrawalStage(
        record: WithdrawalDoc,
        nextStatus: TransactionStatus,
        note: String
    ): Result<Unit> {
        return try {
            requireOwnerUid()
            val wdrRef = db.collection("withdrawals").document(record.id)
            val userRef = db.collection("users").document(record.userId)
            val lbRef = db.collection("leaderboard_public").document(record.userId)
            val txnRef = db.collection("transactions").document("txn_${record.id}")

            db.runTransaction { transaction ->
                val wdrSnap = transaction.get(wdrRef)
                val userSnap = transaction.get(userRef)
                val lbSnap = transaction.get(lbRef)
                val txnSnap = transaction.get(txnRef)

                if (!userSnap.exists()) {
                    throw IllegalStateException("User not found")
                }
                val currentStatusRaw = wdrSnap.getString("status").orEmpty()
                if (currentStatusRaw == TransactionStatus.COMPLETED.name ||
                    currentStatusRaw == TransactionStatus.FAILED.name
                ) {
                    throw IllegalStateException("Withdrawal already finalized ($currentStatusRaw)")
                }

                val curPending = userSnap.getDouble("pendingBalanceBdt") ?: 0.0
                val curAvail = userSnap.getDouble("availableBalanceBdt") ?: 0.0
                val curTotalWithdrawn = userSnap.getDouble("totalWithdrawalsBdt") ?: 0.0
                val newPending = roundBdt((curPending - record.totalDeductedBdt).coerceAtLeast(0.0))

                transaction.update(
                    wdrRef,
                    mapOf(
                        "status" to nextStatus.name,
                        "adminNote" to note.trim().ifBlank { nextStatus.label.en }.take(500),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                when (nextStatus) {
                    TransactionStatus.APPROVED -> {
                        if (txnSnap.exists()) {
                            transaction.update(
                                txnRef,
                                mapOf(
                                    "status" to TransactionStatus.APPROVED.name,
                                    "titleBn" to "${record.methodEnum().title.bn} উত্তোলন অনুমোদিত (মূল ৳${record.amountBdt.toInt()} • ফি ৳${record.feeBdt.toInt()})",
                                    "titleEn" to "${record.methodEnum().title.en} Withdrawal Approved (৳${record.amountBdt.toInt()} • Fee ৳${record.feeBdt.toInt()})"
                                )
                            )
                        }
                    }
                    TransactionStatus.COMPLETED -> {
                        transaction.update(
                            userRef,
                            mapOf(
                                "pendingBalanceBdt" to newPending,
                                "totalWithdrawalsBdt" to roundBdt(curTotalWithdrawn + record.amountBdt),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                        if (txnSnap.exists()) {
                            transaction.update(
                                txnRef,
                                mapOf(
                                    "status" to TransactionStatus.COMPLETED.name,
                                    "titleBn" to "${record.methodEnum().title.bn} পেমেন্ট সফল (মূল ৳${record.amountBdt.toInt()} • ফি ৳${record.feeBdt.toInt()})",
                                    "titleEn" to "${record.methodEnum().title.en} Payment Success (৳${record.amountBdt.toInt()} • Fee ৳${record.feeBdt.toInt()})"
                                )
                            )
                        }
                    }
                    TransactionStatus.FAILED -> {
                        val refundedAvail = roundBdt(curAvail + record.totalDeductedBdt)
                        transaction.update(
                            userRef,
                            mapOf(
                                "pendingBalanceBdt" to newPending,
                                "availableBalanceBdt" to refundedAvail,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                        if (lbSnap.exists()) {
                            transaction.update(
                                lbRef,
                                mapOf(
                                    "availableBalanceBdt" to refundedAvail,
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            )
                        }
                        if (txnSnap.exists()) {
                            transaction.update(
                                txnRef,
                                mapOf(
                                    "status" to TransactionStatus.FAILED.name,
                                    "titleBn" to "${record.methodEnum().title.bn} উত্তোলন বাতিল ও রিফান্ড",
                                    "titleEn" to "${record.methodEnum().title.en} Withdrawal Cancelled & Refunded"
                                )
                            )
                        }
                    }
                    TransactionStatus.PENDING -> Unit
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "withdrawals/${record.id}")
            Result.failure(e)
        }
    }

    suspend fun deleteWithdrawalRecord(withdrawalId: String): Result<Unit> {
        return try {
            requireOwnerUid()
            db.collection("withdrawals").document(withdrawalId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "withdrawals/$withdrawalId")
            Result.failure(e)
        }
    }

    suspend fun deleteCompletedWithdrawals(withdrawalIds: List<String>): Result<Unit> {
        val uniqueIds = withdrawalIds.distinct().filter { it.isNotBlank() }
        if (uniqueIds.isEmpty()) return Result.success(Unit)
        return try {
            requireOwnerUid()
            uniqueIds.chunked(450).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { id ->
                    batch.delete(db.collection("withdrawals").document(id))
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "withdrawals/batch")
            Result.failure(e)
        }
    }

    suspend fun reviewVipRequest(req: VipRequestDoc, approve: Boolean): Result<Unit> {
        return try {
            requireOwnerUid()
            val batch = db.batch()
            val status = if (approve) TransactionStatus.COMPLETED else TransactionStatus.FAILED
            batch.update(
                db.collection("vip_requests").document(req.id),
                mapOf("status" to status.name, "updatedAt" to FieldValue.serverTimestamp())
            )
            if (approve) {
                batch.update(
                    db.collection("users").document(req.userId),
                    mapOf("vipTier" to req.targetTier, "updatedAt" to FieldValue.serverTimestamp())
                )
                batch.update(
                    db.collection("leaderboard_public").document(req.userId),
                    mapOf("vipTier" to req.targetTier, "updatedAt" to FieldValue.serverTimestamp())
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "vip_requests/${req.id}")
            Result.failure(e)
        }
    }

    // --- APP CONFIG ---

    suspend fun saveFullAppConfig(config: AppConfigDoc): Result<Unit> {
        val bkash = AdminSecurity.normalizeBdMobile(config.bkashNumber)
        val nagad = AdminSecurity.normalizeBdMobile(config.nagadNumber)
        return try {
            requireOwnerUid()
            require(AdminSecurity.isValidBdMobile(bkash) && AdminSecurity.isValidBdMobile(nagad)) {
                "Invalid admin bKash/Nagad mobile number."
            }
            val payload = mapOf(
                "bkashNumber" to bkash,
                "nagadNumber" to nagad,
                "vipBronzePriceBdt" to config.vipBronzePriceBdt.coerceAtLeast(0.0),
                "vipSilverPriceBdt" to config.vipSilverPriceBdt.coerceAtLeast(0.0),
                "vipGoldPriceBdt" to config.vipGoldPriceBdt.coerceAtLeast(0.0),
                "vipDiamondPriceBdt" to config.vipDiamondPriceBdt.coerceAtLeast(0.0),
                "latestAppVersion" to config.latestAppVersion.trim().ifBlank { "1.0.0" }.take(40),
                "appDownloadUrl" to config.appDownloadUrl.trim().take(500).ifBlank { null },
                "updateMessageBn" to config.updateMessageBn?.trim()?.take(500)?.ifBlank { null },
                "updateMessageEn" to config.updateMessageEn?.trim()?.take(500)?.ifBlank { null },
                "videoAdsEnabled" to config.videoAdsEnabled,
                "videoAdsComingSoon" to config.videoAdsComingSoon,
                "videoAdsRewardBdt" to config.videoAdsRewardBdt.coerceIn(0.0, 1000.0),
                "videoAdsTitleBn" to config.videoAdsTitleBn?.trim()?.take(160)?.ifBlank { null },
                "videoAdsTitleEn" to config.videoAdsTitleEn?.trim()?.take(160)?.ifBlank { null },
                "videoAdsUrl" to config.videoAdsUrl?.trim()?.take(500)?.ifBlank { null },
                "vpnAutoBanEnabled" to config.vpnAutoBanEnabled,
                "foreignIpAutoBanEnabled" to config.foreignIpAutoBanEnabled,
                "appStopped" to config.appStopped,
                "stopMessageBn" to config.stopMessageBn?.trim()?.take(500)?.ifBlank { null },
                "stopMessageEn" to config.stopMessageEn?.trim()?.take(500)?.ifBlank { null },
                "stopEndEpochMillis" to config.stopEndEpochMillis.coerceAtLeast(0L),
                "dashboardTopNoticeActive" to config.dashboardTopNoticeActive,
                "dashboardTopNoticeBn" to config.dashboardTopNoticeBn?.trim()?.take(500)?.ifBlank { null },
                "dashboardTopNoticeEn" to config.dashboardTopNoticeEn?.trim()?.take(500)?.ifBlank { null },
                "supportEnabled" to config.supportEnabled,
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            db.collection("app_config").document("payment_settings").set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "app_config/payment_settings")
            Result.failure(e)
        }
    }

    // --- ANNOUNCEMENTS & SUPPORT ---

    suspend fun broadcastAnnouncement(
        category: NotificationCategory,
        titleBn: String,
        titleEn: String,
        messageBn: String,
        messageEn: String,
        appVersion: String?,
        updateUrl: String?
    ): Result<Unit> {
        val id = "ann_${System.currentTimeMillis()}"
        return try {
            requireOwnerUid()
            require(titleBn.isNotBlank() && titleEn.isNotBlank())
            val payload = mapOf(
                "category" to category.name,
                "titleBn" to titleBn.trim().take(200),
                "titleEn" to titleEn.trim().take(200),
                "messageBn" to messageBn.trim().take(2000),
                "messageEn" to messageEn.trim().take(2000),
                "appVersion" to appVersion?.trim()?.take(40)?.ifBlank { null },
                "updateUrl" to updateUrl?.trim()?.take(400)?.ifBlank { null },
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            db.collection("announcements").document(id).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "announcements/$id")
            Result.failure(e)
        }
    }

    suspend fun updateSupportTicket(ticketId: String, status: TicketStatus, reply: String): Result<Unit> {
        return try {
            requireOwnerUid()
            db.collection("support_tickets").document(ticketId).update(
                mapOf(
                    "status" to status.name,
                    "adminReply" to reply.trim().take(1000),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "support_tickets/$ticketId")
            Result.failure(e)
        }
    }

    suspend fun deleteSupportTickets(ticketIds: List<String>): Result<Unit> {
        val uniqueIds = ticketIds.distinct().filter { it.isNotBlank() }
        if (uniqueIds.isEmpty()) return Result.success(Unit)
        return try {
            requireOwnerUid()
            uniqueIds.chunked(450).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { id ->
                    batch.delete(db.collection("support_tickets").document(id))
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "support_tickets/batch")
            Result.failure(e)
        }
    }

    private fun logIfActive(error: Throwable, op: OperationType, path: String, expectedUid: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val toLog = firestoreError ?: (error as? Exception)
        if (toLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(toLog, op, path)
        }
    }
}
