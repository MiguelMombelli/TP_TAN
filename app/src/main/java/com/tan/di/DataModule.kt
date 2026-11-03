package com.tan.di

import android.content.Context
import androidx.room.Room
import com.tan.data.local.TanDatabase
import com.tan.data.local.dao.TranscriptionDao
import com.tan.data.repository.RoomTranscriptionRepository
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

    companion object {
        @Provides
        @Singleton
        fun provideDatabase(@ApplicationContext context: Context): TanDatabase =
            Room.databaseBuilder(context, TanDatabase::class.java, "tan.db").build()

        @Provides
        fun provideTranscriptionDao(db: TanDatabase): TranscriptionDao = db.transcriptionDao()
    }
}
