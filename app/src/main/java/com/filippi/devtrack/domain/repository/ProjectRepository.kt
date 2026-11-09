package com.filippi.devtrack.domain.repository

import com.filippi.devtrack.data.local.entity.ProjectEntity
import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    val projects: Flow<List<ProjectEntity>>
    suspend fun getById(id: String): ProjectEntity?
    suspend fun add(project: ProjectEntity)
    suspend fun update(project: ProjectEntity)
    suspend fun delete(project: ProjectEntity)
}
