package com.example.data.repository

import android.content.Context
import com.example.core.firebase.FirestoreProvider
import com.example.core.firebase.OperationType
import com.example.core.firebase.handleFirestoreError
import com.example.core.security.AdminSecurity
import com.example.core.security.NetworkSecuritySnapshot
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

class RealRewardFirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(FirestoreProvider.getDatabase(context))

    private fun roundBdt(value: Double): Double = (value * 100.0).roundToLong() / 100.0

    private suspend fun assertAppNotStopped() {
        if (AdminSecurity.isVerifiedOwner(auth.currentUser)) return
        val snap = try {
            db.collection("app_config").document("payment_settings").get().await()
        } catch (_: Exception) {
            null
        } ?: return
        if (!snap.exists()) return
        val stopped = snap.getBoolean("appStopped") == true
        val endEpoch = snap.getLong("stopEndEpochMillis") ?: 0L
        if (stopped && (endEpoch == 0L || System.currentTimeMillis() < endEpoch)) {
            throw IllegalStateException("APP_MAINTENANCE_ACTIVE")
        }
    }

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeUserAccount(userId: String): Flow<UserAccountDoc?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Session UID mismatch." }
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots(MetadataChanges.INCLUDE)
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(UserAccountDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.GET, path, uid)
                    throw error
                }
        )
    }

    fun observeActiveTasks(): Flow<List<MicroTaskDoc>> = flow {
        val uid = requireUserId()
        val path = "tasks"
        emitAll(
            db.collection("tasks")
                .limit(100)
                .snapshots(MetadataChanges.INCLUDE)
                .map { snapshot ->
                    snapshot.toObjects(MicroTaskDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .filter { it.active }
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeMyProofs(userId: String): Flow<List<TaskProofDoc>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Session UID mismatch." }
        val path = "task_proofs"
        emitAll(
            db.collection("task_proofs")
                .whereEqualTo("userId", uid)
                .limit(100)
                .snapshots(MetadataChanges.INCLUDE)
                .map { snapshot ->
                    snapshot.toObjects(TaskProofDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeMyTransactions(userId: String): Flow<List<TransactionDoc>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Session UID mismatch." }
        val path = "transactions"
        emitAll(
            db.collection("transactions")
                .whereEqualTo("userId", uid)
                .limit(100)
                .snapshots(MetadataChanges.INCLUDE)
                .map { snapshot ->
                    snapshot.toObjects(TransactionDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeMyWithdrawals(userId: String): Flow<List<WithdrawalDoc>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Session UID mismatch." }
        val path = "withdrawals"
        emitAll(
            db.collection("withdrawals")
                .whereEqualTo("userId", uid)
                .limit(100)
                .snapshots(MetadataChanges.INCLUDE)
                .map { snapshot ->
                    snapshot.toObjects(WithdrawalDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeMyReferrals(userId: String): Flow<List<ReferralRecordDoc>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Session UID mismatch." }
        val path = "referrals"
        emitAll(
            db.collection("referrals")
                .whereEqualTo("referrerId", uid)
                .limit(100)
                .snapshots(MetadataChanges.INCLUDE)
                .map { snapshot ->
                    snapshot.toObjects(ReferralRecordDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeMyVipRequests(userId: String): Flow<List<VipRequestDoc>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Session UID mismatch." }
        val path = "vip_requests"
        emitAll(
            db.collection("vip_requests")
                .whereEqualTo("userId", uid)
                .limit(50)
                .snapshots(MetadataChanges.INCLUDE)
                .map { snapshot ->
                    snapshot.toObjects(VipRequestDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeMySupportTickets(userId: String): Flow<List<SupportTicketDoc>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Session UID mismatch." }
        val path = "support_tickets"
        emitAll(
            db.collection("support_tickets")
                .whereEqualTo("userId", uid)
                .limit(50)
                .snapshots(MetadataChanges.INCLUDE)
                .map { snapshot ->
                    snapshot.toObjects(SupportTicketDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeAnnouncements(): Flow<List<AnnouncementDoc>> = flow {
        val uid = requireUserId()
        val path = "announcements"
        emitAll(
            db.collection("announcements")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .snapshots(MetadataChanges.INCLUDE)
                .map { snapshot ->
                    snapshot.toObjects(AnnouncementDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observeLeaderboard(): Flow<List<LeaderboardPublicDoc>> = flow {
        val uid = requireUserId()
        val path = "leaderboard_public"
        emitAll(
            db.collection("leaderboard_public")
                .orderBy("earningsBdt", Query.Direction.DESCENDING)
                .limit(50)
                .snapshots(MetadataChanges.INCLUDE)
                .map { snapshot ->
                    snapshot.toObjects(LeaderboardPublicDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.availableBalanceBdt ?: it.earningsBdt }
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.LIST, path, uid)
                    throw error
                }
        )
    }

    fun observePaymentConfig(): Flow<AppConfigDoc> = flow {
        val uid = requireUserId()
        val path = "app_config/payment_settings"
        emitAll(
            db.collection("app_config").document("payment_settings")
                .snapshots(MetadataChanges.INCLUDE)
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(AppConfigDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            ?: AppConfigDoc()
                    } else AppConfigDoc()
                }
                .catch { error ->
                    logStreamErrorIfActive(error, OperationType.GET, path, uid)
                    throw error
                }
        )
    }

    suspend fun checkRemovedAccount(uid: String, email: String): RemovedAccountDoc? {
        return try {
            val uidSnap = db.collection("removed_accounts").document(uid).get().await()
            if (uidSnap.exists()) {
                return uidSnap.toObject(RemovedAccountDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: RemovedAccountDoc(userId = uid, email = email)
            }
            if (email.isNotBlank()) {
                val emailKey = AdminSecurity.sanitizeEmailKey(email)
                if (emailKey.isNotBlank()) {
                    val emailSnap = db.collection("removed_emails").document(emailKey).get().await()
                    if (emailSnap.exists()) {
                        return emailSnap.toObject(RemovedAccountDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            ?: RemovedAccountDoc(userId = uid, email = email)
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun checkDeviceRegistryOwner(deviceId: String): String? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            val snap = db.collection("device_registry").document(deviceId).get().await()
            if (snap.exists()) {
                val ownerUid = snap.getString("userId")
                if (!ownerUid.isNullOrBlank() && ownerUid != uid) ownerUid else null
            } else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun recordNetworkCheckAndAutoBanIfNeeded(
        user: UserAccountDoc,
        snapshot: NetworkSecuritySnapshot,
        config: AppConfigDoc,
        isOwner: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.success(Unit)
        if (uid != user.userId || user.banned) return Result.success(Unit)

        val shouldBanVpn = !isOwner && config.vpnAutoBanEnabled && snapshot.vpnDetected
        val shouldBanForeignIp = !isOwner && config.foreignIpAutoBanEnabled &&
            snapshot.countryCode.isNotBlank() &&
            !snapshot.countryCode.equals("BD", ignoreCase = true)

        val banReason = when {
            shouldBanVpn -> "VPN_DETECTED"
            shouldBanForeignIp -> "FOREIGN_IP_${snapshot.countryCode}"
            else -> null
        }

        if (banReason == null &&
            user.lastIpAddress == snapshot.ipAddress &&
            user.lastCountryCode == snapshot.countryCode &&
            user.lastLocation == snapshot.locationLabel &&
            user.vpnDetected == snapshot.vpnDetected
        ) {
            return Result.success(Unit)
        }

        return try {
            val batch = db.batch()
            val payload = mutableMapOf<String, Any>(
                "lastIpAddress" to snapshot.ipAddress.take(64),
                "lastCountryCode" to snapshot.countryCode.take(16),
                "lastLocation" to snapshot.locationLabel.take(160),
                "vpnDetected" to snapshot.vpnDetected,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (banReason != null) {
                payload["banned"] = true
                payload["permanentlyBanned"] = true
                payload["banReason"] = banReason.take(160)
                val alertId = "sec_${uid}_${System.currentTimeMillis()}"
                batch.set(
                    db.collection("fraud_alerts").document(alertId),
                    mapOf(
                        "userId" to uid,
                        "username" to user.username,
                        "email" to user.email,
                        "alertType" to if (shouldBanVpn) "VPN_DETECTED" else "FOREIGN_IP",
                        "titleBn" to if (shouldBanVpn) "VPN ব্যবহারে Auto-Ban" else "বিদেশি IP শনাক্তে Auto-Ban",
                        "titleEn" to if (shouldBanVpn) "VPN Auto-Ban Triggered" else "Foreign IP Auto-Ban",
                        "details" to "Reason: $banReason | IP: ${snapshot.ipAddress}",
                        "ipAddress" to snapshot.ipAddress.take(64),
                        "locationLabel" to snapshot.locationLabel.take(160),
                        "resolved" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.update(db.collection("users").document(uid), payload)

            if (user.referralCode.isNotBlank()) {
                batch.update(
                    db.collection("referral_codes").document(user.referralCode),
                    mapOf(
                        "lastIpAddress" to snapshot.ipAddress.take(64),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    /**
     * Registers a brand-new account with strict single-mobile & single-device lock,
     * awards exact ৳10 signup bonus, and verifies any referral code via IP + Device anti-scam rules (৳15 referrer bonus).
     */
    suspend fun registerNewAccount(
        fullName: String,
        username: String,
        mobileRaw: String,
        deviceId: String,
        referralCodeRaw: String = "",
        passwordRaw: String = "",
        ipAddress: String = "unknown",
        countryCode: String = "",
        locationLabel: String = "",
        vpnDetected: Boolean = false
    ): Result<Unit> {
        val mobile = AdminSecurity.normalizeBdMobile(mobileRaw)
        val cleanName = fullName.trim()
        val cleanUser = username.trim()
        val cleanReferralInput = referralCodeRaw.trim().uppercase()
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Google Sign-In required."))
        val email = auth.currentUser?.email.orEmpty()
        val path = "users/$uid"

        return try {
            assertAppNotStopped()
            require(cleanName.length in 2..100) { "Invalid full name length." }
            require(cleanUser.length in 3..40) { "Invalid username length." }
            require(AdminSecurity.isValidBdMobile(mobile)) { "Invalid Bangladesh mobile number." }

            // Ensure permanently removed UIDs/emails cannot re-register
            val removed = checkRemovedAccount(uid, email)
            if (removed != null) {
                return Result.failure(IllegalStateException("ACCOUNT_REMOVED_PERMANENTLY"))
            }

            // Check single-account mobile & device lock before batch commit
            val mobileSnap = db.collection("mobile_registry").document(mobile).get().await()
            if (mobileSnap.exists() && mobileSnap.getString("userId") != uid) {
                return Result.failure(IllegalStateException("DUPLICATE_MOBILE"))
            }
            val deviceSnap = db.collection("device_registry").document(deviceId).get().await()
            if (deviceSnap.exists() && deviceSnap.getString("userId") != uid) {
                return Result.failure(IllegalStateException("DUPLICATE_DEVICE"))
            }

            var refSnap: DocumentSnapshot? = null
            if (cleanReferralInput.isNotBlank()) {
                val candidate = db.collection("referral_codes").document(cleanReferralInput).get().await()
                if (!candidate.exists()) {
                    return Result.failure(IllegalStateException("INVALID_REFERRAL_CODE"))
                }
                refSnap = candidate
            }

            val accountCode = "RR-BD-${(10000..99999).random()}"
            val myReferralCode = AdminSecurity.generateReferralCode(uid)
            val batch = db.batch()

            val referrerUid = refSnap?.getString("userId")?.takeIf { it.isNotBlank() }
            val referrerUsername = refSnap?.getString("username").orEmpty().ifBlank { "member" }
            val referrerDeviceId = refSnap?.getString("deviceId").orEmpty()
            val referrerIp = refSnap?.getString("lastIpAddress").orEmpty()

            var referralScamReason: String? = null
            if (refSnap != null && referrerUid != null) {
                referralScamReason = when {
                    referrerUid == uid -> "Self-referral attempt"
                    referrerDeviceId == deviceId -> "Same device ID detected ($deviceId)"
                    ipAddress != "unknown" && ipAddress.isNotBlank() && referrerIp == ipAddress ->
                        "Same IP address referral scam ($ipAddress)"
                    vpnDetected -> "VPN detected during referred signup"
                    countryCode.isNotBlank() && !countryCode.equals("BD", ignoreCase = true) ->
                        "Foreign IP ($countryCode) referral attempt"
                    else -> null
                }
            }

            val cleanPassword = passwordRaw.trim()
            val passwordHash = if (cleanPassword.length >= 6) {
                AdminSecurity.hashUserPassword(uid, cleanPassword)
            } else ""

            val userPayload = mapOf(
                "userId" to uid,
                "accountId" to accountCode,
                "email" to email,
                "fullName" to cleanName,
                "username" to cleanUser,
                "mobileNumber" to mobile,
                "deviceId" to deviceId,
                "bkashPersonal" to "",
                "nagadPersonal" to "",
                "paymentAccountsLocked" to false,
                "walletChangeApprovedOnce" to false,
                "walletResetCount" to 0L,
                "securityPasswordHash" to passwordHash,
                "vipTier" to VipTierCode.REGULAR.name,
                "banned" to false,
                "permanentlyBanned" to false,
                "twoFactorEnabled" to true,
                "loginAlertsEnabled" to true,
                "registrationBonusClaimed" to true,
                "availableBalanceBdt" to 10.0,
                "pendingBalanceBdt" to 0.0,
                "totalEarningsBdt" to 10.0,
                "totalWithdrawalsBdt" to 0.0,
                "dailyEarningsBdt" to 0.0,
                "completedTasksCount" to 0L,
                "lastIpAddress" to ipAddress.take(64),
                "lastCountryCode" to countryCode.take(16),
                "lastLocation" to locationLabel.take(160),
                "vpnDetected" to vpnDetected,
                "referralCode" to myReferralCode,
                "referredByCode" to if (refSnap != null) cleanReferralInput else null,
                "referredByUid" to referrerUid,
                "referralCount" to 0L,
                "referralEarningsBdt" to 0.0,
                "referralScamCount" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            batch.set(db.collection("users").document(uid), userPayload)

            batch.set(
                db.collection("referral_codes").document(myReferralCode),
                mapOf(
                    "code" to myReferralCode,
                    "userId" to uid,
                    "username" to cleanUser,
                    "deviceId" to deviceId,
                    "lastIpAddress" to ipAddress.take(64),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            batch.set(
                db.collection("leaderboard_public").document(uid),
                mapOf(
                    "userId" to uid,
                    "displayName" to cleanName,
                    "username" to cleanUser,
                    "vipTier" to VipTierCode.REGULAR.name,
                    "tasksCompleted" to 0L,
                    "earningsBdt" to 10.0,
                    "availableBalanceBdt" to 10.0,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            if (!mobileSnap.exists()) {
                batch.set(
                    db.collection("mobile_registry").document(mobile),
                    mapOf("userId" to uid, "createdAt" to FieldValue.serverTimestamp())
                )
            }
            if (!deviceSnap.exists()) {
                batch.set(
                    db.collection("device_registry").document(deviceId),
                    mapOf("userId" to uid, "createdAt" to FieldValue.serverTimestamp())
                )
            }

            val bonusTxnId = "bonus_${uid}"
            batch.set(
                db.collection("transactions").document(bonusTxnId),
                mapOf(
                    "userId" to uid,
                    "titleBn" to "রেজিস্ট্রেশন বোনাস (৳১০)",
                    "titleEn" to "Registration Welcome Bonus (৳10)",
                    "type" to TransactionType.BONUS.name,
                    "amountBdt" to 10.0,
                    "status" to TransactionStatus.COMPLETED.name,
                    "referenceId" to "SIGNUP-$accountCode",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )

            // Process Referral Verification + ৳15 Bonus or Scam Flag
            if (refSnap != null && referrerUid != null) {
                val isScam = referralScamReason != null
                val referralDocPayload = mutableMapOf<String, Any>(
                    "referrerId" to referrerUid,
                    "referrerUsername" to referrerUsername,
                    "referredId" to uid,
                    "referredUsername" to cleanUser,
                    "referredEmail" to email.take(200),
                    "referralCode" to cleanReferralInput,
                    "referredIp" to ipAddress.take(64),
                    "referredDeviceId" to deviceId.take(128),
                    "bonusBdt" to if (isScam) 0.0 else 15.0,
                    "status" to if (isScam) "FAILED_SCAM" else "CREDITED",
                    "createdAt" to FieldValue.serverTimestamp()
                )
                if (referralScamReason != null) {
                    referralDocPayload["scamReason"] = referralScamReason.take(300)
                }
                batch.set(db.collection("referrals").document(uid), referralDocPayload)

                if (isScam) {
                    if (referrerUid != uid) {
                        batch.update(
                            db.collection("users").document(referrerUid),
                            mapOf(
                                "referralScamCount" to FieldValue.increment(1L),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                    val alertId = "refscam_${uid}_${System.currentTimeMillis()}"
                    batch.set(
                        db.collection("fraud_alerts").document(alertId),
                        mapOf(
                            "userId" to referrerUid,
                            "username" to referrerUsername,
                            "email" to email.take(200),
                            "alertType" to "REFERRAL_SCAM",
                            "titleBn" to "ভুয়া রেফারেল স্ক্যাম চেষ্টা শনাক্ত",
                            "titleEn" to "Referral Scam Attempt Detected",
                            "details" to "Code: $cleanReferralInput | New User: @$cleanUser ($email) | $referralScamReason",
                            "ipAddress" to ipAddress.take(64),
                            "locationLabel" to locationLabel.take(160),
                            "resolved" to false,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    batch.update(
                        db.collection("users").document(referrerUid),
                        mapOf(
                            "availableBalanceBdt" to FieldValue.increment(15.0),
                            "totalEarningsBdt" to FieldValue.increment(15.0),
                            "referralCount" to FieldValue.increment(1L),
                            "referralEarningsBdt" to FieldValue.increment(15.0),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    batch.update(
                        db.collection("leaderboard_public").document(referrerUid),
                        mapOf(
                            "earningsBdt" to FieldValue.increment(15.0),
                            "availableBalanceBdt" to FieldValue.increment(15.0),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    batch.set(
                        db.collection("transactions").document("ref_${uid}"),
                        mapOf(
                            "userId" to referrerUid,
                            "titleBn" to "রেফারেল বোনাস: @$cleanUser (+৳১৫)",
                            "titleEn" to "Referral Bonus: @$cleanUser (+৳15)",
                            "type" to TransactionType.BONUS.name,
                            "amountBdt" to 15.0,
                            "status" to TransactionStatus.COMPLETED.name,
                            "referenceId" to "REF_$uid",
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun syncLeaderboardFromUserIfNeeded(user: UserAccountDoc) {
        val uid = auth.currentUser?.uid ?: return
        if (uid != user.userId) return
        try {
            val lbRef = db.collection("leaderboard_public").document(uid)
            val snap = lbRef.get().await()
            if (!snap.exists()) {
                lbRef.set(
                    mapOf(
                        "userId" to uid,
                        "displayName" to user.fullName,
                        "username" to user.username,
                        "vipTier" to user.vipTier,
                        "tasksCompleted" to user.completedTasksCount,
                        "earningsBdt" to user.totalEarningsBdt,
                        "availableBalanceBdt" to user.availableBalanceBdt,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val curAvail = snap.getDouble("availableBalanceBdt")
                val curEarnings = snap.getDouble("earningsBdt")
                val curTasks = snap.getLong("tasksCompleted")
                val curVip = snap.getString("vipTier")
                if (curAvail != user.availableBalanceBdt ||
                    curEarnings != user.totalEarningsBdt ||
                    curTasks != user.completedTasksCount ||
                    curVip != user.vipTier
                ) {
                    lbRef.update(
                        mapOf(
                            "displayName" to user.fullName,
                            "username" to user.username,
                            "vipTier" to user.vipTier,
                            "tasksCompleted" to user.completedTasksCount,
                            "earningsBdt" to user.totalEarningsBdt,
                            "availableBalanceBdt" to user.availableBalanceBdt,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
        } catch (_: Exception) {
            // Best-effort non-fatal ranking parity sync
        }
    }

    suspend fun submitTaskProof(
        task: MicroTaskDoc,
        user: UserAccountDoc,
        proofNote: String,
        attachmentLabel: String?
    ): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Sign in required."))
        val proofId = "${task.id}_$uid"
        val path = "task_proofs/$proofId"

        return try {
            assertAppNotStopped()
            require(proofNote.trim().isNotEmpty() || !attachmentLabel.isNullOrBlank()) { "EMPTY_PROOF" }
            val cleanNote = proofNote.trim().ifBlank { "Screenshot attached: $attachmentLabel" }.take(1500)
            val taskRef = db.collection("tasks").document(task.id)
            val userRef = db.collection("users").document(uid)
            val lbRef = db.collection("leaderboard_public").document(uid)
            val proofRef = db.collection("task_proofs").document(proofId)
            val txnId = "txn_proof_${task.id}_${uid}_${System.currentTimeMillis()}"

            db.runTransaction { transaction ->
                val taskSnap = transaction.get(taskRef)
                val userSnap = transaction.get(userRef)
                val lbSnap = transaction.get(lbRef)
                val existingProof = transaction.get(proofRef)

                if (userSnap.getBoolean("banned") == true) {
                    throw IllegalStateException("ACCOUNT_BANNED")
                }
                if (existingProof.exists()) {
                    throw IllegalStateException("PROOF_ALREADY_SUBMITTED")
                }
                val isActive = taskSnap.getBoolean("active") == true
                val curSlots = taskSnap.getLong("availableSlots") ?: 0L
                val baseReward = taskSnap.getDouble("rewardBdt") ?: task.rewardBdt
                val isInstantCredit = taskSnap.getBoolean("instantCredit") == true
                val requiredCode = taskSnap.getString("verificationCode").orEmpty().trim()
                if (!isActive || curSlots < 1L) {
                    throw IllegalStateException("NO_SLOTS_AVAILABLE")
                }
                if (isInstantCredit && requiredCode.isNotEmpty() &&
                    !cleanNote.contains(requiredCode, ignoreCase = true)
                ) {
                    throw IllegalStateException("INVALID_TASK_VERIFICATION")
                }

                val freshUsername = userSnap.getString("username").orEmpty().ifBlank { user.username }
                val freshName = userSnap.getString("fullName").orEmpty().ifBlank { user.fullName }
                val freshVip = userSnap.getString("vipTier").orEmpty().ifBlank { user.vipTier }
                val vipBonusPct = AdminSecurity.vipBonusPercentForTier(freshVip)
                val reward = AdminSecurity.calculateTaskRewardWithVip(baseReward, freshVip)
                val vipTagBn = if (vipBonusPct > 0) " (+${vipBonusPct}% VIP)" else ""
                val vipTagEn = if (vipBonusPct > 0) " (+${vipBonusPct}% VIP)" else ""
                val freshTaskTitle = taskSnap.getString("titleEn").orEmpty()
                    .ifBlank { taskSnap.getString("titleBn").orEmpty() }
                    .ifBlank { task.titleEn.ifBlank { task.titleBn } }

                val proofMap = mutableMapOf<String, Any>(
                    "taskId" to task.id,
                    "taskTitle" to freshTaskTitle.take(200),
                    "userId" to uid,
                    "username" to freshUsername,
                    "rewardBdt" to reward,
                    "proofNote" to cleanNote,
                    "status" to if (isInstantCredit) ProofStatus.APPROVED.name else ProofStatus.PENDING_REVIEW.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!attachmentLabel.isNullOrBlank()) {
                    proofMap["attachmentLabel"] = attachmentLabel.take(200)
                }
                if (isInstantCredit) {
                    proofMap["adminNote"] = "Instant Verified Credit$vipTagEn"
                }

                transaction.set(proofRef, proofMap)
                transaction.update(
                    taskRef,
                    mapOf(
                        "availableSlots" to (curSlots - 1L).coerceAtLeast(0L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                if (isInstantCredit) {
                    val curAvail = userSnap.getDouble("availableBalanceBdt") ?: 0.0
                    val curTotal = userSnap.getDouble("totalEarningsBdt") ?: 0.0
                    val curDaily = userSnap.getDouble("dailyEarningsBdt") ?: 0.0
                    val curTasks = userSnap.getLong("completedTasksCount") ?: 0L

                    val newAvail = roundBdt(curAvail + reward)
                    val newTotal = roundBdt(curTotal + reward)
                    val newDaily = roundBdt(curDaily + reward)
                    val newTasks = curTasks + 1L

                    transaction.update(
                        userRef,
                        mapOf(
                            "availableBalanceBdt" to newAvail,
                            "totalEarningsBdt" to newTotal,
                            "dailyEarningsBdt" to newDaily,
                            "completedTasksCount" to newTasks,
                            "lastCompletedTaskId" to task.id,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    if (lbSnap.exists()) {
                        transaction.update(
                            lbRef,
                            mapOf(
                                "displayName" to freshName,
                                "username" to freshUsername,
                                "vipTier" to freshVip,
                                "tasksCompleted" to newTasks,
                                "earningsBdt" to newTotal,
                                "availableBalanceBdt" to newAvail,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    } else {
                        transaction.set(
                            lbRef,
                            mapOf(
                                "userId" to uid,
                                "displayName" to freshName,
                                "username" to freshUsername,
                                "vipTier" to freshVip,
                                "tasksCompleted" to newTasks,
                                "earningsBdt" to newTotal,
                                "availableBalanceBdt" to newAvail,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                    transaction.set(
                        db.collection("transactions").document(txnId),
                        mapOf(
                            "userId" to uid,
                            "titleBn" to "${task.titleBn} (তাৎক্ষণিক পেমেন্ট$vipTagBn)",
                            "titleEn" to "${task.titleEn} (Instant Credit$vipTagEn)",
                            "type" to TransactionType.TASK_EARNING.name,
                            "amountBdt" to reward,
                            "status" to TransactionStatus.COMPLETED.name,
                            "referenceId" to proofId,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    val curPending = userSnap.getDouble("pendingBalanceBdt") ?: 0.0
                    transaction.update(
                        userRef,
                        mapOf(
                            "pendingBalanceBdt" to roundBdt(curPending + reward),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    transaction.set(
                        db.collection("transactions").document(txnId),
                        mapOf(
                            "userId" to uid,
                            "titleBn" to "${task.titleBn} (প্রুফ পেন্ডিং$vipTagBn)",
                            "titleEn" to "${task.titleEn} (Proof Pending$vipTagEn)",
                            "type" to TransactionType.TASK_EARNING.name,
                            "amountBdt" to reward,
                            "status" to TransactionStatus.PENDING.name,
                            "referenceId" to proofId,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun submitWithdrawalRequest(
        user: UserAccountDoc,
        method: WithdrawalMethod,
        accountRaw: String,
        amountBdt: Double
    ): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Sign in required."))
        val account = AdminSecurity.normalizeBdMobile(accountRaw)
        val cleanAmount = roundBdt(amountBdt)
        val fee = AdminSecurity.calculateWithdrawalFeeBdt(cleanAmount)
        val totalDeducted = roundBdt(cleanAmount + fee)
        val withdrawalId = "wdr_${uid}_${System.currentTimeMillis()}"
        val path = "withdrawals/$withdrawalId"

        return try {
            assertAppNotStopped()
            require(AdminSecurity.isValidBdMobile(account)) { "INVALID_MOBILE" }
            require(cleanAmount >= 300.0) { "MIN_WITHDRAWAL_300" }

            val userRef = db.collection("users").document(uid)
            val lbRef = db.collection("leaderboard_public").document(uid)

            db.runTransaction { transaction ->
                val userSnap = transaction.get(userRef)
                val lbSnap = transaction.get(lbRef)

                if (userSnap.getBoolean("banned") == true) {
                    throw IllegalStateException("ACCOUNT_BANNED")
                }
                val freshAvail = userSnap.getDouble("availableBalanceBdt") ?: 0.0
                val freshPend = userSnap.getDouble("pendingBalanceBdt") ?: 0.0
                val freshTotal = userSnap.getDouble("totalEarningsBdt") ?: 0.0
                val freshTasks = userSnap.getLong("completedTasksCount") ?: 0L
                val freshVip = userSnap.getString("vipTier").orEmpty().ifBlank { user.vipTier }
                val freshName = userSnap.getString("fullName").orEmpty().ifBlank { user.fullName }
                val freshUsername = userSnap.getString("username").orEmpty().ifBlank { user.username }

                if (freshAvail < totalDeducted) {
                    throw IllegalStateException("INSUFFICIENT_BALANCE_WITH_FEE")
                }

                val newAvail = roundBdt((freshAvail - totalDeducted).coerceAtLeast(0.0))
                val newPend = roundBdt(freshPend + totalDeducted)

                transaction.set(
                    db.collection("withdrawals").document(withdrawalId),
                    mapOf(
                        "userId" to uid,
                        "username" to freshUsername,
                        "method" to method.name,
                        "accountNumber" to account,
                        "amountBdt" to cleanAmount,
                        "feeBdt" to fee,
                        "totalDeductedBdt" to totalDeducted,
                        "status" to TransactionStatus.PENDING.name,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                transaction.update(
                    userRef,
                    mapOf(
                        "availableBalanceBdt" to newAvail,
                        "pendingBalanceBdt" to newPend,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                if (lbSnap.exists()) {
                    transaction.update(
                        lbRef,
                        mapOf(
                            "displayName" to freshName,
                            "username" to freshUsername,
                            "vipTier" to freshVip,
                            "tasksCompleted" to freshTasks,
                            "earningsBdt" to freshTotal,
                            "availableBalanceBdt" to newAvail,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    transaction.set(
                        lbRef,
                        mapOf(
                            "userId" to uid,
                            "displayName" to freshName,
                            "username" to freshUsername,
                            "vipTier" to freshVip,
                            "tasksCompleted" to freshTasks,
                            "earningsBdt" to freshTotal,
                            "availableBalanceBdt" to newAvail,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                transaction.set(
                    db.collection("transactions").document("txn_$withdrawalId"),
                    mapOf(
                        "userId" to uid,
                        "titleBn" to "${method.title.bn} উত্তোলন (মূল ৳${cleanAmount.toInt()} + ফি ৳${fee.toInt()})",
                        "titleEn" to "${method.title.en} Withdrawal (৳${cleanAmount.toInt()} + Fee ৳${fee.toInt()})",
                        "type" to TransactionType.WITHDRAWAL.name,
                        "amountBdt" to -totalDeducted,
                        "feeBdt" to fee,
                        "netAmountBdt" to cleanAmount,
                        "status" to TransactionStatus.PENDING.name,
                        "referenceId" to withdrawalId,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun submitVipUpgradeRequest(
        user: UserAccountDoc,
        targetTier: VipTierCode,
        method: WithdrawalMethod,
        senderNumberRaw: String,
        trxId: String,
        amountBdt: Double
    ): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Sign in required."))
        val sender = AdminSecurity.normalizeBdMobile(senderNumberRaw)
        val reqId = "vip_${uid}_${System.currentTimeMillis()}"
        return try {
            assertAppNotStopped()
            check(!user.banned) { "ACCOUNT_BANNED" }
            require(AdminSecurity.isValidBdMobile(sender)) { "INVALID_MOBILE" }
            require(trxId.trim().length in 4..60) { "INVALID_TRX_ID" }
            db.collection("vip_requests").document(reqId).set(
                mapOf(
                    "userId" to uid,
                    "username" to user.username,
                    "targetTier" to targetTier.name,
                    "method" to method.name,
                    "senderNumber" to sender,
                    "transactionId" to trxId.trim(),
                    "amountBdt" to amountBdt,
                    "status" to TransactionStatus.PENDING.name,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "vip_requests/$reqId")
            Result.failure(e)
        }
    }

    suspend fun submitSupportTicket(
        user: UserAccountDoc,
        category: SupportCategory,
        subject: String,
        message: String,
        attachmentLabel: String?
    ): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Sign in required."))
        val ticketId = "sup_${uid}_${System.currentTimeMillis()}"
        return try {
            assertAppNotStopped()
            require(subject.trim().length in 3..160)
            require(message.trim().length in 5..2000)
            val payload = mapOf(
                "userId" to uid,
                "fullName" to user.fullName.take(100),
                "username" to user.username,
                "category" to category.name,
                "subject" to subject.trim(),
                "message" to message.trim(),
                "attachmentLabel" to attachmentLabel?.take(200),
                "status" to TicketStatus.OPEN.name,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            db.collection("support_tickets").document(ticketId).set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "support_tickets/$ticketId")
            Result.failure(e)
        }
    }

    suspend fun updateProfileInfo(fullName: String, username: String): Result<Unit> {
        val uid = requireUserId()
        return try {
            val cleanName = fullName.trim()
            val cleanUser = username.trim()
            require(cleanName.length in 2..100 && cleanUser.length in 3..40)
            val batch = db.batch()
            batch.update(
                db.collection("users").document(uid),
                mapOf(
                    "fullName" to cleanName,
                    "username" to cleanUser,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                db.collection("leaderboard_public").document(uid),
                mapOf(
                    "displayName" to cleanName,
                    "username" to cleanUser,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun updatePaymentAccounts(bkashRaw: String, nagadRaw: String): Result<Unit> {
        val uid = requireUserId()
        val bkash = AdminSecurity.normalizeBdMobile(bkashRaw)
        val nagad = AdminSecurity.normalizeBdMobile(nagadRaw)
        return try {
            require(AdminSecurity.isValidBdMobile(bkash) && AdminSecurity.isValidBdMobile(nagad))
            val userRef = db.collection("users").document(uid)
            val bkashRegRef = db.collection("wallet_registry").document(bkash)
            val nagadRegRef = db.collection("wallet_registry").document(nagad)

            db.runTransaction { transaction ->
                val userSnap = transaction.get(userRef)
                val alreadyLocked = userSnap.getBoolean("paymentAccountsLocked") == true
                if (alreadyLocked) {
                    throw IllegalStateException("PAYMENT_ACCOUNTS_LOCKED")
                }

                val oldBkash = userSnap.getString("bkashPersonal").orEmpty().trim()
                val oldNagad = userSnap.getString("nagadPersonal").orEmpty().trim()
                val wasResetApproved = userSnap.getBoolean("walletChangeApprovedOnce") == true
                val currentResetCount = userSnap.getLong("walletResetCount") ?: 0L

                val bkashSnap = transaction.get(bkashRegRef)
                val nagadSnap = if (nagad != bkash) transaction.get(nagadRegRef) else bkashSnap

                val oldBkashRef = if (AdminSecurity.isValidBdMobile(oldBkash) && oldBkash != bkash && oldBkash != nagad) {
                    db.collection("wallet_registry").document(oldBkash)
                } else null
                val oldBkashSnap = oldBkashRef?.let { transaction.get(it) }

                val oldNagadRef = if (AdminSecurity.isValidBdMobile(oldNagad) && oldNagad != bkash && oldNagad != nagad && oldNagad != oldBkash) {
                    db.collection("wallet_registry").document(oldNagad)
                } else null
                val oldNagadSnap = oldNagadRef?.let { transaction.get(it) }

                if (bkashSnap.exists() && bkashSnap.getString("userId") != uid) {
                    throw IllegalStateException("DUPLICATE_WALLET_NUMBER")
                }
                if (nagadSnap.exists() && nagadSnap.getString("userId") != uid) {
                    throw IllegalStateException("DUPLICATE_WALLET_NUMBER")
                }

                if (oldBkashRef != null && oldBkashSnap?.exists() == true && oldBkashSnap.getString("userId") == uid) {
                    transaction.delete(oldBkashRef)
                }
                if (oldNagadRef != null && oldNagadSnap?.exists() == true && oldNagadSnap.getString("userId") == uid) {
                    transaction.delete(oldNagadRef)
                }

                val registryPayload = mapOf(
                    "userId" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
                transaction.set(bkashRegRef, registryPayload)
                if (nagad != bkash) {
                    transaction.set(nagadRegRef, registryPayload)
                }

                transaction.update(
                    userRef,
                    mapOf(
                        "bkashPersonal" to bkash,
                        "nagadPersonal" to nagad,
                        "paymentAccountsLocked" to true,
                        "walletChangeApprovedOnce" to false,
                        "walletResetCount" to if (wasResetApproved) currentResetCount + 1L else currentResetCount,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun changeSecurityPassword(oldPassword: String, newPassword: String): Result<Unit> {
        val uid = requireUserId()
        val cleanNew = newPassword.trim()
        return try {
            require(cleanNew.length in 6..64) { "INVALID_NEW_PASSWORD" }
            val doc = db.collection("users").document(uid).get().await()
            val existingHash = doc.getString("securityPasswordHash").orEmpty()
            if (existingHash.isNotBlank() && !AdminSecurity.verifyUserPassword(uid, oldPassword, existingHash)) {
                return Result.failure(IllegalStateException("WRONG_OLD_PASSWORD"))
            }
            val newHash = AdminSecurity.hashUserPassword(uid, cleanNew)
            db.collection("users").document(uid).update(
                mapOf(
                    "securityPasswordHash" to newHash,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun updateSecurityToggles(twoFactor: Boolean, loginAlerts: Boolean): Result<Unit> {
        val uid = requireUserId()
        return try {
            db.collection("users").document(uid).update(
                mapOf(
                    "twoFactorEnabled" to twoFactor,
                    "loginAlertsEnabled" to loginAlerts,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    private fun logStreamErrorIfActive(error: Throwable, op: OperationType, path: String, expectedUid: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val toLog = firestoreError ?: (error as? Exception)
        if (toLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(toLog, op, path)
        }
    }

    companion object {
        fun getDefaultFaqs(): List<FaqItem> = listOf(
            FaqItem(
                question = LocalizedText("REAL REWARD কী এবং কীভাবে শুরু করব?", "What is REAL REWARD and how do I start?"),
                answer = LocalizedText(
                    "Google অ্যাকাউন্ট দিয়ে সাইন-ইন করে আপনার বাংলাদেশি মোবাইল নম্বর দিয়ে একবার নিবন্ধন করুন। প্রথমবার নিবন্ধনে সাথে সাথে ৳১০ বোনাস এবং প্রতিটি বৈধ রেফারেলে ৳১৫ বোনাস পাবেন।",
                    "Sign in with Google and register once using your Bangladesh mobile number for an instant ৳10 signup bonus plus ৳15 for every verified referral."
                )
            ),
            FaqItem(
                question = LocalizedText("একই মোবাইল বা ডিভাইসে কি একাধিক অ্যাকাউন্ট খোলা যাবে?", "Can I open multiple accounts on one mobile or device?"),
                answer = LocalizedText(
                    "না। প্ল্যাটফর্মের নিরাপত্তার জন্য প্রতিটি মোবাইল নম্বর ও ডিভাইসে শুধুমাত্র একটি অ্যাকাউন্ট অনুমোদিত।",
                    "No. To protect against spam and fraud, strictly one account is allowed per mobile number and device."
                )
            ),
            FaqItem(
                question = LocalizedText("উত্তোলনের সর্বনিম্ন সীমা ও খরচ কত?", "What is the minimum withdrawal and fee?"),
                answer = LocalizedText(
                    "সর্বনিম্ন উত্তোলন ৳৩০০। প্রতি উত্তোলনে ৳১০ অতিরিক্ত প্রসেসিং ফি কাটা হয়, অর্থাৎ ৳৩০০ উত্তোলন করতে উপলব্ধ ব্যালেন্স অন্তত ৳৩১০ থাকতে হবে।",
                    "Minimum withdrawal is ৳300 plus a ৳10 processing fee, so at least ৳310 available balance is required to withdraw ৳300."
                )
            )
        )
    }
}
