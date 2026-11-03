package com.tan.presentation.settings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class SettingsViewModel @Inject constructor() : ViewModel() {
    // Datos falsos. Más adelante el modelo activo sale del almacenamiento de modelos de Miguel.
    private val _state = MutableStateFlow<SettingsUiState>(
        SettingsUiState.Content(activeModel = fakeActiveModel(), language = "Español")
    )
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()
}

fun fakeActiveModel() = ActiveModelUi(
    name = "ggml-base",
    sizeLabel = "145 MB",
    description = "Rápido · Buena precisión"
)
