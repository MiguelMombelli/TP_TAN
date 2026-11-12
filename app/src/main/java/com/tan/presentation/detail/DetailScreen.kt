package com.tan.presentation.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.tan.export.ExportFormat
import com.tan.presentation.components.TanButton
import com.tan.presentation.components.TanButtonStyle
import com.tan.presentation.components.TanChip
import com.tan.presentation.components.TanTopBar
import com.tan.presentation.components.TranscriptionTextCard
import com.tan.share.TranscriptSharer
import com.tan.ui.theme.TANTheme
import com.tan.ui.theme.TanNaranja
import com.tan.ui.theme.TanRojo
import com.tan.ui.theme.TanTexto
import com.tan.ui.theme.TanTurquesa

@Composable
fun DetailScreen(
    onBack: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    fun share(format: ExportFormat) {
        val detail = (state as? DetailUiState.Content)?.detail ?: return
        TranscriptSharer.share(context, detail.title, detail.segments, format)
    }

    DetailContent(
        state = state,
        onBack = onBack,
        onShareTxt = { share(ExportFormat.TXT) },
        onShareSrt = { share(ExportFormat.SRT) },
        onDelete = {
            viewModel.onDelete()
            onBack()
        }
    )
}

@Composable
fun DetailContent(
    state: DetailUiState,
    onBack: () -> Unit,
    onShareTxt: () -> Unit,
    onShareSrt: () -> Unit,
    onDelete: () -> Unit
) {
    // Solo estado de pantalla (si el diálogo está abierto), no datos.
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }

    Scaffold(topBar = { TanTopBar("Detalle", onBack) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (state) {
                DetailUiState.Loading ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))

                is DetailUiState.Error ->
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        TanButton(text = "Volver", onClick = onBack)
                    }

                is DetailUiState.Content -> {
                    val detail = state.detail
                    Column(Modifier.fillMaxSize().padding(16.dp)) {
                        Text(
                            text = detail.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(detail.date, fontSize = 12.sp, color = TanTexto)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TanChip(
                                text = detail.sourceLabel,
                                color = if (detail.sourceLabel == "Grabación") TanNaranja else TanTurquesa
                            )
                            TanChip(text = detail.model, color = TanTexto)
                        }
                        Spacer(Modifier.height(12.dp))
                        TranscriptionTextCard(text = detail.text, modifier = Modifier.weight(1f))
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
                        TextButton(
                            onClick = { confirmingDelete = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Borrar transcripción", color = TanRojo)
                        }
                    }
                }
            }
        }
    }

    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text("¿Borrar esta transcripción?") },
            text = { Text("Se elimina de este dispositivo y no se puede recuperar.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmingDelete = false
                    onDelete()
                }) { Text("Borrar", color = TanRojo) }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) { Text("Cancelar") }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DetailLoadingPreview() {
    TANTheme { DetailContent(DetailUiState.Loading, {}, {}, {}, {}) }
}

@Preview(showBackground = true)
@Composable
private fun DetailContentPreview() {
    TANTheme { DetailContent(DetailUiState.Content(fakeDetail(1)!!), {}, {}, {}, {}) }
}

@Preview(showBackground = true)
@Composable
private fun DetailFilePreview() {
    TANTheme { DetailContent(DetailUiState.Content(fakeDetail(2)!!), {}, {}, {}, {}) }
}

@Preview(showBackground = true)
@Composable
private fun DetailErrorPreview() {
    TANTheme { DetailContent(DetailUiState.Error("No se encontró la transcripción"), {}, {}, {}, {}) }
}
