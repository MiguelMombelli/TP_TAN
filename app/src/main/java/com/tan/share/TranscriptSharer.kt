package com.tan.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.tan.domain.model.Segment
import com.tan.export.ExportFormat
import com.tan.export.TranscriptExporter
import com.tan.export.exportFileName
import java.io.File

/**
 * Comparte una transcripción como archivo TXT o SRT con la hoja del sistema.
 *
 * Privacidad: el archivo se crea en la carpeta de caché privada de la app y se entrega
 * con un FileProvider (nunca queda en almacenamiento compartido). Antes de crear uno
 * nuevo se borran los exportados anteriormente.
 */
object TranscriptSharer {

    private const val EXPORT_DIR = "exports"

    fun share(context: Context, title: String, segments: List<Segment>, format: ExportFormat) {
        val file = writeExportFile(context, title, segments, format)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

        val send = Intent(Intent.ACTION_SEND).apply {
            type = format.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            clipData = ClipData.newRawUri(file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Compartir ${format.extension.uppercase()}"))
    }

    private fun writeExportFile(
        context: Context,
        title: String,
        segments: List<Segment>,
        format: ExportFormat
    ): File {
        val dir = File(context.cacheDir, EXPORT_DIR).apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, exportFileName(title, format))
        file.writeText(TranscriptExporter.export(segments, format), Charsets.UTF_8)
        return file
    }
}
