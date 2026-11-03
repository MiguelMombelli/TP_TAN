package com.tan.export

import com.tan.domain.model.Segment

/**
 * Exportación de una transcripción a texto plano (.txt) y subtítulos (.srt).
 */
object TranscriptExporter {

    /** Une el texto de los segmentos, uno por línea.*/
    fun toTxt(segments: List<Segment>): String =
        segments
            .map { it.text.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(separator = "\n")

    /**
     * Genera un .srt válido. Los segmentos vacíos se descartan y la numeración
     * se hace sobre los que quedan
     */
    fun toSrt(segments: List<Segment>): String =
        segments
            .filter { it.text.isNotBlank() }
            .mapIndexed { index, segment ->
                val start = segment.startMs.coerceAtLeast(0L)
                val end = segment.endMs.coerceAtLeast(start)
                buildString {
                    append(index + 1).append('\n')
                    append(formatSrtTime(start))
                    append(" --> ")
                    append(formatSrtTime(end)).append('\n')
                    append(cleanSrtText(segment.text))
                    append('\n')
                }
            }
            .joinToString(separator = "\n")

    /** En SRT una línea en blanco cierra el bloque: se colapsan para no romper el archivo. */
    private fun cleanSrtText(text: String): String =
        text.trim().lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n")
}
