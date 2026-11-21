package com.tan.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Streaming

/** Único endpoint remoto de la app: bajar el archivo de un modelo de Whisper desde Hugging Face. */
interface ModelDownloadApi {

    // @Streaming: el cuerpo se lee por partes a medida que llega. Sin esto Retrofit intentaría
    // cargar el archivo entero en memoria (hasta 1,5 GB) antes de devolverlo.
    @Streaming
    @GET("{fileName}")
    suspend fun download(@Path("fileName") fileName: String): Response<ResponseBody>

    companion object {
        const val BASE_URL = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/"
    }
}
