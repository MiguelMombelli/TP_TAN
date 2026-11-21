package com.tan.di

import com.tan.data.remote.ModelDownloadApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        // Tiempo máximo SIN recibir datos, no de la descarga completa (que puede tardar minutos).
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideModelDownloadApi(client: OkHttpClient): ModelDownloadApi =
        Retrofit.Builder()
            .baseUrl(ModelDownloadApi.BASE_URL)
            .client(client)
            .build() // sin conversor: el cuerpo se recibe crudo (ResponseBody)
            .create(ModelDownloadApi::class.java)
}
