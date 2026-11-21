package com.tan.presentation.start

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tan.domain.repository.PreferencesRepository
import com.tan.presentation.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Decide con qué pantalla arranca la app: la bienvenida solo la primera vez. */
@HiltViewModel
class StartViewModel @Inject constructor(
    prefs: PreferencesRepository,
) : ViewModel() {

    /** null mientras se lee el ajuste (unos milisegundos); después queda fija para toda la sesión. */
    val startRoute: StateFlow<String?> = flow {
        emit(if (prefs.welcomeSeen.first()) Routes.HOME else Routes.WELCOME)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
