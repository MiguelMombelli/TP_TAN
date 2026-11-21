package com.tan.data.audio

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.ByteBuffer
import java.nio.ByteOrder

class WavWriterTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun header(bytes: ByteArray): ByteBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

    @Test
    fun alCerrarLaCabeceraTraeLosTamañosReales() {
        val file = folder.newFile("grabacion.wav")
        // 100 ms de audio a 16 kHz mono 16 bits = 3200 bytes
        WavWriter(file).use { it.write(ByteArray(3200), 0, 3200) }

        val bytes = file.readBytes()
        val h = header(bytes)
        assertEquals(WavWriter.HEADER_SIZE + 3200, bytes.size)
        assertEquals(36 + 3200, h.getInt(4))   // tamaño del resto del archivo
        assertEquals(1, h.getShort(22).toInt()) // mono
        assertEquals(16_000, h.getInt(24))      // frecuencia
        assertEquals(32_000, h.getInt(28))      // bytes por segundo
        assertEquals(16, h.getShort(34).toInt()) // bits por muestra
        assertEquals(3200, h.getInt(40))        // tamaño de los datos
    }

    @Test
    fun variosBloquesSeSuman() {
        val file = folder.newFile("bloques.wav")
        WavWriter(file).use { wav ->
            repeat(10) { wav.write(ByteArray(3200), 0, 3200) }
        }
        assertEquals(32_000, header(file.readBytes()).getInt(40)) // 1 segundo exacto
    }

    @Test
    fun unaGrabacionVaciaEsUnWavValido() {
        val file = folder.newFile("vacio.wav")
        WavWriter(file).use { }

        val bytes = file.readBytes()
        assertEquals(WavWriter.HEADER_SIZE, bytes.size)
        assertEquals(0, header(bytes).getInt(40))
    }
}
