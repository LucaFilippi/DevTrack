package com.filippi.devtrack.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.filippi.devtrack.data.local.database.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var databaseName: String

    @Before fun setUp() {
        databaseName = "migration-${System.nanoTime()}.db"
    }

    @After fun tearDown() {
        context.deleteDatabase(databaseName)
    }

    @Test fun migrateFromVersion1To3PreservesExistingProject() = runBlocking {
        context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null).apply {
            execSQL(
                """
                CREATE TABLE projects (
                    id TEXT NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    description TEXT NOT NULL,
                    status TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    deadlineAt INTEGER
                )
                """.trimIndent()
            )
            execSQL("INSERT INTO projects VALUES ('existing', 'Existing', '', 'active', 10, 20, 3000)")
            version = 1
            close()
        }

        val database = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .addMigrations(AppDatabase.Migration1To2, AppDatabase.Migration2To3)
            .build()
        try {
            runBlocking {
                val project = requireNotNull(database.projectDao().getById("existing"))
                assertEquals("Existing", project.name)
                assertEquals(null, project.manualPriority)
                assertEquals(null, project.classification)
                assertEquals(0, database.goalDao().getAll().size)
            }
        } finally {
            database.close()
        }
    }
}
