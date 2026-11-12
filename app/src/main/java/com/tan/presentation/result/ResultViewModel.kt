package com.tan.presentation.result

import androidx.lifecycle.ViewModel
import com.tan.domain.model.Segment
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class ResultViewModel @Inject constructor() : ViewModel() {
    // Datos falsos. Más adelante lee la transcripción real desde TranscriptionRepository.
    private val _state = MutableStateFlow<ResultUiState>(
        ResultUiState.Content(
            title = "Grabación 17/04/2026 21:15",
            segments = fakeSegments()
        )
    )
    val state: StateFlow<ResultUiState> = _state.asStateFlow()
}

fun fakeSegments(): List<Segment> = listOf(
    Segment(0, 3_200, "Receta para brownies"),
    Segment(3_200, 9_000, "Primero derretimos 200 gr de manteca con 300 gr de chocolate en baño maria."),
    Segment(9_000, 16_500, "Batimos los huevos a punto letra y luego mezclamos con el chocolate una vez frio."),
    Segment(16_500, 23_000, "Hornear por 30min en temperatura 180 grados")
)