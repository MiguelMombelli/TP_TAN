package com.tan.presentation.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tan.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val prefs: PreferencesRepository,
) : ViewModel() {

    /** Marca la bienvenida como vista para que no vuelva a aparecer. */
    fun onContinue() {
        viewModelScope.launch { prefs.markWelcomeSeen() }
    }
}
