package com.example.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.security.NetworkSecurityMonitor
import com.example.core.security.NetworkSecuritySnapshot
import com.example.data.repository.RealRewardFirestoreRepository
import com.example.domain.model.*
import com.example.ui.i18n.LanguagePreferenceStore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val repository: RealRewardFirestoreRepository,
    private val languageStore: LanguagePreferenceStore,
    private val currentUserId: String
) : ViewModel() {

    private val retryTrigger = MutableStateFlow(0)

    val language: StateFlow<AppLanguage> = languageStore.languageFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppLanguage.BANGLA)

    val notificationSoundEnabled: StateFlow<Boolean> = languageStore.notificationSoundEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val userAccountState: StateFlow<UiState<UserAccountDoc?>> = retryTrigger.flatMapLatest {
        repository.observeUserAccount(currentUserId)
            .map<UserAccountDoc?, UiState<UserAccountDoc?>> { UiState.Success(it) }
            .catch { error -> emit(handleStreamError("userAccount", error)) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    val tasksState: StateFlow<UiState<List<MicroTaskDoc>>> = retryTrigger.flatMapLatest {
        repository.observeActiveTasks()
            .map<List<MicroTaskDoc>, UiState<List<MicroTaskDoc>>> { UiState.Success(it) }
            .catch { error -> emit(handleStreamError("tasks", error)) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    val myProofsState: StateFlow<UiState<List<TaskProofDoc>>> = retryTrigger.flatMapLatest {
        repository.observeMyProofs(currentUserId)
            .map<List<TaskProofDoc>, UiState<List<TaskProofDoc>>> { UiState.Success(it) }
            .catch { error -> emit(handleStreamError("myProofs", error)) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    val myTransactionsState: StateFlow<UiState<List<TransactionDoc>>> = retryTrigger.flatMapLatest {
        repository.observeMyTransactions(currentUserId)
            .map<List<TransactionDoc>, UiState<List<TransactionDoc>>> { UiState.Success(it) }
            .catch { error -> emit(handleStreamError("myTransactions", error)) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    val myWithdrawalsState: StateFlow<UiState<List<WithdrawalDoc>>> = retryTrigger.flatMapLatest {
        repository.observeMyWithdrawals(currentUserId)
            .map<List<WithdrawalDoc>, UiState<List<WithdrawalDoc>>> { UiState.Success(it) }
            .catch { error -> emit(handleStreamError("myWithdrawals", error)) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    val myReferralsState: StateFlow<UiState<List<ReferralRecordDoc>>> = retryTrigger.flatMapLatest {
        repository.observeMyReferrals(currentUserId)
            .map<List<ReferralRecordDoc>, UiState<List<ReferralRecordDoc>>> { UiState.Success(it) }
            .catch { error -> emit(handleStreamError("myReferrals", error)) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    val myVipRequestsState: StateFlow<UiState<List<VipRequestDoc>>> = retryTrigger.flatMapLatest {
        repository.observeMyVipRequests(currentUserId)
            .map<List<VipRequestDoc>, UiState<List<VipRequestDoc>>> { UiState.Success(it) }
            .catch { error -> emit(handleStreamError("myVipRequests", error)) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    val mySupportTicketsState: StateFlow<UiState<List<SupportTicketDoc>>> = retryTrigger.flatMapLatest {
        repository.observeMySupportTickets(currentUserId)
            .map<List<SupportTicketDoc>, UiState<List<SupportTicketDoc>>> { UiState.Success(it) }
            .catch { error -> emit(handleStreamError("mySupportTickets", error)) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    val announcementsState: StateFlow<UiState<List<AnnouncementDoc>>> = retryTrigger.flatMapLatest {
        repository.observeAnnouncements()
            .map<List<AnnouncementDoc>, UiState<List<AnnouncementDoc>>> { UiState.Success(it) }
            .catch { error -> emit(handleStreamError("announcements", error)) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    val leaderboardState: StateFlow<UiState<List<LeaderboardPublicDoc>>> = retryTrigger.flatMapLatest {
        repository.observeLeaderboard()
            .map<List<LeaderboardPublicDoc>, UiState<List<LeaderboardPublicDoc>>> { UiState.Success(it) }
            .catch { error -> emit(handleStreamError("leaderboard", error)) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    private val _isAppConfigReady = MutableStateFlow(false)
    val isAppConfigReady: StateFlow<Boolean> = _isAppConfigReady.asStateFlow()

    val paymentConfigState: StateFlow<AppConfigDoc> = retryTrigger.flatMapLatest {
        repository.observePaymentConfig()
            .onEach { _isAppConfigReady.value = true }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                Log.w(TAG, "Payment config stream error", firestoreError ?: error)
                _isAppConfigReady.value = true
                emit(AppConfigDoc())
            }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppConfigDoc())

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    val faqs: List<FaqItem> = RealRewardFirestoreRepository.getDefaultFaqs()

    fun toggleLanguage() {
        viewModelScope.launch {
            val next = if (language.value == AppLanguage.BANGLA) AppLanguage.ENGLISH else AppLanguage.BANGLA
            languageStore.setLanguage(next)
        }
    }

    fun setNotificationSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            languageStore.setNotificationSoundEnabled(enabled)
        }
    }

    fun retryAllStreams() {
        retryTrigger.value += 1
    }

    fun syncLeaderboardIfNeeded(user: UserAccountDoc) {
        viewModelScope.launch {
            repository.syncLeaderboardFromUserIfNeeded(user)
        }
    }

    suspend fun checkRemovedAccount(uid: String, email: String): RemovedAccountDoc? {
        return repository.checkRemovedAccount(uid, email)
    }

    suspend fun checkDeviceRegistryOwner(deviceId: String): String? {
        return repository.checkDeviceRegistryOwner(deviceId)
    }

    fun inspectAndEnforceNetworkSecurity(
        context: Context,
        user: UserAccountDoc,
        config: AppConfigDoc,
        isOwner: Boolean
    ) {
        viewModelScope.launch {
            val snapshot = NetworkSecurityMonitor.inspectNetwork(context)
            repository.recordNetworkCheckAndAutoBanIfNeeded(user, snapshot, config, isOwner)
        }
    }

    fun registerNewAccount(
        context: Context,
        fullName: String,
        username: String,
        mobileRaw: String,
        deviceId: String,
        referralCodeRaw: String,
        passwordRaw: String = "",
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val snapshot: NetworkSecuritySnapshot = NetworkSecurityMonitor.inspectNetwork(context)
            val res = repository.registerNewAccount(
                fullName = fullName,
                username = username,
                mobileRaw = mobileRaw,
                deviceId = deviceId,
                referralCodeRaw = referralCodeRaw,
                passwordRaw = passwordRaw,
                ipAddress = snapshot.ipAddress,
                countryCode = snapshot.countryCode,
                locationLabel = snapshot.locationLabel,
                vpnDetected = snapshot.vpnDetected
            )
            _isSubmitting.value = false
            onResult(res)
        }
    }

    fun submitTaskProof(
        task: MicroTaskDoc,
        user: UserAccountDoc,
        proofNote: String,
        attachmentLabel: String?,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = repository.submitTaskProof(task, user, proofNote, attachmentLabel)
            _isSubmitting.value = false
            onResult(res)
        }
    }

    fun submitWithdrawalRequest(
        user: UserAccountDoc,
        method: WithdrawalMethod,
        accountRaw: String,
        amountBdt: Double,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = repository.submitWithdrawalRequest(user, method, accountRaw, amountBdt)
            _isSubmitting.value = false
            onResult(res)
        }
    }

    fun submitVipUpgradeRequest(
        user: UserAccountDoc,
        targetTier: VipTierCode,
        method: WithdrawalMethod,
        senderNumberRaw: String,
        trxId: String,
        amountBdt: Double,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = repository.submitVipUpgradeRequest(user, targetTier, method, senderNumberRaw, trxId, amountBdt)
            _isSubmitting.value = false
            onResult(res)
        }
    }

    fun submitSupportTicket(
        user: UserAccountDoc,
        category: SupportCategory,
        subject: String,
        message: String,
        attachmentLabel: String?,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = repository.submitSupportTicket(user, category, subject, message, attachmentLabel)
            _isSubmitting.value = false
            onResult(res)
        }
    }

    fun updateProfileInfo(
        fullName: String,
        username: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = repository.updateProfileInfo(fullName, username)
            _isSubmitting.value = false
            onResult(res)
        }
    }

    fun updatePaymentAccounts(
        bkashRaw: String,
        nagadRaw: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = repository.updatePaymentAccounts(bkashRaw, nagadRaw)
            _isSubmitting.value = false
            onResult(res)
        }
    }

    fun changeSecurityPassword(
        oldPassword: String,
        newPassword: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            val res = repository.changeSecurityPassword(oldPassword, newPassword)
            _isSubmitting.value = false
            onResult(res)
        }
    }

    fun updateSecurityToggles(
        twoFactor: Boolean,
        loginAlerts: Boolean,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.updateSecurityToggles(twoFactor, loginAlerts)
            onResult(res)
        }
    }

    private fun handleStreamError(streamName: String, error: Throwable): UiState.Error {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing $streamName", rootError)
        return UiState.Error(rootError.localizedMessage ?: "Failed to sync $streamName")
    }

    private companion object {
        const val TAG = "MainViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
