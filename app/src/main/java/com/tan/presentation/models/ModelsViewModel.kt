package com.tan.presentation.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tan.domain.engine.TranscriptionEngine
import com.tan.domain.model.ModelCatalog
import com.tan.domain.repository.DownloadFailure
import com.tan.domain.repository.DownloadState
import com.tan.domain.repository.ModelRepository
import com.tan.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ModelsViewModel @Inject constructor(
    private val models: ModelRepository,
    private val prefs: PreferencesRepository,
    private val engine: TranscriptionEngine,
) : ViewModel() {

    // La pantalla no guarda estado propio: lo arma combinando tres fuentes del repositorio
    // (qué hay descargado, qué se está descargando y cuál es el modelo activo).
    val state: StateFlow<ModelsUiState> = combine(
        models.downloadedIds,
        models.downloads,
        prefs.activeModelId,
    ) { downloaded, downloads, activeId ->
        ModelsUiState.Content(
            models = ModelCatalog.all.map { m ->
                val download = downloads[m.id]
                ModelItemUi(
                    id = m.id,
                    name = "ggml-${m.id}",
                    sizeLabel = m.sizeLabel,
                    speedLabel = m.speedLabel,
                    description = m.description,
                    status = when {
                        download is DownloadState.Progress -> ModelStatus.Downloading(download.fraction)
                        m.id in downloaded && m.id == activeId -> ModelStatus.Active
                        m.id in downloaded -> ModelStatus.Downloaded
                        else -> ModelStatus.NotDownloaded
                    },
                )
            },
            errorMessage = downloads.values.filterIsInstance<DownloadState.Failed>().firstOrNull()
                ?.let { errorText(it.reason) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ModelsUiState.Loading)

    init {
        // Si volvés a la pantalla, no queda el cartel rojo de un intento anterior.
        models.clearFailures()
    }

    fun onDownload(id: String) = models.startDownload(id)

    fun onSelect(id: String) {
        viewModelScope.launch { prefs.setActiveModelId(id) }
    }

    fun onDelete(id: String) {
        viewModelScope.launch {
            // Si el modelo borrado es el que está cargado en memoria, se libera también la RAM.
            if (prefs.activeModelId.first() == id) engine.unloadModel()
            models.delete(id)
        }
    }
}

private fun errorText(reason: DownloadFailure) = when (reason) {
    DownloadFailure.NO_CONNECTION -> "Sin conexión a internet. Verificá tu conexión e intentá de nuevo"
    DownloadFailure.NO_SPACE -> "No hay espacio suficiente en el dispositivo para este modelo"
    DownloadFailure.OTHER -> "No se pudo descargar el modelo. Intentá de nuevo en un rato"
}

// Datos de ejemplo: solo para las previews de ModelsScreen.
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
