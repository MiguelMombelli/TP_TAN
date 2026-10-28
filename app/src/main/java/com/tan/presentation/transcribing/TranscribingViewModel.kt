package com.tan.presentation.transcribing

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
class TranscribingViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow<TranscribingUiState>(
        TranscribingUiState.InProgress(progress = 0f, minutesLeft = 10)
    )
    val state: StateFlow<TranscribingUiState> = _state.asStateFlow()

    private var job: Job? = null

    // Progreso falso: sube de a poco hasta 100%. El real viene del motor en la Etapa 3.
    init {
        job = viewModelScope.launch {
            var p = 0f
            while (p < 1f) {
                delay(300)
                p = (p + 0.02f).coerceAtMost(1f)
                _state.value = TranscribingUiState.InProgress(
                    progress = p,
                    minutesLeft = ((1f - p) * 10).toInt() + 1,
                )
            }
        }
    }

    fun onCancel() {
        job?.cancel()
    }
}