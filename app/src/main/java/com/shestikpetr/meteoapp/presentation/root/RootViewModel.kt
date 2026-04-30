package com.shestikpetr.meteoapp.presentation.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shestikpetr.meteoapp.domain.model.ThemeMode
import com.shestikpetr.meteoapp.domain.usecase.auth.IsLoggedInUseCase
import com.shestikpetr.meteoapp.domain.usecase.settings.ObserveSettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Хранит общеприложенческое состояние, которое нужно знать сразу при старте:
 * — определена ли стартовая навигация (логин/главная)
 * — какую тему выбрал пользователь.
 */
@HiltViewModel
class RootViewModel @Inject constructor(
    isLoggedIn: IsLoggedInUseCase,
    observeSettings: ObserveSettingsUseCase
) : ViewModel() {

    enum class StartDestination { Auth, Main }

    private val _start = MutableStateFlow<StartDestination?>(null)
    val start: StateFlow<StartDestination?> = _start.asStateFlow()

    val themeMode: StateFlow<ThemeMode> = observeSettings()
        .map { it.themeMode }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    init {
        viewModelScope.launch {
            _start.value =
                if (isLoggedIn()) StartDestination.Main else StartDestination.Auth
        }
    }
}
