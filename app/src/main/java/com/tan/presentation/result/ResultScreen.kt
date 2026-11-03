package com.tan.presentation.result

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.tan.presentation.components.TanButton
import com.tan.presentation.components.TanButtonStyle
import com.tan.presentation.components.TanTopBar
import com.tan.presentation.components.TranscriptionTextCard
import com.tan.ui.theme.TANTheme

@Composable
fun ResultScreen(
    onBack: () -> Unit,
    onRecordAgain: () -> Unit,
    // Se conectan con FileProvider y la hoja del sistema en la etapa de compartir.
    onShareTxt: () -> Unit = {},
    onShareSrt: () -> Unit = {},
    viewModel: ResultViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    ResultContent(
        state = state,
        onBack = onBack,
        onRecordAgain = onRecordAgain,
        onShareTxt = onShareTxt,
        onShareSrt = onShareSrt
    )
}

@Composable
fun ResultContent(
    state: ResultUiState,
    onBack: () -> Unit,
    onRecordAgain: () -> Unit,
    onShareTxt: () -> Unit,
    onShareSrt: () -> Unit
) {
    Scaffold(topBar = { TanTopBar("Transcripción", onBack) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (state) {
                ResultUiState.Loading ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))

                is ResultUiState.Error ->
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        TanButton(text = "Volver", onClick = onBack)
                    }

                is ResultUiState.Content ->
                    Column(Modifier.fillMaxSize().padding(16.dp)) {
                        Text(
                            text = state.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(12.dp))
                        TranscriptionTextCard(text = state.text, modifier = Modifier.weight(1f))
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            TanButton(
                                text = "Compartir TXT",
                                onClick = onShareTxt,
                                modifier = Modifier.weight(1f),
                                style = TanButtonStyle.Turquesa
                            )
                            TanButton(
                                text = "Compartir SRT",
                                onClick = onShareSrt,
                                modifier = Modifier.weight(1f),
                                style = TanButtonStyle.Turquesa
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        TanButton(text = "Grabar de nuevo", onClick = onRecordAgain)
                    }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ResultLoadingPreview() {
    TANTheme { ResultContent(ResultUiState.Loading, {}, {}, {}, {}) }
}

@Preview(showBackground = true)
@Composable
private fun ResultContentPreview() {
    TANTheme {
        ResultContent(
            ResultUiState.Content("Grabación 17/04/2026 21:15", fakeResultText()),
            {}, {}, {}, {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ResultErrorPreview() {
    TANTheme { ResultContent(ResultUiState.Error("No se pudo cargar la transcripción"), {}, {}, {}, {}) }
}
