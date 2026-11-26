package com.tan.data.engine

/**
 * Declaración de las funciones que están implementadas en C++ (app/src/main/cpp/whisper_jni.cpp).
 *
 * Cada `external fun` corresponde a una función C++ llamada
 * `Java_com_tan_data_engine_WhisperNative_<nombre>`: si se mueve esta clase de paquete o se renombra
 * una función, el C++ tiene que cambiar igual o la app se cae con UnsatisfiedLinkError.
 *
 * No usar directamente desde pantallas: usar TranscriptionEngine.
 * Los errores llegan como excepciones de com.tan.domain.engine.
 */
object WhisperNative {

    init {
        // Carga libwhisper_jni.so (la librería que arma CMake).
        System.loadLibrary("whisper_jni")
    }

    /** Versión de whisper.cpp y con qué instrucciones del procesador se compiló (NEON, DOTPROD...). */
    external fun nativeSystemInfo(): String

    external fun nativeLoadModel(modelPath: String)

    external fun nativeFreeModel()

    /** Pide cortar la transcripción en curso. Seguro de llamar desde cualquier hilo. */
    external fun nativeCancel()

    /** Bloquea hasta terminar. Llamar siempre desde un hilo de fondo. */
    external fun nativeTranscribe(
        wavPath: String,
        threads: Int,
        language: String,
        listener: ProgressListener?,
    ): Array<NativeSegment>
}
