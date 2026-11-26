package com.tan.export

private const val MAX_BASE_LENGTH = 80
private const val DEFAULT_BASE = "transcripcion"

private val INVALID_CHARS = Regex("""[\\*?"<>|]""")
private val WHITESPACE = Regex("""\s+""")

/**
 * Arma un nombre de archivo seguro a partir del título de la transcripción.
 */
fun exportFileName(title: String, format: ExportFormat): String {
    val base = title
        .trim()
        .replace('/', '-')
        .replace(':', '-')
        .replace(INVALID_CHARS, "")
        .replace(WHITESPACE, "_")
        .trim('_', '.', '-')
        .take(MAX_BASE_LENGTH)
        .trim('_', '.', '-')
        .ifEmpty { DEFAULT_BASE }
    return "$base.${format.extension}"
}
