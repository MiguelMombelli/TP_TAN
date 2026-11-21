package com.tan.data.audio

import java.io.Closeable
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Escribe un WAV PCM 16 bits mono. No se sabe de antemano cuánto va a durar la grabación, así que
 * se escribe una cabecera provisoria y al cerrar se vuelve al principio a poner los tamaños reales.
 * Si el proceso muere antes de cerrar, queda un WAV con tamaños en 0 que el lector nativo sabe
 * recuperar usando el tamaño del archivo.
 */
class WavWriter(file: File, private val sampleRate: Int = 16_000) : Closeable {

    private val out = RandomAccessFile(file, "rw")
    private var dataBytes = 0L

    init {
        out.setLength(0)
        out.write(header(0))
    }

    fun write(buffer: ByteArray, offset: Int, length: Int) {
        out.write(buffer, offset, length)
        dataBytes += length
    }

    override fun close() {
        out.seek(0)
        out.write(header(dataBytes)) // ahora sí, con los tamaños reales
        out.close()
    }

    private fun header(dataSize: Long): ByteArray =
        ByteBuffer.allocate(HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt((36 + dataSize).toInt())      // tamaño del resto del archivo
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16)                           // tamaño del bloque fmt
            putShort(1)                          // formato 1 = PCM sin compresión
            putShort(1)                          // canales: mono
            putInt(sampleRate)
            putInt(sampleRate * 2)               // bytes por segundo (16 bits = 2 bytes por muestra)
            putShort(2)                          // bytes por muestra de todos los canales
            putShort(16)                         // bits por muestra
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(dataSize.toInt())
        }.array()

    companion object {
        const val HEADER_SIZE = 44
    }
}
