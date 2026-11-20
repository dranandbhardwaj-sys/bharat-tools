package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.LocalPreferencesStore
import com.example.data.model.Announcement
import com.example.data.model.AppConfig
import com.example.data.model.PurchaseRecord
import com.example.data.model.SupportTicket
import com.example.data.model.ToolCategory
import com.example.data.model.ToolHistory
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.repository.BharatToolsRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
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

class MainViewModel(
    private val repository: BharatToolsRepository,
    private val currentUserId: String,
    private val localPrefs: LocalPreferencesStore? = null
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<ToolCategory?>(null)
    val selectedCategory: StateFlow<ToolCategory?> = _selectedCategory.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    val recentSearches: StateFlow<List<String>> = (localPrefs?.recentSearches ?: kotlinx.coroutines.flow.flowOf(emptyList()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val profileState: StateFlow<UiState<UserProfile>> = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile>> { profile ->
            UiState.Success(profile ?: UserProfile(userId = currentUserId, displayName = "Bharat User"))
        }
        .unwrapFirestoreErrors("profile")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val historyState: StateFlow<UiState<List<ToolHistory>>> = repository.observeToolHistory(currentUserId)
        .map<List<ToolHistory>, UiState<List<ToolHistory>>> { UiState.Success(it) }
        .unwrapFirestoreErrors("history")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val purchasesState: StateFlow<UiState<List<PurchaseRecord>>> = repository.observeUserPurchases(currentUserId)
        .map<List<PurchaseRecord>, UiState<List<PurchaseRecord>>> { UiState.Success(it) }
        .unwrapFirestoreErrors("purchases")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val ticketsState: StateFlow<UiState<List<SupportTicket>>> = repository.observeUserTickets(currentUserId)
        .map<List<SupportTicket>, UiState<List<SupportTicket>>> { UiState.Success(it) }
        .unwrapFirestoreErrors("tickets")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val announcementsState: StateFlow<UiState<List<Announcement>>> = repository.observeActiveAnnouncements(currentUserId)
        .map<List<Announcement>, UiState<List<Announcement>>> { UiState.Success(it) }
        .unwrapFirestoreErrors("announcements")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val configState: StateFlow<AppConfig> = repository.observeAppConfig(currentUserId)
        .map { it ?: AppConfig() }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(AppConfig())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AppConfig())

    init {
        viewModelScope.launch {
            Firebase.auth.currentUser?.let { user ->
                repository.ensureUserProfile(user)
                _isAdmin.value = repository.checkIsAdmin(user)
            }
        }
    }

    fun updateSearch(q: String) {
        _searchQuery.value = q
    }

    fun selectCategory(cat: ToolCategory?) {
        _selectedCategory.value = cat
    }

    fun recordRecentSearch(query: String) {
        viewModelScope.launch {
            localPrefs?.addSearch(query)
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            localPrefs?.clearSearches()
        }
    }

    fun clearToolHistory() {
        viewModelScope.launch {
            val res = repository.clearToolHistory()
            _actionMessage.value = if (res.isSuccess) {
                "Tool history cleared."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to clear history"
            }
        }
    }

    fun clearMessage() {
        _actionMessage.value = null
    }

    fun setLanguage(lang: String) {
        viewModelScope.launch {
            repository.updatePreferences(language = lang).onFailure {
                _actionMessage.value = it.localizedMessage ?: "Failed to update language"
            }
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            repository.updatePreferences(themeMode = mode).onFailure {
                _actionMessage.value = it.localizedMessage ?: "Failed to update theme"
            }
        }
    }

    fun setAutoRenewConsent(enabled: Boolean) {
        viewModelScope.launch {
            repository.updatePreferences(autoRenewConsent = enabled).onFailure {
                _actionMessage.value = it.localizedMessage ?: "Failed to update renewal consent"
            }
        }
    }

    fun toggleFavorite(toolId: String) {
        val current = (profileState.value as? UiState.Success)?.data?.favorites ?: emptyList()
        viewModelScope.launch {
            repository.toggleFavorite(toolId, current).onFailure {
                _actionMessage.value = it.localizedMessage ?: "Failed to update favorite"
            }
        }
    }

    fun recordToolRun(toolId: String, title: String, summary: String) {
        val current = (profileState.value as? UiState.Success)?.data?.recentTools ?: emptyList()
        viewModelScope.launch {
            repository.recordRecentTool(toolId, current, title, summary)
        }
    }

    fun submitSubscriptionVerification(method: String, referenceId: String, consentAccepted: Boolean, notes: String) {
        viewModelScope.launch {
            val res = repository.submitSubscriptionVerification(method, referenceId, consentAccepted, notes)
            _actionMessage.value = if (res.isSuccess) {
                "Subscription request submitted! Access activates only after server verification of your ₹399/month payment."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to submit subscription verification"
            }
        }
    }

    fun cancelActiveSubscription(activePurchaseId: String?) {
        viewModelScope.launch {
            val res = repository.cancelSubscription(activePurchaseId)
            _actionMessage.value = if (res.isSuccess) {
                "Your ₹399/month recurring subscription has been cancelled."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to cancel subscription"
            }
        }
    }

    fun requestRefund(purchaseId: String, reason: String) {
        viewModelScope.launch {
            val res = repository.requestPurchaseRefund(purchaseId, reason)
            _actionMessage.value = if (res.isSuccess) {
                "Refund request submitted under the disclosed monthly refund policy."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to request refund"
            }
        }
    }

    fun submitSupportTicket(category: String, subject: String, message: String) {
        viewModelScope.launch {
            val res = repository.createSupportTicket(category, subject, message)
            _actionMessage.value = if (res.isSuccess) {
                "Support ticket created successfully."
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Failed to submit support ticket"
            }
        }
    }

    fun applyReferralCode(code: String) {
        viewModelScope.launch {
            val res = repository.applyReferralCode(code)
            _actionMessage.value = if (res.isSuccess) {
                "Referral attribution saved!"
            } else {
                res.exceptionOrNull()?.localizedMessage ?: "Invalid referral code"
            }
        }
    }

    private fun <T> Flow<UiState<T>>.unwrapFirestoreErrors(tag: String): Flow<UiState<T>> =
        this.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w("MainViewModel", "Error observing $tag", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load $tag"))
        }
}
