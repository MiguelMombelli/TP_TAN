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
    HistoryItemUi(
        id = 1,
        title = "Grabación 17/04/2026 21:15",
        date = "17/04/2026",
        sourceLabel = "Grabación",
        model = "tiny",
        preview = "Receta para brownies. Primero derretimos 200 gr de manteca con 300 gr ..."
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