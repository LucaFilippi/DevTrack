package com.filippi.devtrack.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.filippi.devtrack.data.local.database.AppDatabase
import com.filippi.devtrack.data.local.entity.ProjectEntity
import com.filippi.devtrack.data.repository.LocalProjectRepository
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

class ProjectsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = Room.databaseBuilder(application, AppDatabase::class.java, "devtrack.db").build()
    private val repository = LocalProjectRepository(database.projectDao())
    val state = repository.projects
        .map<List<ProjectEntity>, ProjectsState> { ProjectsState.Ready(it) }
        .catch { emit(ProjectsState.Failed) }
        .stateIn(viewModelScope, SharingStarted.Lazily, ProjectsState.Loading)

    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _writeFailed = MutableStateFlow(false)
    val writeFailed = _writeFailed.asStateFlow()

    fun clearError() { _writeFailed.value = false }

    fun save(project: ProjectEntity, isNew: Boolean, onSuccess: () -> Unit) = write(onSuccess) {
        val clean = project.copy(name = project.name.trim(), description = project.description.trim())
        if (isNew) repository.add(clean) else repository.update(clean)
    }

    fun delete(project: ProjectEntity, onSuccess: () -> Unit) = write(onSuccess) {
        repository.delete(project)
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
            } catch (_: Exception) {
                _writeFailed.value = true
            } finally {
                _busy.value = false
            }
        }
    }

    override fun onCleared() { database.close() }
}
