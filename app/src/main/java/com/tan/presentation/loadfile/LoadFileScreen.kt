package com.tan.presentation.loadfile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.tan.R
import com.tan.presentation.components.TanTopBar
import com.tan.ui.theme.TANTheme
import com.tan.ui.theme.TanNaranja
import com.tan.ui.theme.TanRojo
import com.tan.ui.theme.TanTexto

@Composable
fun LoadFileScreen(
    onBack: () -> Unit,
    onContinue: (wavPath: String) -> Unit,
    viewModel: LoadFileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Selector de archivos del sistema: no pide ningún permiso de almacenamiento.
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        viewModel.onFilePicked(uri?.toString())
    }

    // Con el audio ya convertido se pasa a Transcribiendo; después se limpia el estado para que,
    // al volver atrás, no se repita la navegación.
    LaunchedEffect(state) {
        val current = state
        if (current is LoadFileUiState.Converted) {
            onContinue(current.wavPath)
            viewModel.onNavigated()
        }
    }

    LoadFileContent(
        state = state,
        onBack = onBack,
        onPickFile = { picker.launch(arrayOf("audio/*", "video/mp4", "application/ogg")) },
        onContinue = viewModel::onContinue,
        onCancel = viewModel::onCancelConversion,
    )
}

@Composable
fun LoadFileContent(
    state: LoadFileUiState,
    onBack: () -> Unit,
    onPickFile: () -> Unit,
    onContinue: () -> Unit,
    onCancel: () -> Unit = {},
) {
    val converting = state is LoadFileUiState.Converting

    Scaffold(topBar = { TanTopBar(title = "Cargar archivo", onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Importar audio",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TanTexto,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Selecciona una grabación para comenzar una nueva transcripción.",
                fontSize = 12.sp,
                color = TanTexto.copy(alpha = 0.6f),
            )

            Spacer(Modifier.height(20.dp))
            DropZone(onClick = { if (!converting) onPickFile() })

            if (state is LoadFileUiState.UnsupportedFormat) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Formato no soportado. Elegí un archivo MP3, M4A o WAV.",
                    fontSize = 12.sp,
                    color = TanRojo,
                )
            }
            if (state is LoadFileUiState.Failed) {
                Spacer(Modifier.height(10.dp))
                Text(text = state.message, fontSize = 12.sp, color = TanRojo)
            }

            val card: Pair<String, String>? = when (state) {
                is LoadFileUiState.Selected -> state.fileName to state.details
                is LoadFileUiState.Converting -> state.fileName to state.details
                else -> null
            }
            if (card != null) {
                Spacer(Modifier.height(28.dp))
                Text(
                    text = "ARCHIVO SELECCIONADO",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = TanTexto.copy(alpha = 0.5f),
                )
                Spacer(Modifier.height(10.dp))
                FileCard(name = card.first, details = card.second)
            }

            if (state is LoadFileUiState.Converting) {
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { state.percent / 100f },
                    color = TanNaranja,
                    trackColor = TanNaranja.copy(alpha = 0.2f),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Preparando el audio… ${state.percent} %",
                    fontSize = 12.sp,
                    color = TanTexto.copy(alpha = 0.6f),
                )
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = if (converting) onCancel else onContinue,
                enabled = state is LoadFileUiState.Selected || converting,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TanNaranja,
                    disabledContainerColor = TanNaranja.copy(alpha = 0.4f),
                ),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text(if (converting) "Cancelar" else "Continuar", fontSize = 14.sp, color = Color.White)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DropZone(onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(shape)
            .background(TanNaranja.copy(alpha = 0.3f), shape)
            .clickable(onClick = onClick)
            .drawBehind {
                drawRoundRect(
                    color = TanNaranja,
                    cornerRadius = CornerRadius(16.dp.toPx()),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f)),
                    ),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier.size(48.dp).background(Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_upload),
                    contentDescription = null,
                    tint = TanNaranja,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Selecciona un archivo",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TanTexto,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "MP3, M4A, WAV · máximo 2 GB",
                fontSize = 10.sp,
                color = TanTexto.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun FileCard(name: String, details: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(TanNaranja.copy(alpha = 0.25f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_file),
                    contentDescription = null,
                    tint = TanNaranja,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TanTexto)
                Text(details, fontSize = 11.sp, color = TanTexto.copy(alpha = 0.6f))
            }
        }
    }
}

// ---------- Previews ----------

@Preview(showBackground = true)
@Composable
private fun LoadFileEmptyPreview() = TANTheme {
    LoadFileContent(LoadFileUiState.Empty, {}, {}, {})
}

@Preview(showBackground = true)
@Composable
private fun LoadFileSelectedPreview() = TANTheme {
    LoadFileContent(
        LoadFileUiState.Selected("Entrevista_grupo3.mp3", "38 MB · 42 min"),
        {}, {}, {},
    )
}

@Preview(showBackground = true)
@Composable
private fun LoadFileUnsupportedPreview() = TANTheme {
    LoadFileContent(LoadFileUiState.UnsupportedFormat, {}, {}, {})
}

@Preview(showBackground = true)
@Composable
private fun LoadFileConvertingPreview() = TANTheme {
    LoadFileContent(
        LoadFileUiState.Converting("Entrevista_grupo3.mp3", "38 MB · 42 min", 35),
        {}, {}, {},
    )
}

@Preview(showBackground = true)
@Composable
private fun LoadFileFailedPreview() = TANTheme {
    LoadFileContent(LoadFileUiState.Failed("No se pudo abrir el archivo."), {}, {}, {})
}
