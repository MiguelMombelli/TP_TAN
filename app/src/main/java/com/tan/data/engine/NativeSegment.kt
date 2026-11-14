package com.tan.data.engine

/**
 * Segmento tal como lo arma el código C++ (whisper_jni.cpp lo construye por nombre y firma:
 * `(JJLjava/lang/String;)V`). No usar fuera de la capa data: se convierte a Segment de dominio.
 * Si se cambian los parámetros del constructor hay que actualizar el JNI.
 */
class NativeSegment(
    @JvmField val startMs: Long,
    @JvmField val endMs: Long,
    @JvmField val text: String,
)
