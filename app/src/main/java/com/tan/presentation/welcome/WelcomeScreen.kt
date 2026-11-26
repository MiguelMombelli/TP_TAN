package com.tan.presentation.welcome

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.tan.R
import com.tan.presentation.components.TanButton
import com.tan.ui.theme.TANTheme
import com.tan.ui.theme.TanFondo
import com.tan.ui.theme.TanNaranja
import com.tan.ui.theme.TanTexto
import com.tan.ui.theme.TanTurquesa

/**
 * Bienvenida: se muestra solo la primera vez. Explica qué hace la app y avisa que hace falta
 * internet una sola vez, para descargar el modelo. Al continuar se marca como vista.
 */
@Composable
fun WelcomeScreen(
    onChooseModel: () -> Unit,
    viewModel: WelcomeViewModel = hiltViewModel(),
) {
    WelcomeContent(
        onChooseModel = {
            viewModel.onContinue()
            onChooseModel()
        },
    )
}

@Composable
fun WelcomeContent(onChooseModel: () -> Unit) {
    Scaffold(containerColor = TanFondo) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()) // en pantallas chicas o con letra grande
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))
            Image(
                painter = painterResource(R.drawable.tan_icon),
                contentDescription = "Logo de TAN",
                modifier = Modifier
                    .size(132.dp)
                    .clip(RoundedCornerShape(32.dp)),
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Bienvenido a TAN",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TanTexto,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Pasá tus audios a texto directamente en tu celular.",
                fontSize = 15.sp,
                color = TanTexto,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(28.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                WelcomePoint(
                    icon = R.drawable.ic_mic,
                    title = "Grabá o importá",
                    text = "Grabaciones, audios de WhatsApp, MP3, M4A, WAV o videos MP4.",
                )
                WelcomePoint(
                    icon = R.drawable.ic_file,
                    title = "Privado y sin internet",
                    text = "La transcripción se hace en tu celular. El audio nunca sale del dispositivo.",
                )
                WelcomePoint(
                    icon = R.drawable.ic_upload,
                    iconRotation = 180f, // la flecha de subir, dada vuelta, indica descarga
                    title = "Necesitás conexión una sola vez",
                    text = "Para descargar el modelo que transcribe (desde 75 MB). Te recomendamos usar " +
                        "Wi-Fi. Después, TAN funciona incluso en modo avión.",
                    highlighted = true,
                )
            }

            Spacer(Modifier.height(32.dp))
            TanButton(text = "Elegir modelo", onClick = onChooseModel)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun WelcomePoint(
    @DrawableRes icon: Int,
    title: String,
    text: String,
    iconRotation: Float = 0f,
    highlighted: Boolean = false,
) {
    // El aviso de conexión va resaltado en naranja: es lo único que el usuario tiene que hacer antes.
    val accent = if (highlighted) TanNaranja else TanTurquesa
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (highlighted) TanNaranja.copy(alpha = 0.12f) else Color.White,
                shape = RoundedCornerShape(14.dp),
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(accent.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null, // decorativo: el título ya lo dice
                tint = accent,
                modifier = Modifier
                    .size(22.dp)
                    .rotate(iconRotation),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TanTexto)
            Spacer(Modifier.height(2.dp))
            Text(text = text, fontSize = 13.sp, lineHeight = 18.sp, color = TanTexto)
        }
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun WelcomePreview() = TANTheme {
    WelcomeContent(onChooseModel = {})
}
