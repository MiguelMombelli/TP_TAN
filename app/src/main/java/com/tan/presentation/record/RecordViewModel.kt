package com.tan.presentation.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecordViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow<RecordUiState>(RecordUiState.Idle)
    val state: StateFlow<RecordUiState> = _state.asStateFlow()

    private var timerJob: Job? = null

    // Datos falsos: solo simulamos el cronómetro. La grabación real llega en la Etapa 3.
    fun onStart() {
        _state.value = RecordUiState.Recording(0)
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var s = 0
            while (true) {
                delay(1000)
                s++
                _state.value = RecordUiState.Recording(s)
            }
        }
    }

    fun onStop() {
        timerJob?.cancel()
        _state.value = RecordUiState.Saved
    }

    fun onRecordAgain() {
        _state.value = RecordUiState.Idle
    }
}