package com.filippi.devtrack.data.repository

import com.filippi.devtrack.data.local.dao.ProjectDao
import com.filippi.devtrack.data.local.entity.ProjectEntity
import com.filippi.devtrack.domain.repository.ProjectRepository
import kotlinx.coroutines.flow.Flow

class LocalProjectRepository(private val dao: ProjectDao) : ProjectRepository {
    override val projects: Flow<List<ProjectEntity>> = dao.observeProjects()
    override suspend fun getById(id: String): ProjectEntity? = dao.getById(id)
    override suspend fun add(project: ProjectEntity) {
        require(project.name.isNotBlank()) { "Project name must not be blank" }
        dao.insert(project)
    }
}
