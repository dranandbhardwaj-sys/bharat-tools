package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.PurchaseRecord
import com.example.data.model.ReferralAttribution
import com.example.data.model.SupportTicket
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.repository.BharatToolsRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminViewModel(
    private val repository: BharatToolsRepository,
    currentUserId: String
) : ViewModel() {

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    val usersState: StateFlow<UiState<List<UserProfile>>> = repository.observeAllUsers(currentUserId)
        .map<List<UserProfile>, UiState<List<UserProfile>>> { UiState.Success(it) }
        .unwrap("users")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val purchasesState: StateFlow<UiState<List<PurchaseRecord>>> = repository.observeAllPurchases(currentUserId)
        .map<List<PurchaseRecord>, UiState<List<PurchaseRecord>>> { UiState.Success(it) }
        .unwrap("purchases")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val ticketsState: StateFlow<UiState<List<SupportTicket>>> = repository.observeAllTickets(currentUserId)
        .map<List<SupportTicket>, UiState<List<SupportTicket>>> { UiState.Success(it) }
        .unwrap("tickets")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val referralsState: StateFlow<UiState<List<ReferralAttribution>>> = repository.observeAllReferrals(currentUserId)
        .map<List<ReferralAttribution>, UiState<List<ReferralAttribution>>> { UiState.Success(it) }
        .unwrap("referrals")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    fun clearBanner() {
        _statusBanner.value = null
    }

    fun updateSubscriptionStatus(purchaseId: String, targetUid: String, newStatus: String) {
        viewModelScope.launch {
            val res = repository.adminUpdateSubscriptionStatus(purchaseId, targetUid, newStatus)
            _statusBanner.value = if (res.isSuccess) {
                "Subscription updated to ${newStatus.uppercase()}."
            } else {
                res.exceptionOrNull()?.localizedMessage
            }
        }
    }

    fun replyTicket(ticketId: String, status: String, reply: String) {
        viewModelScope.launch {
            val res = repository.adminReplyTicket(ticketId, status, reply)
            _statusBanner.value = if (res.isSuccess) "Ticket reply saved." else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun publishAnnouncement(title: String, titleHi: String, body: String, bodyHi: String) {
        viewModelScope.launch {
            val res = repository.adminPublishAnnouncement(title, titleHi, body, bodyHi)
            _statusBanner.value = if (res.isSuccess) "Announcement published!" else res.exceptionOrNull()?.localizedMessage
        }
    }

    fun saveAppConfig(trialLimit: Int, maintenance: Boolean, supportEmail: String) {
        viewModelScope.launch {
            val res = repository.adminSaveConfig(trialLimit, maintenance, supportEmail)
            _statusBanner.value = if (res.isSuccess) "App config saved." else res.exceptionOrNull()?.localizedMessage
        }
    }

    private fun <T> Flow<UiState<T>>.unwrap(tag: String): Flow<UiState<T>> =
        this.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w("AdminViewModel", "Error observing $tag", rootError)
            emit(UiState.Error(rootError.message ?: "Access denied or failed loading $tag"))
        }
}
