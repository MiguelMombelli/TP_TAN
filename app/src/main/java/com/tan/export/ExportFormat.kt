package com.tan.export

/** Formatos en los que se puede exportar una transcripción. */
enum class ExportFormat(val extension: String, val mimeType: String) {
    TXT("txt", "text/plain"),
    SRT("srt", "application/x-subrip")
}
