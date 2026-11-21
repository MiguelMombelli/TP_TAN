package com.tan.di

import android.content.Context
import androidx.room.Room
import com.tan.data.audio.ForegroundAudioRecorder
import com.tan.data.engine.ForegroundTranscriptionKeepAlive
import com.tan.data.engine.WhisperEngine
import com.tan.data.local.TanDatabase
import com.tan.data.local.dao.TranscriptionDao
import com.tan.data.repository.DataStorePreferencesRepository
import com.tan.data.repository.FileModelRepository
import com.tan.data.repository.RoomTranscriptionRepository
import com.tan.domain.audio.AudioRecorder
import com.tan.domain.engine.TranscriptionEngine
import com.tan.domain.engine.TranscriptionKeepAlive
import com.tan.domain.repository.ModelRepository
import com.tan.domain.repository.PreferencesRepository
import com.tan.domain.repository.TranscriptionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindTranscriptionRepository(impl: RoomTranscriptionRepository): TranscriptionRepository

    @Binds
    @Singleton
    abstract fun bindTranscriptionEngine(impl: WhisperEngine): TranscriptionEngine

    @Binds
    @Singleton
    abstract fun bindTranscriptionKeepAlive(impl: ForegroundTranscriptionKeepAlive): TranscriptionKeepAlive

    @Binds
    @Singleton
    abstract fun bindAudioRecorder(impl: ForegroundAudioRecorder): AudioRecorder

    @Binds
    @Singleton
    abstract fun bindModelRepository(impl: FileModelRepository): ModelRepository

    @Binds
    @Singleton
    abstract fun bindPreferencesRepository(impl: DataStorePreferencesRepository): PreferencesRepository

    companion object {
        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): TanDatabase =
            Room.databaseBuilder(context, TanDatabase::class.java, "tan.db").build()

        @Provides
        fun provideTranscriptionDao(db: TanDatabase): TranscriptionDao = db.transcriptionDao()
    }
}
