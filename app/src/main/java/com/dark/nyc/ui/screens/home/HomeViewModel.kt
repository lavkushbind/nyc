package com.dark.nyc.ui.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dark.nyc.data.models.NYCUser
import com.dark.nyc.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepo: UserRepository
) : ViewModel() {

    // ===== UI STATE =====
    data class HomeUiState(
        val currentUser: NYCUser? = null,
        val potentialMatches: List<NYCUser> = emptyList(),
        val isLoading: Boolean = true,
        val isSwiping: Boolean = false,
        val error: String? = null,
        val matchFound: Boolean = false,
        val matchedUser: NYCUser? = null
    )

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Swipe Stack Management
    private val _swipeQueue = MutableStateFlow<List<NYCUser>>(emptyList())
    private var lastDocument: com.google.firebase.firestore.DocumentSnapshot? = null
    private var isLoadingMore = false

    init {
        loadCurrentUser()
    }

    // 1️⃣ Load Current User (Instant from Cache)
    private fun loadCurrentUser() {
        viewModelScope.launch {
            userRepo.getCurrentUser().collect { user ->
                _uiState.update { it.copy(currentUser = user) }
                if (user != null) {
                    // Load matches after getting user
                    loadPotentialMatches()
                }
            }
        }
    }

    // 2️⃣ Load Potential Matches (PUBLIC so HomeScreen can call it)
    fun loadPotentialMatches() {
        if (isLoadingMore) return
        isLoadingMore = true

        viewModelScope.launch {
            try {
                val currentUserId = _uiState.value.currentUser?.uid ?: return@launch

                val (users, lastDoc) = userRepo.getPotentialMatches(
                    currentUserId = currentUserId,
                    limit = 10,
                    lastDoc = lastDocument
                )

                lastDocument = lastDoc
                val existing = _swipeQueue.value.toMutableList()
                val newUsers = users.filter { user ->
                    existing.none { it.uid == user.uid }
                }

                _swipeQueue.value = existing + newUsers

                _uiState.update {
                    it.copy(
                        potentialMatches = _swipeQueue.value,
                        isLoading = false,
                        error = null
                    )
                }

                isLoadingMore = false

                // Preload more if stack is low
                if (_swipeQueue.value.size < 3) {
                    loadPotentialMatches()
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load: ${e.message}"
                    )
                }
                isLoadingMore = false
            }
        }
    }

    // Helper to refresh from start when stack is empty
    fun refreshMatches() {
        lastDocument = null
        _swipeQueue.value = emptyList()
        isLoadingMore = false
        _uiState.update { it.copy(isLoading = true, error = null) }
        loadPotentialMatches()
    }

    // 3️⃣ Swipe Right (Like)
    fun onSwipeRight(user: NYCUser) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSwiping = true) }
            removeUserFromQueue(user.uid)

            val currentUserId = _uiState.value.currentUser?.uid ?: return@launch

            // Save Swipe
            userRepo.saveSwipe(currentUserId, user.uid, isLike = true)

            // Check for Match
            val isMatch = userRepo.checkMatch(currentUserId, user.uid)
            if (isMatch) {
                // Create Match Document
                userRepo.createMatch(currentUserId, user.uid)
                _uiState.update {
                    it.copy(
                        matchFound = true,
                        matchedUser = user,
                        isSwiping = false
                    )
                }
                // Auto-dismiss match popup after 3s
                kotlinx.coroutines.delay(3000)
                _uiState.update {
                    it.copy(
                        matchFound = false,
                        matchedUser = null
                    )
                }
            } else {
                _uiState.update { it.copy(isSwiping = false) }
            }

            // Load more if needed
            if (_swipeQueue.value.size < 3) {
                loadPotentialMatches()
            }
        }
    }

    // 4️⃣ Swipe Left (Pass)
    fun onSwipeLeft(user: NYCUser) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSwiping = true) }
            removeUserFromQueue(user.uid)

            val currentUserId = _uiState.value.currentUser?.uid ?: return@launch
            userRepo.saveSwipe(currentUserId, user.uid, isLike = false)

            _uiState.update { it.copy(isSwiping = false) }

            // Load more if needed
            if (_swipeQueue.value.size < 3) {
                loadPotentialMatches()
            }
        }
    }

    // 5️⃣ Remove User from Queue
    private fun removeUserFromQueue(uid: String) {
        val updated = _swipeQueue.value.filter { it.uid != uid }
        _swipeQueue.value = updated
        _uiState.update {
            it.copy(potentialMatches = updated)
        }
    }

    // 6️⃣ Dismiss Match Dialog
    fun dismissMatch() {
        _uiState.update {
            it.copy(matchFound = false, matchedUser = null)
        }
    }

    // 7️⃣ Direct Message (Send Request)
    fun sendDirectMessage(targetUser: NYCUser) {
        viewModelScope.launch {
            Log.d("HomeVM", "Sending message request to: ${targetUser.name}")
        }
    }
}