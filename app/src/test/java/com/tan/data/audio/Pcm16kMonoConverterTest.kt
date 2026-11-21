package com.tan.data.audio

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.io.ByteArrayOutputStream
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import org.junit.Test

class Pcm16kMonoConverterTest {

    /** Seno de [freq] Hz de 10000 de amplitud, pasado por el conversor en bloques de [chunk] cuadros. */
    private fun convert(rate: Int, channels: Int, frames: Int, chunk: Int, freq: Double = 440.0): ShortArray {
        val input = ShortArray(frames * channels)
        for (f in 0 until frames) for (c in 0 until channels) {
            input[f * channels + c] = (10_000 * sin(2 * PI * freq * f / rate)).toInt().toShort()
        }
        val converter = Pcm16kMonoConverter(rate, channels)
        val out = ByteArrayOutputStream()
        var start = 0
        while (start < frames) {
            val n = minOf(chunk, frames - start)
            converter.convert(input.copyOfRange(start * channels, (start + n) * channels), n * channels) { bytes, length ->
                out.write(bytes, 0, length)
            }
            start += n
        }
        val bytes = out.toByteArray()
        return ShortArray(bytes.size / 2) {
            ((bytes[2 * it].toInt() and 0xFF) or (bytes[2 * it + 1].toInt() shl 8)).toShort()
        }
    }

    private fun zeroCrossings(s: ShortArray) = (1 until s.size).count { (s[it - 1] < 0) != (s[it] < 0) }

    @Test
    fun unSegundoSiempreDaCercaDe16000Muestras() {
        assertEquals(16_000.0, convert(16_000, 1, 16_000, 1_000).size.toDouble(), 2.0)
        assertEquals(16_000.0, convert(48_000, 2, 48_000, 4_800).size.toDouble(), 2.0)
        assertEquals(16_000.0, convert(44_100, 1, 44_100, 4_096).size.toDouble(), 2.0)
        assertEquals(16_000.0, convert(8_000, 1, 8_000, 500).size.toDouble(), 4.0)
    }

    @Test
    fun conservaLaFrecuenciaYLaAmplitud() {
        val out = convert(48_000, 2, 48_000, 48_000)
        // 440 Hz durante 1 s cruzan el cero ~880 veces
        assertEquals(880.0, zeroCrossings(out).toDouble(), 10.0)
        assertEquals(10_000.0, out.maxOf { abs(it.toInt()) }.toDouble(), 200.0)
    }

    @Test
    fun partirElAudioEnBloquesNoCambiaElResultado() {
        val entero = convert(48_000, 2, 48_000, 48_000)
        assertArrayEquals(entero, convert(48_000, 2, 48_000, 1))
        assertArrayEquals(entero, convert(48_000, 2, 48_000, 7))
        assertArrayEquals(entero, convert(48_000, 2, 48_000, 1_234))
    }

    @Test
    fun losCanalesSePromedianAMono() {
        val converter = Pcm16kMonoConverter(16_000, 2)
        var suma = 0L
        // L = -R: al promediar queda silencio
        converter.convert(shortArrayOf(1000, -1000, 2000, -2000, 3000, -3000), 6) { bytes, length ->
            for (i in 0 until length) suma += abs(bytes[i].toInt())
        }
        assertTrue(suma == 0L)
    }
}
