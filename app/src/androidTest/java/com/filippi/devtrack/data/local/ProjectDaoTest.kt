package com.filippi.devtrack.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.filippi.devtrack.data.local.database.AppDatabase
import com.filippi.devtrack.data.local.entity.ProjectEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProjectDaoTest {
    private lateinit var database: AppDatabase

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(), AppDatabase::class.java
        ).build()
    }

    @After fun tearDown() = database.close()

    @Test fun insertAndObserveProject() = runBlocking {
        val project = ProjectEntity(name = "Portfolio")
        assertNull(database.projectDao().getById(project.id))
        database.projectDao().insert(project)
        assertEquals(project, database.projectDao().getById(project.id))
        assertEquals(listOf(project), database.projectDao().observeProjects().first())
    }

    @Test fun editingArchivingAndDeletingUpdatesTheProjectList() = runBlocking {
        val dao = database.projectDao()
        val project = ProjectEntity(name = "Original")
        dao.insert(project)
        val archived = project.copy(name = "Renamed", status = "archived", deadlineAt = 1_800_000_000_000)
        assertEquals(1, dao.update(archived))
        assertEquals(listOf(archived), dao.observeProjects().first())
        assertEquals(1, dao.delete(archived))
        assertEquals(emptyList<ProjectEntity>(), dao.observeProjects().first())
    }
}
