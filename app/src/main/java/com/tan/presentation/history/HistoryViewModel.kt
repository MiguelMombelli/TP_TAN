package com.tan.presentation.history

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class HistoryViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow<HistoryUiState>(HistoryUiState.Content(fakeItems()))
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()
}

fun fakeItems() = listOf(
    HistoryItemUi(1, "Grabación 17/04/2026 21:15", "17/04/2026", "En la clase de hoy se compartió la receta de los tequeños..."),
    HistoryItemUi(2, "Entrevista a un paciente", "09/09/2026", "El entrevistado comenta que hace tres semanas solo come flips...")
)