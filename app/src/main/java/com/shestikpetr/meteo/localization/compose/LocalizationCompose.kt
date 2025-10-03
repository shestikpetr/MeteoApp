package com.shestikpetr.meteo.localization.compose

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shestikpetr.meteo.localization.interfaces.StringKey
import com.shestikpetr.meteo.localization.interfaces.StringResourceManager
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Composable function to provide string resource manager to child composables
 * Creates a composition local for easy access throughout the UI tree
 */
val LocalStringResourceManager = compositionLocalOf<StringResourceManager> {
    error("StringResourceManager not provided")
}

/**
 * Provider composable that makes StringResourceManager available to child composables
 */
@Composable
fun LocalizationProvider(
    stringResourceManager: StringResourceManager,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalStringResourceManager provides stringResourceManager,
        content = content
    )
}

/**
 * Helper function to get localized string in Compose
 * Use this throughout your UI components for string access
 */
@Composable
fun stringResource(key: StringKey): String {
    val stringManager = LocalStringResourceManager.current
    return stringManager.getStringSync(key)
}

/**
 * Helper function to get localized string with formatting in Compose
 */
@Composable
fun stringResource(key: StringKey, vararg args: Any): String {
    val stringManager = LocalStringResourceManager.current
    return stringManager.getStringSync(key, *args)
}

/**
 * ViewModel for handling localization state in UI
 * Provides reactive access to current locale and language switching
 */
@Stable
class LocalizationViewModel @Inject constructor(
    private val stringResourceManager: StringResourceManager
) : ViewModel() {

    private val _currentLocale = mutableStateOf("ru")
    val currentLocale: State<String> = _currentLocale

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _supportedLocales = mutableStateOf(listOf("ru", "en"))
    val supportedLocales: State<List<String>> = _supportedLocales

    init {
        viewModelScope.launch {
            _currentLocale.value = stringResourceManager.getCurrentLocale()
            loadSupportedLocales()
        }
    }

    fun changeLocale(locale: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                stringResourceManager.setLocale(locale)
                _currentLocale.value = locale
            } catch (e: Exception) {
                // Handle error - perhaps show a snackbar
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadSupportedLocales() {
        // This would be loaded from the service in a real implementation
        _supportedLocales.value = listOf("ru", "en")
    }
}

