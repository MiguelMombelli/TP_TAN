package com.tan.domain.engine

/**
 * Mantiene viva la app mientras se transcribe: que Android no la mate ni duerma el CPU si el
 * usuario apaga la pantalla o cambia de app. Es una interfaz para que el ViewModel no dependa de
 * nada de Android (y se pueda reemplazar por un falso en los tests).
 */
interface TranscriptionKeepAlive {
    fun start()
    fun stop()
}
