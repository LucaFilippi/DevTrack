package com.filippi.devtrack.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.filippi.devtrack.data.local.dao.ProjectDao
import com.filippi.devtrack.data.local.entity.ProjectEntity

@Database(entities = [ProjectEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
}
