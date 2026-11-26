package com.tan.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tan.ui.theme.TanTurquesa

/** Etiqueta chica (por ejemplo "Archivo", "Grabación" o el nombre del modelo). */
@Composable
fun TanChip(text: String, color: Color = TanTurquesa, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 11.sp,
        color = color,
        modifier = modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}
