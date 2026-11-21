package com.tan.domain.model

/** Un modelo de Whisper descargable. [id] es lo que se guarda como "modelo activo". */
data class WhisperModel(
    val id: String,
    val sizeLabel: String,
    val speedLabel: String,
    val description: String,
) {
    /** Nombre del archivo, tal como lo publica whisper.cpp. */
    val fileName: String get() = "ggml-$id.bin"
}

/** Catálogo fijo de modelos que ofrece la app. */
object ModelCatalog {
    val all = listOf(
        WhisperModel("tiny", "75 MB", "⚡ Más rápido · Precisión básica", "Ideal para notas rápidas"),
        WhisperModel("base", "145 MB", "⚡ Rápido · Buena precisión", "Ideal para uso diario"),
        WhisperModel("small", "466 MB", "Lento · Alta precisión", "Ideal para reuniones y entrevistas"),
        WhisperModel("medium", "1500 MB", "Muy lento · Máxima precisión", "Ideal para máxima precisión"),
    )

    fun byId(id: String): WhisperModel? = all.firstOrNull { it.id == id }
}
