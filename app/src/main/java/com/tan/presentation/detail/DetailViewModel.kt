package com.tan.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tan.domain.model.Segment
import com.tan.domain.repository.TranscriptionRepository
import com.tan.presentation.format.toDetailUi
import com.tan.presentation.navigation.Routes
import com.tan.presentation.result.fakeSegments
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: TranscriptionRepository
) : ViewModel() {

    private val id: Long = savedStateHandle[Routes.ARG_ID] ?: -1L

    private val _state = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = try {
                val transcription = repository.getById(id)
                if (transcription == null) {
                    DetailUiState.Error("No se encontró la transcripción")
                } else {
                    DetailUiState.Content(transcription.toDetailUi())
                }
            } catch (e: Exception) {
                DetailUiState.Error("No se pudo cargar la transcripción")
            }
        }
    }

    /** Borra la transcripción y, recién cuando terminó, avisa para volver atrás. */
    fun onDelete(onDone: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.delete(id)
                onDone()
            } catch (e: Exception) {
                _state.value = DetailUiState.Error("No se pudo borrar la transcripción")
            }
        }
    }
}

/** Datos de ejemplo solo para las vistas previas (@Preview). */
fun fakeDetail(id: Long): DetailUi? = when (id) {
    1L -> DetailUi(
        title = "Grabación 17/04/2026 21:15",
        date = "17/04/2026",
        sourceLabel = "Grabación",
        model = "tiny",
        segments = fakeSegments()
    )
    2L -> DetailUi(
        title = "Entrevista a un paciente",
        date = "09/09/2026",
        sourceLabel = "Archivo",
        model = "base",
        segments = listOf(
            Segment(0, 5_200, "El entrevistado comenta que hace tres semanas solo come flips"),
            Segment(5_200, 9_800, "y que le cuesta dormir.")
        )
    )
    else -> null
}
