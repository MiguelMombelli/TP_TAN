package com.tan.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tan.R
import com.tan.presentation.components.AccessCard
import com.tan.presentation.components.HeroRecordCard
import com.tan.presentation.components.SettingsRow

@Composable
fun HomeScreen(
    onRecord: () -> Unit,
    onImport: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("TAN", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Column {
                Text(
                    "¿Qué quieres hacer?",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Convierte tus audios en texto claro, listo para usar.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            HeroRecordCard(
                title = "Grabar audio",
                subtitle = "Transcripción en tiempo real",
                chip = "Pulsa para empezar",
                icon = painterResource(R.drawable.ic_mic),
                onClick = onRecord
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                AccessCard(
                    title = "Cargar archivo",
                    subtitle = "Audio o video",
                    icon = painterResource(R.drawable.ic_upload),
                    onClick = onImport,
                    modifier = Modifier.weight(1f)
                )
                AccessCard(
                    title = "Historial",
                    subtitle = "Tus transcripciones",
                    icon = painterResource(R.drawable.ic_clock),
                    onClick = onHistory,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.weight(1f))
            SettingsRow("Configuración", onSettings)
        }
    }
}