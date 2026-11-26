package com.tan.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tan.domain.model.ModelCatalog
import com.tan.domain.repository.PreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    prefs: PreferencesRepository,
) : ViewModel() {

    val state: StateFlow<SettingsUiState> = prefs.activeModelId.map { id ->
        val model = id?.let { ModelCatalog.byId(it) }
        SettingsUiState.Content(
            activeModel = model?.let {
                ActiveModelUi(name = "ggml-${it.id}", sizeLabel = it.sizeLabel, description = it.speedLabel)
            },
            language = "Español", // por ahora el idioma es fijo
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState.Loading)
}

// Se usa en las previews de SettingsScreen.
fun fakeActiveModel() = ActiveModelUi(
    name = "ggml-base",
    sizeLabel = "145 MB",
    description = "Rápido · Buena precisión"
)
