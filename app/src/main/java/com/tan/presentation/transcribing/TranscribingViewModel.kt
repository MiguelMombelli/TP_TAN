package com.tan.presentation.transcribing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tan.domain.model.Transcription
import com.tan.domain.model.TranscriptionSource
import com.tan.domain.repository.TranscriptionRepository
import com.tan.presentation.format.defaultTitle
import com.tan.presentation.result.fakeSegments
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class TranscribingViewModel @Inject constructor(
    private val repository: TranscriptionRepository
) : ViewModel() {

    private val _state = MutableStateFlow<TranscribingUiState>(
        TranscribingUiState.InProgress(progress = 0f, minutesLeft = 10)
    )
    val state: StateFlow<TranscribingUiState> = _state.asStateFlow()

    private var job: Job? = null

    // Transcripción SIMULADA: el progreso sube de a poco y al llegar a 100% se guarda en Room
    // una transcripción de ejemplo. En la Etapa 8b esto se reemplaza por el motor real
    // (progreso, segmentos y cancelación vienen de TranscriptionEngine).
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
            _state.value = try {
                val now = System.currentTimeMillis()
                val id = repository.save(
                    Transcription(
                        title = defaultTitle(TranscriptionSource.RECORDING, now),
                        createdAtMs = now,
                        source = TranscriptionSource.RECORDING,
                        model = "tiny",
                        durationMs = 23_000,
                        segments = fakeSegments()
                    )
                )
                TranscribingUiState.Done(id)
            } catch (e: Exception) {
                TranscribingUiState.Error(progress = 1f, minutesLeft = 0)
            }
        }
    }

    fun onCancel() {
        job?.cancel()
    }
}
