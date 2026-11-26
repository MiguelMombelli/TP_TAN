package com.tan.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tan.domain.model.TranscriptionSummary
import com.tan.domain.repository.TranscriptionRepository
import com.tan.presentation.format.toHistoryState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HistoryViewModel @Inject constructor(
    repository: TranscriptionRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")

    /** Lo que hay escrito en el buscador; vive acá para que sobreviva a rotar la pantalla. */
    val query: StateFlow<String> = _query.asStateFlow()

    fun onQueryChange(text: String) {
        _query.value = text
    }

    /**
     * Lee de Room: se actualiza sola al guardar o borrar, y se vuelve a consultar con cada cambio
     * del buscador. flatMapLatest descarta la consulta anterior si el usuario sigue escribiendo.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<HistoryUiState> = _query
        .flatMapLatest { q ->
            repository.search(q)
                .map { list -> list.toState(q) }
                .catch { emit(HistoryUiState.Error("No se pudo cargar el historial")) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState.Loading)

    private fun List<TranscriptionSummary>.toState(query: String): HistoryUiState =
        if (isEmpty() && query.isNotBlank()) HistoryUiState.NoResults else toHistoryState()
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