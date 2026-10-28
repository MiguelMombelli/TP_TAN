package com.tan.presentation.models

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class ModelsViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow<ModelsUiState>(ModelsUiState.Content(fakeModels()))
    val state: StateFlow<ModelsUiState> = _state.asStateFlow()

    // Datos falsos: en la Etapa 2 real estas acciones llaman al repositorio.
    fun onDownload(id: String) = changeStatus(id, ModelStatus.Downloaded)
    fun onDelete(id: String) = changeStatus(id, ModelStatus.NotDownloaded)
    fun onSelect(id: String) = _state.update { s ->
        if (s !is ModelsUiState.Content) s else s.copy(
            models = s.models.map {
                when {
                    it.id == id -> it.copy(status = ModelStatus.Active)
                    it.status == ModelStatus.Active -> it.copy(status = ModelStatus.Downloaded)
                    else -> it
                }
            }
        )
    }

    private fun changeStatus(id: String, status: ModelStatus) = _state.update { s ->
        if (s !is ModelsUiState.Content) s
        else s.copy(models = s.models.map { if (it.id == id) it.copy(status = status) else it })
    }
}

internal fun fakeModels() = listOf(
    ModelItemUi("tiny", "ggml-tiny", "75 MB", "⚡ Más rápido · Precisión básica",
        "Ideal para notas rápidas", ModelStatus.NotDownloaded),
    ModelItemUi("base", "ggml-base", "145 MB", "⚡ Rápido · Buena precisión",
        "Ideal para uso diario", ModelStatus.NotDownloaded),
    ModelItemUi("small", "ggml-small", "466 MB", "Lento · Alta precisión",
        "Ideal para reuniones y entrevistas", ModelStatus.NotDownloaded),
    ModelItemUi("medium", "ggml-medium", "1500 MB", "Muy lento · Máxima precisión",
        "Ideal para máxima precisión", ModelStatus.NotDownloaded),
)