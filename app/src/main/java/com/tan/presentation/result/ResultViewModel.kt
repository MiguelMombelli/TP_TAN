package com.tan.presentation.result

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tan.domain.model.Segment
import com.tan.domain.repository.TranscriptionRepository
import com.tan.presentation.format.toResultContent
import com.tan.presentation.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ResultViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: TranscriptionRepository
) : ViewModel() {

    private val id: Long = savedStateHandle[Routes.ARG_ID] ?: -1L

    private val _state = MutableStateFlow<ResultUiState>(ResultUiState.Loading)
    val state: StateFlow<ResultUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = try {
                repository.getById(id)?.toResultContent()
                    ?: ResultUiState.Error("No se encontró la transcripción")
            } catch (e: Exception) {
                ResultUiState.Error("No se pudo cargar la transcripción")
            }
        }
    }
}


fun fakeSegments(): List<Segment> = listOf(
    Segment(0, 3_200, "Receta para brownies"),
    Segment(3_200, 9_000, "Primero derretimos 200 gr de manteca con 300 gr de chocolate en baño maria."),
    Segment(9_000, 16_500, "Batimos los huevos a punto letra y luego mezclamos con el chocolate una vez frio."),
    Segment(16_500, 23_000, "Hornear por 30min en temperatura 180 grados")
)