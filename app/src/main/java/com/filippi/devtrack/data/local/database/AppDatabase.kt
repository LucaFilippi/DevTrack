package com.filippi.devtrack.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.filippi.devtrack.data.local.dao.GoalDao
import com.filippi.devtrack.data.local.dao.ProjectDao
import com.filippi.devtrack.data.local.entity.GoalEntity
import com.filippi.devtrack.data.local.entity.ProjectEntity

@Database(entities = [ProjectEntity::class, GoalEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun goalDao(): GoalDao

    companion object {
        val Migration2To3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE projects ADD COLUMN manualPriority TEXT")
                db.execSQL("ALTER TABLE projects ADD COLUMN classification TEXT")
            }
        }

        val Migration1To2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE goals (
                        id TEXT NOT NULL PRIMARY KEY,
                        projectId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL,
                        deadlineAt INTEGER NOT NULL,
                        completed INTEGER NOT NULL,
                        FOREIGN KEY(projectId) REFERENCES projects(id) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX index_goals_projectId ON goals(projectId)")
            }
        }
    }
}
