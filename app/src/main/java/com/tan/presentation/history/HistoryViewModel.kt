package com.tan.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tan.domain.repository.TranscriptionRepository
import com.tan.presentation.format.toHistoryState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HistoryViewModel @Inject constructor(
    repository: TranscriptionRepository
) : ViewModel() {

    /** Lee de Room: se actualiza sola al guardar o borrar una transcripción. */
    val state: StateFlow<HistoryUiState> = repository.observeAll()
        .map { list -> list.toHistoryState() }
        .catch { emit(HistoryUiState.Error("No se pudo cargar el historial")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState.Loading)
}

/** Datos de ejemplo solo para las vistas previas (@Preview). */
fun fakeItems() = listOf(
    HistoryItemUi(
        id = 1,
        title = "Grabación 17/04/2026 21:15",
        date = "17/04/2026",
        sourceLabel = "Grabación",
        model = "tiny",
        preview = "Receta para brownies. Primero derretimos 200 gr de manteca con 300 gr..."
    ),
    HistoryItemUi(
        id = 2,
        title = "Entrevista a un paciente",
        date = "09/09/2026",
        sourceLabel = "Archivo",
        model = "base",
        preview = "El entrevistado comenta que hace tres semanas solo come flips..."
    )
)