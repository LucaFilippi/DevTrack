package com.filippi.devtrack.data.repository

import com.filippi.devtrack.data.local.dao.ProjectDao
import com.filippi.devtrack.data.local.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalProjectRepositoryTest {
    @Test fun rejectsBlankProjectName() = runBlocking {
        val dao = FakeDao()
        val repository = LocalProjectRepository(dao)
        try {
            repository.add(ProjectEntity(name = "  "))
            throw AssertionError("Expected blank name to be rejected")
        } catch (_: IllegalArgumentException) {
            assertEquals(0, dao.insertions)
        }
    }

    private class FakeDao : ProjectDao {
        var insertions = 0
        override fun observeProjects(): Flow<List<ProjectEntity>> = flowOf(emptyList())
        override suspend fun getById(id: String): ProjectEntity? = null
        override suspend fun insert(project: ProjectEntity) { insertions++ }
    }
}
