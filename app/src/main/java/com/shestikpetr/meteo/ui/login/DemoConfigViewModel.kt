package com.shestikpetr.meteo.ui.login

import androidx.lifecycle.ViewModel
import com.shestikpetr.meteo.config.interfaces.DemoConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel for managing demo configuration data.
 * Separated from LoginScreen to follow Single Responsibility Principle.
 */
@HiltViewModel
class DemoConfigViewModel @Inject constructor(
    val demoConfigRepository: DemoConfigRepository
) : ViewModel()