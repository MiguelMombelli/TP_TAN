package com.tan.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.tan.presentation.components.TanTopBar
import com.tan.ui.theme.TANTheme
import com.tan.ui.theme.TanTarjeta
import com.tan.ui.theme.TanTexto

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onManageModels: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    SettingsContent(state, onBack, onManageModels)
}

@Composable
fun SettingsContent(
    state: SettingsUiState,
    onBack: () -> Unit,
    onManageModels: () -> Unit
) {
    Scaffold(topBar = { TanTopBar("Configuración", onBack) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (state) {
                SettingsUiState.Loading ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))

                is SettingsUiState.Content ->
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Modelo activo", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TanTexto)
                        ActiveModelCard(state.activeModel)

                        // Lleva a la pantalla de Modelos para descargar, elegir o borrar.
                        Card(
                            onClick = onManageModels,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Administrar modelos", modifier = Modifier.weight(1f))
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                            }
                        }

                        Text(
                            text = "Idioma de transcripción: ${state.language}",
                            fontSize = 12.sp,
                            color = TanTexto.copy(alpha = 0.7f)
                        )
                    }
            }
        }
    }
}

@Composable
private fun ActiveModelCard(model: ActiveModelUi?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = TanTarjeta)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (model == null) {
                Text("Todavía no elegiste un modelo", fontWeight = FontWeight.Bold, color = TanTexto)
                Text(
                    "Descargá uno desde “Administrar modelos” para poder transcribir.",
                    fontSize = 12.sp,
                    color = TanTexto.copy(alpha = 0.7f)
                )
            } else {
                Text(model.name, fontWeight = FontWeight.Bold, color = TanTexto)
                Text(model.sizeLabel, fontSize = 12.sp, color = TanTexto.copy(alpha = 0.7f))
                Text(model.description, fontSize = 12.sp, color = TanTexto.copy(alpha = 0.7f))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsLoadingPreview() {
    TANTheme { SettingsContent(SettingsUiState.Loading, {}, {}) }
}

@Preview(showBackground = true)
@Composable
private fun SettingsWithModelPreview() {
    TANTheme {
        SettingsContent(SettingsUiState.Content(fakeActiveModel(), "Español"), {}, {})
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsWithoutModelPreview() {
    TANTheme {
        SettingsContent(SettingsUiState.Content(null, "Español"), {}, {})
    }
}
