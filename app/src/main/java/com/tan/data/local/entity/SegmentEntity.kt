package com.tan.data.local.entity

import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "segments",
    foreignKeys = [
        ForeignKey(
            entity = TranscriptionEntity::class,
            parentColumns = ["id"],
            childColumns = ["transcriptionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("transcriptionId")]
)
data class SegmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transcriptionId: Long,
    /** Orden del segmento dentro de la transcripción (0, 1, 2...). */
    val position: Int,
    val startMs: Long,
    val endMs: Long,
    val text: String
)
