package com.tan.presentation.loadfile

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class LoadFileViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow<LoadFileUiState>(LoadFileUiState.Empty)
    val state: StateFlow<LoadFileUiState> = _state.asStateFlow()

    // Es de ejemplo hasta que hagamos la conexión
    fun onPickFile() {
        _state.value = LoadFileUiState.Selected(
            fileName = "Entrevista_grupo3.mp3",
            details = "38 MB · 42 min",
        )
    }
}