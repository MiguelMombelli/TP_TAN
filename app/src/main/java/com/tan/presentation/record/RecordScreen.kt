package com.tan.presentation.record

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
    /** Recibe la ruta del WAV grabado (temporal, lo borra la transcripción al terminar bien). */
    onTranscribe: (String) -> Unit,
    viewModel: RecordViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Pide el micrófono (decisivo) y, desde Android 13, el permiso de notificaciones: sin él la
    // grabación igual funciona, solo que no se ve el aviso.
    // Tras negarlo dos veces Android deja de mostrar el diálogo: el único camino es Ajustes.
    var needsSettings by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results[Manifest.permission.RECORD_AUDIO] == true
        if (!granted) {
            val activity = context.findActivity()
            needsSettings = activity != null &&
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.RECORD_AUDIO)
        }
        viewModel.onPermissionResult(granted)
    }
    val onStart: () -> Unit = {
        val micGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (micGranted) {
            viewModel.onPermissionResult(true)
        } else if (needsSettings) {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            )
        } else {
            val permissions = buildList {
                add(Manifest.permission.RECORD_AUDIO)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
            }
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    RecordContent(
        state = state,
        onBack = onBack,
        onStart = onStart,
        onStop = viewModel::onStop,
        onRecordAgain = viewModel::onRecordAgain,
        onTranscribe = { (state as? RecordUiState.Saved)?.let { onTranscribe(it.wavPath) } },
    )
}

// El contexto de Compose puede venir envuelto; hay que llegar hasta la Activity.
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
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

                is RecordUiState.Saved -> Column(
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

                // Estados no diseñados en el Figma: diseño mínimo propio.
                is RecordUiState.Failed -> Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    BigCircle(TanGrabando.copy(alpha = 0.4f), onClick = null) { MicIcon() }
                    Text(
                        state.message,
                        fontSize = 13.sp,
                        color = TanTexto,
                        textAlign = TextAlign.Center,
                    )
                    ActionButton("Reintentar", TanNaranja, onRecordAgain) {}
                }

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
    RecordContent(RecordUiState.Saved(""), {}, {}, {}, {}, {})
}

@Preview(showBackground = true)
@Composable
private fun RecordPermissionPreview() = TANTheme {
    RecordContent(RecordUiState.PermissionDenied, {}, {}, {}, {}, {})
}

@Preview(showBackground = true)
@Composable
private fun RecordFailedPreview() = TANTheme {
    RecordContent(RecordUiState.Failed("No se pudo acceder al micrófono."), {}, {}, {}, {}, {})
}