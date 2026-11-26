package com.tan.data.audio

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.provider.OpenableColumns
import com.tan.domain.audio.AudioFileInfo
import com.tan.domain.audio.AudioImportException
import com.tan.domain.audio.AudioImporter
import com.tan.domain.audio.UnsupportedAudioException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.ByteOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Importa audio con las herramientas que trae Android, sin librerías de terceros:
 * [MediaExtractor] separa la pista de audio del contenedor (MP3, M4A, OGG/Opus, WAV, MP4…),
 * [MediaCodec] la decodifica a PCM y [Pcm16kMonoConverter] lo deja en 16 kHz mono para Whisper.
 * Todo se procesa por bloques y se escribe directo a un WAV temporal.
 */
@Singleton
class MediaCodecAudioImporter @Inject constructor(
    @ApplicationContext private val context: Context,
) : AudioImporter {

    override suspend fun inspect(uri: String): AudioFileInfo = withContext(Dispatchers.IO) {
        val parsed = Uri.parse(uri)
        val (name, size) = nameAndSize(parsed)
        val extractor = openExtractor(parsed)
        try {
            val format = extractor.getTrackFormat(audioTrack(extractor))
            val durationMs =
                if (format.containsKey(MediaFormat.KEY_DURATION)) format.getLong(MediaFormat.KEY_DURATION) / 1000 else 0L
            AudioFileInfo(name, size, durationMs)
        } finally {
            extractor.release()
        }
    }

    override suspend fun convertToWav(uri: String, onProgress: (Int) -> Unit): String =
        withContext(Dispatchers.IO) {
            val output = File(context.cacheDir, "import_${System.currentTimeMillis()}.wav")
            var done = false
            val extractor = openExtractor(Uri.parse(uri))
            var codec: MediaCodec? = null
            try {
                val track = audioTrack(extractor)
                val format = extractor.getTrackFormat(track)
                extractor.selectTrack(track)
                codec = createDecoder(format)
                decode(extractor, codec, format, output, onProgress)
                done = true
                output.absolutePath
            } catch (e: AudioImportException) {
                throw e
            } catch (e: UnsupportedAudioException) {
                throw e
            } catch (e: IOException) {
                throw AudioImportException("No se pudo guardar el audio. ¿Falta espacio en el dispositivo?", e)
            } catch (e: IllegalStateException) {
                throw AudioImportException("No se pudo decodificar el audio. El archivo puede estar dañado.", e)
            } finally {
                runCatching { codec?.stop() }
                codec?.release()
                extractor.release()
                if (!done) output.delete() // si falló o se canceló, no queda un WAV a medias
            }
        }

    // ---------- lectura del archivo ----------

    private fun openExtractor(uri: Uri): MediaExtractor {
        val extractor = MediaExtractor()
        try {
            val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
                ?: throw AudioImportException("No se pudo abrir el archivo.")
            // MediaExtractor se queda con su propia copia: se puede cerrar el descriptor al volver.
            descriptor.use { extractor.setDataSource(it.fileDescriptor) }
            return extractor
        } catch (e: AudioImportException) {
            extractor.release()
            throw e
        } catch (e: FileNotFoundException) {
            extractor.release()
            throw AudioImportException("No se pudo abrir el archivo.", e)
        } catch (e: SecurityException) {
            extractor.release()
            throw AudioImportException("No hay permiso para leer el archivo.", e)
        } catch (e: IOException) {
            extractor.release() // el contenedor no se reconoce: no es un audio o video válido
            throw UnsupportedAudioException()
        } catch (e: RuntimeException) {
            extractor.release()
            throw UnsupportedAudioException()
        }
    }

    private fun audioTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val mime = extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME).orEmpty()
            if (mime.startsWith("audio/")) return i
        }
        throw UnsupportedAudioException() // por ejemplo, un video sin pista de audio
    }

    private fun nameAndSize(uri: Uri): Pair<String, Long> {
        var name = "audio"
        var size = -1L
        runCatching {
            context.contentResolver.query(
                uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameColumn >= 0) cursor.getString(nameColumn)?.let { name = it }
                    val sizeColumn = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeColumn >= 0 && !cursor.isNull(sizeColumn)) size = cursor.getLong(sizeColumn)
                }
            }
        }
        return name to size
    }

    // ---------- decodificación ----------

    private fun createDecoder(format: MediaFormat): MediaCodec {
        val mime = format.getString(MediaFormat.KEY_MIME) ?: throw UnsupportedAudioException()
        val codec = try {
            MediaCodec.createDecoderByType(mime)
        } catch (e: Exception) {
            throw UnsupportedAudioException() // este celular no trae decodificador para ese códec
        }
        try {
            codec.configure(format, null, null, 0)
            codec.start()
        } catch (e: Exception) {
            codec.release()
            throw UnsupportedAudioException()
        }
        return codec
    }

    private fun newConverter(format: MediaFormat): Pcm16kMonoConverter {
        if (!format.containsKey(MediaFormat.KEY_SAMPLE_RATE) || !format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
            throw AudioImportException("No se pudo leer el formato del audio.")
        }
        return Pcm16kMonoConverter(
            inRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE),
            channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT).coerceAtLeast(1),
        )
    }

    /**
     * Bucle clásico de MediaCodec: se le dan al decodificador trozos comprimidos (entrada) y se
     * retiran trozos de PCM (salida). Las dos mitades avanzan por turnos hasta el fin del archivo.
     */
    private fun CoroutineScope.decode(
        extractor: MediaExtractor,
        codec: MediaCodec,
        inputFormat: MediaFormat,
        output: File,
        onProgress: (Int) -> Unit,
    ) {
        val durationUs =
            if (inputFormat.containsKey(MediaFormat.KEY_DURATION)) inputFormat.getLong(MediaFormat.KEY_DURATION) else 0L
        var converter = newConverter(inputFormat)
        var floatPcm = false
        var shorts = ShortArray(0)
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false
        var lastPercent = -1
        var idleLoops = 0
        var writtenBytes = 0L

        WavWriter(output).use { wav ->
            while (!outputDone) {
                ensureActive() // permite cancelar la conversión en cualquier momento
                var moved = false

                if (!inputDone) {
                    val index = codec.dequeueInputBuffer(TIMEOUT_US)
                    if (index >= 0) {
                        moved = true
                        val size = extractor.readSampleData(codec.getInputBuffer(index)!!, 0)
                        if (size < 0) {
                            codec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            val timeUs = extractor.sampleTime
                            codec.queueInputBuffer(index, 0, size, timeUs, 0)
                            extractor.advance()
                            if (durationUs > 0) {
                                val percent = (timeUs * 100 / durationUs).toInt().coerceIn(0, 100)
                                if (percent != lastPercent) {
                                    lastPercent = percent
                                    onProgress(percent)
                                }
                            }
                        }
                    }
                }

                val outIndex = codec.dequeueOutputBuffer(info, TIMEOUT_US)
                when {
                    outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        // El decodificador avisa la frecuencia y canales reales de lo que entrega.
                        val format = codec.outputFormat
                        converter = newConverter(format)
                        floatPcm = format.containsKey(MediaFormat.KEY_PCM_ENCODING) &&
                            format.getInteger(MediaFormat.KEY_PCM_ENCODING) == AudioFormat.ENCODING_PCM_FLOAT
                        moved = true
                    }
                    outIndex >= 0 -> {
                        moved = true
                        if (info.size > 0) {
                            val buffer = codec.getOutputBuffer(outIndex)!!
                            buffer.position(info.offset)
                            buffer.limit(info.offset + info.size)
                            buffer.order(ByteOrder.LITTLE_ENDIAN)
                            val count: Int
                            if (floatPcm) {
                                val floats = buffer.asFloatBuffer()
                                count = floats.remaining()
                                if (shorts.size < count) shorts = ShortArray(count)
                                for (k in 0 until count) {
                                    shorts[k] = (floats.get() * 32767f).roundToInt().coerceIn(-32768, 32767).toShort()
                                }
                            } else {
                                val pcm = buffer.asShortBuffer()
                                count = pcm.remaining()
                                if (shorts.size < count) shorts = ShortArray(count)
                                pcm.get(shorts, 0, count)
                            }
                            converter.convert(shorts, count) { bytes, length ->
                                wav.write(bytes, 0, length)
                                writtenBytes += length
                            }
                        }
                        codec.releaseOutputBuffer(outIndex, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                    }
                }

                // Si el decodificador se cuelga, no quedamos girando para siempre.
                idleLoops = if (moved) 0 else idleLoops + 1
                if (idleLoops > MAX_IDLE_LOOPS) throw AudioImportException("El decodificador dejó de responder.")
            }
        }
        if (writtenBytes == 0L) throw AudioImportException("No se pudo extraer audio del archivo.")
    }

    private companion object {
        const val TIMEOUT_US = 10_000L
        const val MAX_IDLE_LOOPS = 1_000 // ~10-20 s sin que entre ni salga nada
    }
}
