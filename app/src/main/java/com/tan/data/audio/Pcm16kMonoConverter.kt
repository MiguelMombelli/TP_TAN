package com.tan.data.audio

import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Convierte audio PCM de 16 bits (cualquier frecuencia, mono o estéreo) al formato que necesita
 * Whisper: mono a 16 kHz. Trabaja "en streaming": se le pasan bloques a medida que el decodificador
 * los entrega, así un audio de horas nunca está entero en memoria.
 *
 * Mono: promedia los canales. Frecuencia: interpolación lineal entre muestras vecinas.
 * Limitación conocida: no filtra antes de bajar la frecuencia (un 48 kHz → 16 kHz puede meter algo
 * de ruido agudo); para voz, que es lo que transcribe Whisper, no afecta al texto.
 */
class Pcm16kMonoConverter(inRate: Int, private val channels: Int) {

    // Cuántas muestras de entrada avanzamos por cada muestra de salida (3.0 para 48 kHz).
    private val step = inRate.toDouble() / TARGET_RATE

    // Posición de la próxima muestra de salida, medida en muestras de entrada desde el inicio del
    // bloque actual. Puede ser negativa (entre -1 y 0): cae entre la última muestra del bloque
    // anterior y la primera de este.
    private var pos = 0.0
    private var prev = 0f // última muestra (ya en mono) del bloque anterior

    private var mono = FloatArray(0)
    private var out = ByteArray(0)

    /**
     * [samples] trae [count] muestras intercaladas (L, R, L, R… si es estéreo). Por cada tanda de
     * salida llama a [sink] con bytes PCM 16 bits little-endian: (buffer, cantidad de bytes válidos).
     * El buffer se reutiliza: hay que consumirlo antes de que [sink] retorne.
     */
    fun convert(samples: ShortArray, count: Int, sink: (ByteArray, Int) -> Unit) {
        val frames = count / channels
        if (frames == 0) return

        if (mono.size < frames) mono = FloatArray(frames)
        for (f in 0 until frames) {
            var sum = 0f
            for (c in 0 until channels) sum += samples[f * channels + c]
            mono[f] = sum / channels
        }

        val maxOut = (frames / step).toInt() + 2
        if (out.size < maxOut * 2) out = ByteArray(maxOut * 2)

        var o = 0
        while (pos < frames - 1) {
            val i = floor(pos).toInt()
            val a = if (i < 0) prev else mono[i]
            val b = mono[i + 1]
            val value = (a + (b - a) * (pos - i).toFloat()).roundToInt().coerceIn(-32768, 32767)
            out[o++] = value.toByte()
            out[o++] = (value shr 8).toByte()
            pos += step
        }
        pos -= frames
        prev = mono[frames - 1]

        if (o > 0) sink(out, o)
    }

    companion object {
        const val TARGET_RATE = 16_000
    }
}
