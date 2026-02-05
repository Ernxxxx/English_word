package com.example.englishword.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.englishword.billing.PremiumManager
import com.example.englishword.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for Onboarding screen
 */
data class OnboardingUiState(
    val currentPage: Int = 0,
    val isLoading: Boolean = true,
    val shouldNavigateToHome: Boolean = false,
    val isOnboardingNeeded: Boolean = true
)

/**
 * ViewModel for the Onboarding screen
 * Manages onboarding state and persistence
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val premiumManager: PremiumManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    val totalPages = OnboardingPages.pages.size

    init {
        checkOnboardingStatus()
    }

    /**
     * Check if onboarding has been completed
     */
    private fun checkOnboardingStatus() {
        viewModelScope.launch {
            try {
                val isCompleted = settingsRepository.isOnboardingCompleted().first()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isOnboardingNeeded = !isCompleted,
                    shouldNavigateToHome = isCompleted
                )
            } catch (e: Exception) {
                // If there's an error, show onboarding
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isOnboardingNeeded = true
                )
            }
        }
    }

    /**
     * Navigate to the next page
     */
    fun nextPage() {
        val currentPage = _uiState.value.currentPage
        if (currentPage < totalPages - 1) {
            _uiState.value = _uiState.value.copy(currentPage = currentPage + 1)
        } else {
            completeOnboarding()
        }
    }

    /**
     * Navigate to a specific page
     */
    fun goToPage(page: Int) {
        if (page in 0 until totalPages) {
            _uiState.value = _uiState.value.copy(currentPage = page)
        }
    }

    /**
     * Skip onboarding and go directly to home
     */
    fun skipOnboarding() {
        completeOnboarding()
    }

    /**
     * Complete onboarding, save the flag, start trial, and trigger navigation to home
     */
    fun completeOnboarding() {
        viewModelScope.launch {
            try {
                // Save onboarding completed flag
                settingsRepository.setOnboardingCompleted(true)

                // Mark first launch as completed
                settingsRepository.markAppLaunched()

                // Start 7-day free trial for new users
                premiumManager.startTrial()

                // Trigger navigation to home
                _uiState.value = _uiState.value.copy(shouldNavigateToHome = true)
            } catch (e: Exception) {
                // Even if saving fails, still navigate to home
                _uiState.value = _uiState.value.copy(shouldNavigateToHome = true)
            }
        }
    }

    /**
     * Check if current page is the last page
     */
    fun isLastPage(): Boolean = _uiState.value.currentPage == totalPages - 1

    /**
     * Reset navigation flag after navigation has occurred
     */
    fun onNavigationHandled() {
        _uiState.value = _uiState.value.copy(shouldNavigateToHome = false)
    }
}
