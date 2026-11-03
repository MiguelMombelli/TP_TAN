package com.tan.presentation.result

import androidx.lifecycle.ViewModel
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
            text = fakeResultText()
        )
    )
    val state: StateFlow<ResultUiState> = _state.asStateFlow()
}

fun fakeResultText(): String =
    "Receta para brownies.\n" +
        "Primero derretimos 200 gr de manteca con 300 gr de chocolate en baño maria.\n" +
        "Batimos los huevos a punto letra y luego mezclamos con el chocolate una vez frio.\n" +
        "Hornear por 30min en temperatura 180 grados"
