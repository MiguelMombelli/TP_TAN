package com.tan.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tan.data.local.dao.TranscriptionDao
import com.tan.data.local.entity.SegmentEntity
import com.tan.data.local.entity.TranscriptionEntity

@Database(
    entities = [TranscriptionEntity::class, SegmentEntity::class],
    version = 1,
    exportSchema = true
)
abstract class TanDatabase : RoomDatabase() {
    abstract fun transcriptionDao(): TranscriptionDao
}
