package com.tan.presentation.transcribing

import androidx.compose.foundation.background
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.tan.presentation.components.TanTopBar
import com.tan.ui.theme.TANTheme
import com.tan.ui.theme.TanNaranja
import com.tan.ui.theme.TanTarjeta
import com.tan.ui.theme.TanTexto
import com.tan.ui.theme.TanTurquesa

@Composable
fun TranscribingScreen(
    onCancel: () -> Unit,
    onErrorAccepted: () -> Unit,
    viewModel: TranscribingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    TranscribingContent(
        state = state,
        onCancel = {
            viewModel.onCancel()
            onCancel()
        },
        onErrorAccepted = onErrorAccepted,
    )
}

@Composable
fun TranscribingContent(
    state: TranscribingUiState,
    onCancel: () -> Unit,
    onErrorAccepted: () -> Unit,
) {
    val progress: Float
    val minutesLeft: Int
    when (state) {
        is TranscribingUiState.InProgress -> {
            progress = state.progress
            minutesLeft = state.minutesLeft
        }
        is TranscribingUiState.Error -> {
            progress = state.progress
            minutesLeft = state.minutesLeft
        }
    }

    Scaffold(topBar = { TanTopBar(title = "Transcribiendo") }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))

            // Anillo de progreso con el porcentaje adentro
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(170.dp),
                    color = TanNaranja,
                    trackColor = TanTurquesa.copy(alpha = 0.15f),
                    strokeWidth = 14.dp,
                    strokeCap = StrokeCap.Round,
                    gapSize = 0.dp,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TanTexto,
                    )
                    Text("completado", fontSize = 10.sp, color = TanTexto.copy(alpha = 0.6f))
                }
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text = "Procesando audio de larga duración",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TanTexto,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Quedan aproximadamente $minutesLeft ${if (minutesLeft == 1) "minuto" else "minutos"}. " +
                        "Puedes salir de esta pantalla sin interrumpir el proceso.",
                fontSize = 12.sp,
                color = TanTexto.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.weight(1.4f))

            // Tarjeta de seguridad
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TanTarjeta, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(8.dp).background(TanNaranja, CircleShape))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "El procesamiento ocurre de forma segura en tu dispositivo.",
                    fontSize = 10.sp,
                    color = TanTexto.copy(alpha = 0.7f),
                )
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onCancel,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TanNaranja),
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Text("Cancelar", fontSize = 14.sp, color = Color.White)
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (state is TranscribingUiState.Error) {
        ErrorDialog(onAccept = onErrorAccepted)
    }
}

@Composable
private fun ErrorDialog(onAccept: () -> Unit) {
    Dialog(onDismissRequest = {}) {
        Surface(shape = RoundedCornerShape(12.dp), color = Color.White) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Text(
                    text = "Error en el proceso de transcripción",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TanTexto,
                    textAlign = TextAlign.Center,
                )
                Button(
                    onClick = onAccept,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TanNaranja),
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                ) {
                    Text("Aceptar", fontSize = 14.sp, color = Color.White)
                }
            }
        }
    }
}

// ---------- Previews: un frame del Figma por estado ----------

@Preview(showBackground = true)
@Composable
private fun TranscribingInProgressPreview() = TANTheme {
    TranscribingContent(TranscribingUiState.InProgress(0.62f, 8), {}, {})
}

@Preview(showBackground = true)
@Composable
private fun TranscribingStartPreview() = TANTheme {
    TranscribingContent(TranscribingUiState.InProgress(0f, 10), {}, {})
}

@Preview(showBackground = true)
@Composable
private fun TranscribingErrorPreview() = TANTheme {
    TranscribingContent(TranscribingUiState.Error(0.62f, 8), {}, {})
}