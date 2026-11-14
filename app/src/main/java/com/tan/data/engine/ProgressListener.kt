package com.tan.data.engine

/** El C++ llama a [onProgress] por nombre y firma (`onProgress(I)V`) desde el hilo que transcribe. */
fun interface ProgressListener {
    fun onProgress(percent: Int)
}
