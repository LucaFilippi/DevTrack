package com.filippi.devtrack.data.repository

import androidx.room.withTransaction
import com.filippi.devtrack.data.local.database.AppDatabase
import com.filippi.devtrack.data.local.entity.GoalEntity
import com.filippi.devtrack.data.local.entity.ProjectEntity

class GoalDeadlineException : IllegalArgumentException()
class ProjectDeadlineException : IllegalArgumentException()

class GoalRepository(private val database: AppDatabase) {
    private val goals = database.goalDao()
    val all = goals.observeGoals()

    suspend fun save(goal: GoalEntity, isNew: Boolean) = database.withTransaction {
        require(goal.name.isNotBlank() && goal.description.isNotBlank())
        val projectDeadline = database.projectDao().getById(goal.projectId)?.deadlineAt
        if (projectDeadline == null || goal.deadlineAt >= projectDeadline) {
            throw GoalDeadlineException()
        }
        if (isNew) goals.insert(goal) else check(goals.update(goal) == 1)
    }

    suspend fun updateProject(project: ProjectEntity, repository: LocalProjectRepository) =
        database.withTransaction {
            val latest = goals.latestDeadline(project.id)
            if (latest != null && (project.deadlineAt == null || project.deadlineAt <= latest)) {
                throw ProjectDeadlineException()
            }
            repository.update(project)
        }

    suspend fun delete(goal: GoalEntity) { goals.delete(goal) }
}
