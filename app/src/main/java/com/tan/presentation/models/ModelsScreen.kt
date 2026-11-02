package com.tan.presentation.models

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.tan.presentation.components.TanTopBar
import com.tan.ui.theme.TANTheme
import com.tan.ui.theme.TanNaranja
import com.tan.ui.theme.TanRojo
import com.tan.ui.theme.TanTarjeta
import com.tan.ui.theme.TanTarjetaActiva
import com.tan.ui.theme.TanTexto
import com.tan.ui.theme.TanTurquesa

@Composable
fun ModelsScreen(
    onBack: () -> Unit,
    viewModel: ModelsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    ModelsContent(
        state = state,
        onBack = onBack,
        onDownload = viewModel::onDownload,
        onSelect = viewModel::onSelect,
        onDelete = viewModel::onDelete,
    )
}

@Composable
fun ModelsContent(
    state: ModelsUiState,
    onBack: () -> Unit,
    onDownload: (String) -> Unit,
    onSelect: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    Scaffold(
        topBar = { TanTopBar(title = "Seleccionar modelo", onBack = onBack) },
        bottomBar = {
            if (state is ModelsUiState.Content && state.connectionError) {
                OfflineBanner()
            }
        },
    ) { padding ->
        when (state) {
            ModelsUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = TanTurquesa)
            }

            is ModelsUiState.Content -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.models, key = { it.id }) { model ->
                    ModelCard(
                        model = model,
                        onDownload = { onDownload(model.id) },
                        onSelect = { onSelect(model.id) },
                        onDelete = { onDelete(model.id) },
                    )
                }
            }
        }
    }
}

// Franja roja pegada abajo, sin botón (Figma "Error descarga").
@Composable
private fun OfflineBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(TanRojo)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = "Sin conexión a internet. Verificá tu conexión e intentá de nuevo",
            color = Color.White,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun ModelCard(
    model: ModelItemUi,
    onDownload: () -> Unit,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
) {
    val status = model.status
    val active = status is ModelStatus.Active
    val container = if (active) TanTarjetaActiva else TanTarjeta

    Card(
        onClick = onSelect,
        enabled = status is ModelStatus.Downloaded, // tocar una tarjeta descargada la activa
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = container,
            disabledContainerColor = container,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (active) BorderStroke(1.dp, TanTurquesa) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(model.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TanTexto)
                Text(model.sizeLabel, fontSize = 11.sp, color = TanTexto.copy(alpha = 0.6f))
                Text(model.speedLabel, fontSize = 11.sp, color = TanTexto.copy(alpha = 0.7f))
                Text(model.description, fontSize = 10.sp, color = TanTexto.copy(alpha = 0.6f))

                if (status is ModelStatus.Downloading) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { status.progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = TanTurquesa,
                        trackColor = TanTurquesa.copy(alpha = 0.25f),
                        strokeCap = StrokeCap.Round,
                    )
                    Text(
                        text = "${(status.progress * 100).toInt()}%",
                        fontSize = 10.sp,
                        color = TanTexto.copy(alpha = 0.6f),
                    )
                }

                if (active) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "En uso",
                        fontSize = 9.sp,
                        color = TanTurquesa,
                        modifier = Modifier
                            .border(1.dp, TanTurquesa, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }

            when (status) {
                ModelStatus.NotDownloaded -> Button(
                    onClick = onDownload,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TanNaranja),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp),
                ) {
                    Text("Descargar", fontSize = 11.sp, color = Color.White)
                }

                is ModelStatus.Downloading -> Unit // en descarga no hay botón

                ModelStatus.Downloaded -> DeleteIcon(onDelete)

                ModelStatus.Active -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Modelo activo",
                        tint = TanTurquesa,
                        modifier = Modifier.size(20.dp),
                    )
                    DeleteIcon(onDelete)
                }
            }
        }
    }
}

@Composable
private fun DeleteIcon(onDelete: () -> Unit) {
    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Eliminar modelo",
            tint = TanRojo,
            modifier = Modifier.size(20.dp),
        )
    }
}

// ---------- Previews: una por frame del Figma ----------

private val sample = fakeModels()

@Preview(showBackground = true)
@Composable
private fun ModelsLoadingPreview() = TANTheme {
    ModelsContent(ModelsUiState.Loading, {}, {}, {}, {})
}

@Preview(showBackground = true)
@Composable
private fun ModelsListPreview() = TANTheme {
    ModelsContent(ModelsUiState.Content(sample), {}, {}, {}, {})
}

@Preview(showBackground = true)
@Composable
private fun ModelsDownloadingPreview() = TANTheme {
    val models = sample.map {
        if (it.id == "base") it.copy(status = ModelStatus.Downloading(0.2f)) else it
    }
    ModelsContent(ModelsUiState.Content(models), {}, {}, {}, {})
}

@Preview(showBackground = true)
@Composable
private fun ModelsActivePreview() = TANTheme {
    val models = sample.map {
        if (it.id == "base") it.copy(status = ModelStatus.Active) else it
    }
    ModelsContent(ModelsUiState.Content(models), {}, {}, {}, {})
}

@Preview(showBackground = true)
@Composable
private fun ModelsOfflinePreview() = TANTheme {
    ModelsContent(ModelsUiState.Content(sample, connectionError = true), {}, {}, {}, {})
}