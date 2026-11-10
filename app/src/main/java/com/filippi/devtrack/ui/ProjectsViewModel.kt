package com.filippi.devtrack.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.filippi.devtrack.R
import com.filippi.devtrack.data.local.database.AppDatabase
import com.filippi.devtrack.data.local.entity.GoalEntity
import com.filippi.devtrack.data.local.entity.ProjectEntity
import com.filippi.devtrack.data.repository.LocalProjectRepository
import com.filippi.devtrack.data.repository.GoalDeadlineException
import com.filippi.devtrack.data.repository.GoalRepository
import com.filippi.devtrack.data.repository.ProjectDeadlineException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ProjectsState {
    data object Loading : ProjectsState
    data object Failed : ProjectsState
    data class Ready(val projects: List<ProjectEntity>) : ProjectsState
}

sealed interface GoalsState {
    data object Loading : GoalsState
    data object Failed : GoalsState
    data class Ready(val goals: List<GoalEntity>) : GoalsState
}

class ProjectsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = Room.databaseBuilder(application, AppDatabase::class.java, "devtrack.db")
        .addMigrations(AppDatabase.Migration1To2, AppDatabase.Migration2To3)
        .build()
    private val repository = LocalProjectRepository(database.projectDao())
    private val goalRepository = GoalRepository(database)
    val state = repository.projects
        .map<List<ProjectEntity>, ProjectsState> { ProjectsState.Ready(it) }
        .catch { emit(ProjectsState.Failed) }
        .stateIn(viewModelScope, SharingStarted.Lazily, ProjectsState.Loading)
    val goals = goalRepository.all
        .map<List<GoalEntity>, GoalsState> { GoalsState.Ready(it) }
        .catch { emit(GoalsState.Failed) }
        .stateIn(viewModelScope, SharingStarted.Lazily, GoalsState.Loading)

    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _writeError = MutableStateFlow<Int?>(null)
    val writeError = _writeError.asStateFlow()

    fun clearError() { _writeError.value = null }

    fun save(project: ProjectEntity, isNew: Boolean, onSuccess: () -> Unit) = write(onSuccess) {
        val clean = project.copy(
            name = project.name.trim(),
            description = project.description.trim(),
            classification = project.classification?.trim()?.takeIf { it.isNotEmpty() }
        )
        if (isNew) repository.add(clean) else goalRepository.updateProject(clean, repository)
    }

    fun delete(project: ProjectEntity, onSuccess: () -> Unit) = write(onSuccess) {
        repository.delete(project)
    }

    fun saveGoal(goal: GoalEntity, isNew: Boolean, onSuccess: () -> Unit) = write(onSuccess) {
        goalRepository.save(goal.copy(name = goal.name.trim(), description = goal.description.trim()), isNew)
    }

    fun deleteGoal(goal: GoalEntity, onSuccess: () -> Unit) = write(onSuccess) {
        goalRepository.delete(goal)
    }

    private fun write(onSuccess: () -> Unit, operation: suspend () -> Unit) {
        if (_busy.value) return
        _busy.value = true
        clearError()
        viewModelScope.launch {
            try {
                operation()
                onSuccess()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _writeError.value = when (error) {
                    is GoalDeadlineException -> R.string.goal_deadline_rule
                    is ProjectDeadlineException -> R.string.project_deadline_conflict
                    else -> R.string.write_failed
                }
            } finally {
                _busy.value = false
            }
        }
    }

    override fun onCleared() { database.close() }
}
