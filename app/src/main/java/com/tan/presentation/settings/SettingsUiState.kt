package com.tan.presentation.settings

sealed interface SettingsUiState {
    data object Loading : SettingsUiState
    data class Content(
        /** Null si todavía no hay un modelo elegido. */
        val activeModel: ActiveModelUi?,
        val language: String
    ) : SettingsUiState
}

data class ActiveModelUi(
    val name: String,        // "ggml-base"
    val sizeLabel: String,   // "145 MB"
    val description: String  // "Rápido · Buena precisión"
)
