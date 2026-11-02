package com.tan.presentation.record

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
import com.tan.ui.theme.TanGrabando
import com.tan.ui.theme.TanNaranja
import com.tan.ui.theme.TanTexto
import com.tan.ui.theme.TanTurquesa
import com.tan.ui.theme.TanVerde

@Composable
fun RecordScreen(
    onBack: () -> Unit,
    onTranscribe: () -> Unit,
    viewModel: RecordViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    RecordContent(
        state = state,
        onBack = onBack,
        onStart = viewModel::onStart,
        onStop = viewModel::onStop,
        onRecordAgain = viewModel::onRecordAgain,
        onTranscribe = onTranscribe,
    )
}

@Composable
fun RecordContent(
    state: RecordUiState,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRecordAgain: () -> Unit,
    onTranscribe: () -> Unit,
) {
    Scaffold(topBar = { TanTopBar(title = "Grabar audio", onBack = onBack) }) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            when (state) {
                RecordUiState.Idle -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    BigCircle(TanNaranja, onClick = onStart) { MicIcon() }
                    Text("Toca para grabar", fontSize = 13.sp, color = TanTexto)
                }

                is RecordUiState.Recording -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    BigCircle(TanGrabando, onClick = onStop) { StopSquare() }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = formatTime(state.seconds),
                        fontSize = 34.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black,
                    )
                    Text("Grabando…", fontSize = 13.sp, color = TanGrabando)
                }

                RecordUiState.Saved -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    BigCircle(TanVerde, onClick = null) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(64.dp),
                        )
                    }
                    Text(
                        "Grabación guardada",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TanVerde,
                    )
                    Spacer(Modifier.height(4.dp))
                    ActionButton("Transcribir", TanTurquesa, onTranscribe) { StopSquareSmall() }
                    ActionButton("Grabar de nuevo", TanNaranja, onRecordAgain) { MicIcon(size = 16) }
                }

                // Estado no diseñado en el Figma: diseño mínimo propio.
                RecordUiState.PermissionDenied -> Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    BigCircle(TanNaranja.copy(alpha = 0.4f), onClick = null) { MicIcon() }
                    Text(
                        "Necesitamos acceso al micrófono para grabar",
                        fontSize = 13.sp,
                        color = TanTexto,
                        textAlign = TextAlign.Center,
                    )
                    ActionButton("Dar permiso", TanNaranja, onStart) {}
                }
            }
        }
    }
}

private fun formatTime(totalSeconds: Int): String =
    "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)

// Círculo grande con sombra; si onClick es null no es tocable.
@Composable
private fun BigCircle(
    color: Color,
    onClick: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    val base = Modifier
        .size(112.dp)
        .shadow(8.dp, CircleShape)
        .background(color, CircleShape)
    Box(
        modifier = if (onClick != null) base.clickable(onClick = onClick) else base,
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun MicIcon(size: Int = 56) {
    Icon(
        painter = painterResource(R.drawable.ic_mic),
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier.size(size.dp),
    )
}

@Composable
private fun StopSquare() {
    Box(Modifier.size(36.dp).background(Color.White, RoundedCornerShape(4.dp)))
}

@Composable
private fun StopSquareSmall() {
    Box(Modifier.size(14.dp).background(Color.White, RoundedCornerShape(2.dp)))
}

@Composable
private fun ActionButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        modifier = Modifier.width(210.dp).height(44.dp),
    ) {
        icon()
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 13.sp, color = Color.White)
    }
}

// ---------- Previews: un frame del Figma por estado ----------

@Preview(showBackground = true)
@Composable
private fun RecordIdlePreview() = TANTheme {
    RecordContent(RecordUiState.Idle, {}, {}, {}, {}, {})
}

@Preview(showBackground = true)
@Composable
private fun RecordRecordingPreview() = TANTheme {
    RecordContent(RecordUiState.Recording(10), {}, {}, {}, {}, {})
}

@Preview(showBackground = true)
@Composable
private fun RecordSavedPreview() = TANTheme {
    RecordContent(RecordUiState.Saved, {}, {}, {}, {}, {})
}

@Preview(showBackground = true)
@Composable
private fun RecordPermissionPreview() = TANTheme {
    RecordContent(RecordUiState.PermissionDenied, {}, {}, {}, {}, {})
}