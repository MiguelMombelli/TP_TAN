package com.tan.presentation.models

sealed interface ModelStatus {
    data object NotDownloaded : ModelStatus
    data class Downloading(val progress: Float) : ModelStatus // 0f..1f
    data object Downloaded : ModelStatus
    data object Active : ModelStatus
}

data class ModelItemUi(
    val id: String,
    val name: String,          // "ggml-tiny"
    val sizeLabel: String,     // "75 MB"
    val speedLabel: String,    // "⚡ Más rápido · Precisión básica"
    val description: String,   // "Ideal para notas rápidas"
    val status: ModelStatus,
)

sealed interface ModelsUiState {
    data object Loading : ModelsUiState
    data class Content(
        val models: List<ModelItemUi>,
        val connectionError: Boolean = false, // franja roja inferior
    ) : ModelsUiState
}