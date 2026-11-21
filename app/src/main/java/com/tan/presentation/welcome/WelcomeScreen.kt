package com.tan.presentation.welcome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

/**
 * PROVISORIA: solo sirve para probar el flujo "bienvenida solo la primera vez".
 * Paola la reemplaza con el diseño de Figma; lo único que tiene que conservar es
 * llamar a viewModel.onContinue() antes de salir de la pantalla.
 */
@Composable
fun WelcomeScreen(
    onChooseModel: () -> Unit,
    viewModel: WelcomeViewModel = hiltViewModel(),
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Bienvenido a TAN", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(
            text = "Transcribí audio sin conexión. Para empezar, descargá un modelo.",
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        Button(onClick = {
            viewModel.onContinue()
            onChooseModel()
        }) {
            Text("Elegir modelo")
        }
    }
}
