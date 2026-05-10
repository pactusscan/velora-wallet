package com.andrutstudio.velora.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.andrutstudio.velora.data.local.AppPreferences
import com.andrutstudio.velora.data.local.ThemePreference
import com.andrutstudio.velora.domain.repository.WalletRepository
import com.andrutstudio.velora.presentation.navigation.ONBOARDING_GRAPH
import com.andrutstudio.velora.presentation.navigation.Screen
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val walletRepository: WalletRepository,
    private val appPreferences: AppPreferences,
) : ViewModel() {

    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination

    val themePreference: StateFlow<ThemePreference> = appPreferences.themeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemePreference.SYSTEM)

    init {
        viewModelScope.launch {
            delay(1200)
            if (walletRepository.hasWallet()) {
                walletRepository.loadLockedWallet()
                _startDestination.value = Screen.Home.route
            } else {
                _startDestination.value = ONBOARDING_GRAPH
            }
        }
    }
}
