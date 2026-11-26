package com.tan.presentation.events

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first

/**
 * Emite un evento de un solo uso recién cuando hay alguien escuchando.
 *
 * Un SharedFlow con replay = 0 descarta lo que se emite sin receptor (por ejemplo, mientras la
 * pantalla está apagada o se está recreando). Acá se espera a que la pantalla se suscriba, así el
 * aviso no se pierde y tampoco queda guardado para repetirse después.
 */
suspend fun <T> MutableSharedFlow<T>.emitWhenObserved(event: T) {
    subscriptionCount.first { it > 0 }
    emit(event)
}
