package com.tan.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transcriptions")
data class TranscriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val createdAtMs: Long,
    /** Nombre de TranscriptionSource (RECORDING o FILE). */
    val source: String,
    val model: String,
    val durationMs: Long,
    /** Texto corrido: sirve para el adelanto del Historial y, más adelante, para buscar. */
    val text: String
)
