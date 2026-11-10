package com.filippi.devtrack.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.filippi.devtrack.data.local.database.AppDatabase
import com.filippi.devtrack.data.local.entity.GoalEntity
import com.filippi.devtrack.data.local.entity.ProjectEntity
import com.filippi.devtrack.data.repository.GoalDeadlineException
import com.filippi.devtrack.data.repository.GoalRepository
import com.filippi.devtrack.data.repository.LocalProjectRepository
import com.filippi.devtrack.data.repository.ProjectDeadlineException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GoalRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test fun validatesDeadlinesAndKeepsMultipleGoalsPerProject() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val projects = LocalProjectRepository(database.projectDao())
            val goals = GoalRepository(database)
            val project = ProjectEntity(name = "Project", deadlineAt = 3_000)
            val undated = ProjectEntity(name = "Undated")
            projects.add(project)
            projects.add(undated)
            val goal = GoalEntity(projectId = project.id, name = "Goal", description = "Description", deadlineAt = 1_000)
            goals.save(goal, isNew = true)
            goals.save(goal.copy(id = "second", deadlineAt = 2_000), isNew = true)

            for (invalid in listOf(
                goal.copy(deadlineAt = 3_000),
                goal.copy(deadlineAt = 4_000),
                goal.copy(projectId = undated.id),
                goal.copy(projectId = "missing")
            )) {
                val failure = runCatching { goals.save(invalid, isNew = false) }.exceptionOrNull()
                assertTrue(failure is GoalDeadlineException)
            }
            for (deadline in listOf(null, 2_000L, 1_000L)) {
                val failure = runCatching {
                    goals.updateProject(project.copy(deadlineAt = deadline), projects)
                }.exceptionOrNull()
                assertTrue(failure is ProjectDeadlineException)
            }
            goals.updateProject(project.copy(deadlineAt = 2_500), projects)
            assertEquals(2_500L, projects.getById(project.id)?.deadlineAt)
            assertEquals(2, goals.all.first().size)
            projects.delete(project)
            assertTrue(goals.all.first().isEmpty())
        } finally {
            database.close()
        }
    }

    @Test fun migratesExistingProjectsAndCanSaveGoals() = runBlocking {
        val name = "goal-migration-${System.nanoTime()}.db"
        try {
            context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { old ->
                old.execSQL(
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
                old.execSQL("INSERT INTO projects VALUES ('existing', 'Existing project', '', 'active', 0, 0, 3000)")
                old.version = 1
            }
            val database = Room.databaseBuilder(context, AppDatabase::class.java, name)
                .addMigrations(AppDatabase.Migration1To2, AppDatabase.Migration2To3).build()
            try {
                val project = database.projectDao().getById("existing")
                assertEquals("Existing project", project?.name)
                assertEquals(null, project?.manualPriority)
                assertEquals(null, project?.classification)
                val goals = GoalRepository(database)
                goals.save(GoalEntity(projectId = "existing", name = "Goal",
                    description = "Description", deadlineAt = 1_000), isNew = true)
                assertEquals("existing", goals.all.first().single().projectId)
                val updated = requireNotNull(project).copy(manualPriority = "high", classification = "Portfolio")
                goals.updateProject(updated, LocalProjectRepository(database.projectDao()))
                assertEquals(updated, database.projectDao().getById("existing"))
            } finally {
                database.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
        Unit
    }
}
