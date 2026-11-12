package com.tan.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.tan.presentation.navigation.Routes
import com.tan.presentation.result.fakeSegments
import com.tan.domain.model.Segment
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val id: Long = savedStateHandle[Routes.ARG_ID] ?: -1L

    // Datos falsos. Más adelante busca la transcripción con TranscriptionRepository.getById(id).
    private val _state = MutableStateFlow<DetailUiState>(
        fakeDetail(id)?.let { DetailUiState.Content(it) }
            ?: DetailUiState.Error("No se encontró la transcripción")
    )
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    fun onDelete() {
        // Con datos reales: repository.delete(id)
    }
}

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
