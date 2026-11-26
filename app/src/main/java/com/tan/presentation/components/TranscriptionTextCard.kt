package com.tan.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tan.ui.theme.TanTarjeta
import com.tan.ui.theme.TanTexto

/** Tarjeta con el texto de una transcripción, con scroll. La usan Resultado y Detalle. */
@Composable
fun TranscriptionTextCard(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(TanTarjeta, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            color = TanTexto,
            modifier = Modifier.verticalScroll(rememberScrollState())
        )
    }
}
