package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AdminFirestoreRepository
import com.example.domain.model.*
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class AdminViewModel(
    private val adminRepository: AdminFirestoreRepository
) : ViewModel() {

    private val retryTrigger = MutableStateFlow(0)

    val allUsersState: StateFlow<UiState<List<UserAccountDoc>>> = retryTrigger.flatMapLatest {
        adminRepository.observeAllUsers()
            .map<List<UserAccountDoc>, UiState<List<UserAccountDoc>>> { UiState.Success(it) }
            .catch { emit(unwrapError("allUsers", it)) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    val fraudAlertsState: StateFlow<UiState<List<FraudAlertDoc>>> = retryTrigger.flatMapLatest {
        adminRepository.observeFraudAlerts()
            .map<List<FraudAlertDoc>, UiState<List<FraudAlertDoc>>> { UiState.Success(it) }
            .catch { emit(unwrapError("fraudAlerts", it)) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    val allReferralsState: StateFlow<UiState<List<ReferralRecordDoc>>> = retryTrigger.flatMapLatest {
        adminRepository.observeAllReferrals()
            .map<List<ReferralRecordDoc>, UiState<List<ReferralRecordDoc>>> { UiState.Success(it) }
            .catch { emit(unwrapError("allReferrals", it)) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    val allTasksState: StateFlow<UiState<List<MicroTaskDoc>>> = retryTrigger.flatMapLatest {
        adminRepository.observeAllTasks()
            .map<List<MicroTaskDoc>, UiState<List<MicroTaskDoc>>> { UiState.Success(it) }
            .catch { emit(unwrapError("allTasks", it)) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    val allProofsState: StateFlow<UiState<List<TaskProofDoc>>> = retryTrigger.flatMapLatest {
        adminRepository.observeAllProofs()
            .map<List<TaskProofDoc>, UiState<List<TaskProofDoc>>> { UiState.Success(it) }
            .catch { emit(unwrapError("allProofs", it)) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    val allWithdrawalsState: StateFlow<UiState<List<WithdrawalDoc>>> = retryTrigger.flatMapLatest {
        adminRepository.observeAllWithdrawals()
            .map<List<WithdrawalDoc>, UiState<List<WithdrawalDoc>>> { UiState.Success(it) }
            .catch { emit(unwrapError("allWithdrawals", it)) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    val allVipRequestsState: StateFlow<UiState<List<VipRequestDoc>>> = retryTrigger.flatMapLatest {
        adminRepository.observeAllVipRequests()
            .map<List<VipRequestDoc>, UiState<List<VipRequestDoc>>> { UiState.Success(it) }
            .catch { emit(unwrapError("allVipRequests", it)) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    val allSupportTicketsState: StateFlow<UiState<List<SupportTicketDoc>>> = retryTrigger.flatMapLatest {
        adminRepository.observeAllSupportTickets()
            .map<List<SupportTicketDoc>, UiState<List<SupportTicketDoc>>> { UiState.Success(it) }
            .catch { emit(unwrapError("allSupportTickets", it)) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    val announcementsState: StateFlow<UiState<List<AnnouncementDoc>>> = retryTrigger.flatMapLatest {
        adminRepository.observeAnnouncements()
            .map<List<AnnouncementDoc>, UiState<List<AnnouncementDoc>>> { UiState.Success(it) }
            .catch { emit(unwrapError("announcements", it)) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    val appConfigState: StateFlow<AppConfigDoc> = retryTrigger.flatMapLatest {
        adminRepository.observeAppConfig()
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                Log.w(TAG, "Admin config error", firestoreError ?: error)
                emit(AppConfigDoc())
            }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppConfigDoc())

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    fun retry() {
        retryTrigger.value += 1
    }

    fun toggleUserBan(
        userId: String,
        ban: Boolean,
        permanent: Boolean = true,
        reason: String? = null,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = adminRepository.toggleUserBan(userId, ban, permanent, reason)
            _isBusy.value = false
            onResult(res)
        }
    }

    fun allowUserWalletChangeOnce(userId: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = adminRepository.allowUserWalletChangeOnce(userId)
            _isBusy.value = false
            onResult(res)
        }
    }

    fun permanentlyRemoveUser(
        user: UserAccountDoc,
        reason: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = adminRepository.permanentlyRemoveUserAccount(user, reason)
            _isBusy.value = false
            onResult(res)
        }
    }

    fun loadUserFullHistory(
        user: UserAccountDoc,
        onResult: (Result<UserFullHistory>) -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = adminRepository.loadUserFullHistory(user)
            _isBusy.value = false
            onResult(res)
        }
    }

    fun resolveFraudAlert(alertId: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = adminRepository.resolveFraudAlert(alertId)
            _isBusy.value = false
            onResult(res)
        }
    }

    fun setUserVipTier(userId: String, tier: VipTierCode, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = adminRepository.setUserVipTier(userId, tier)
            _isBusy.value = false
            onResult(res)
        }
    }

    fun adjustUserBalance(user: UserAccountDoc, deltaBdt: Double, reason: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = adminRepository.adjustUserBalance(user, deltaBdt, reason)
            _isBusy.value = false
            onResult(res)
        }
    }

    fun createTask(
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
        verificationCode: String = "",
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = adminRepository.createTask(
                titleBn, titleEn, category, rewardBdt,
                descriptionBn, descriptionEn, instructionsBn, instructionsEn,
                taskLink, deadlineText, totalSlots, featured,
                instantCredit, verificationCode
            )
            _isBusy.value = false
            onResult(res)
        }
    }

    fun toggleTaskActive(taskId: String, active: Boolean, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = adminRepository.toggleTaskActive(taskId, active)
            _isBusy.value = false
            onResult(res)
        }
    }

    fun deleteTask(taskId: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = adminRepository.deleteTask(taskId)
            _isBusy.value = false
            onResult(res)
        }
    }

    fun reviewProof(proof: TaskProofDoc, approve: Boolean, note: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = adminRepository.reviewProof(proof, approve, note)
            _isBusy.value = false
            onResult(res)
        }
    }

    fun updateWithdrawalStage(
        record: WithdrawalDoc,
        nextStatus: TransactionStatus,
        note: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = adminRepository.updateWithdrawalStage(record, nextStatus, note)
            _isBusy.value = false
            onResult(res)
        }
    }

    fun deleteWithdrawal(withdrawalId: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = adminRepository.deleteWithdrawalRecord(withdrawalId)
            _isBusy.value = false
            onResult(res)
        }
    }

    fun deleteCompletedWithdrawals(withdrawalIds: List<String>, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = adminRepository.deleteCompletedWithdrawals(withdrawalIds)
            _isBusy.value = false
            onResult(res)
        }
    }

    fun reviewVipRequest(req: VipRequestDoc, approve: Boolean, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = adminRepository.reviewVipRequest(req, approve)
            _isBusy.value = false
            onResult(res)
        }
    }

    fun saveFullAppConfig(
        config: AppConfigDoc,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = adminRepository.saveFullAppConfig(config)
            _isBusy.value = false
            onResult(res)
        }
    }

    fun broadcastAnnouncement(
        category: NotificationCategory,
        titleBn: String,
        titleEn: String,
        messageBn: String,
        messageEn: String,
        appVersion: String?,
        updateUrl: String?,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = adminRepository.broadcastAnnouncement(
                category, titleBn, titleEn, messageBn, messageEn, appVersion, updateUrl
            )
            _isBusy.value = false
            onResult(res)
        }
    }

    fun updateSupportTicket(ticketId: String, status: TicketStatus, reply: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = adminRepository.updateSupportTicket(ticketId, status, reply)
            _isBusy.value = false
            onResult(res)
        }
    }

    fun deleteSupportTickets(ticketIds: List<String>, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = adminRepository.deleteSupportTickets(ticketIds)
            _isBusy.value = false
            onResult(res)
        }
    }

    private fun unwrapError(name: String, error: Throwable): UiState.Error {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Admin stream error ($name)", rootError)
        return UiState.Error(rootError.localizedMessage ?: "Admin sync failed ($name)")
    }

    private companion object {
        const val TAG = "AdminViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
