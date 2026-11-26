package com.tan.domain.engine

/**
 * Mantiene viva la app mientras hay un trabajo largo (transcribir, convertir un audio, descargar
 * un modelo): que Android no la mate ni duerma el CPU si el usuario apaga la pantalla o cambia de
 * app. Cada [start] se empareja con un [stop]; el servicio se detiene cuando terminan todos.
 * Es una interfaz para que el ViewModel no dependa de
 * nada de Android (y se pueda reemplazar por un falso en los tests).
 */
interface TranscriptionKeepAlive {
    fun start()
    fun stop()
}
