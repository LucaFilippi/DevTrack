package com.filippi.devtrack.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.filippi.devtrack.data.local.database.AppDatabase
import com.filippi.devtrack.data.local.entity.GoalEntity
import com.filippi.devtrack.data.local.entity.ProjectEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupServiceTest {
    private lateinit var database: AppDatabase
    private lateinit var service: BackupService

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        service = BackupService(context.contentResolver, database)
    }

    @After fun tearDown() = database.close()

    @Test fun restoreReplacesExistingProjectsAndGoals() = runBlocking {
        database.projectDao().insert(ProjectEntity(id = "old", name = "Old"))
        val project = ProjectEntity(id = "new", name = "New", classification = "Portfolio")
        val goal = GoalEntity(id = "goal", projectId = project.id, name = "Goal", description = "", deadlineAt = 1)

        service.restore(BackupDocument(1, "DevTrack", java.time.Instant.now(), listOf(project), listOf(goal)))

        assertEquals(listOf(project), database.projectDao().getAll())
        assertEquals(listOf(goal), database.goalDao().getAll())
    }

    @Test fun invalidBackupIsRejectedBeforeRestoreAndExistingDataRemains() = runBlocking {
        val existing = ProjectEntity(id = "existing", name = "Keep me")
        database.projectDao().insert(existing)

        try {
            BackupCodec.decode("{\"formatVersion\":1,\"app\":\"DevTrack\",\"projects\":[]}")
            throw AssertionError("Expected invalid backup")
        } catch (_: BackupFormatException) {
            assertEquals(listOf(existing), database.projectDao().getAll())
            assertTrue(database.goalDao().getAll().isEmpty())
        }
    }
}
