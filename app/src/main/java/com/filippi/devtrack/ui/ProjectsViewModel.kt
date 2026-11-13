package com.filippi.devtrack.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.filippi.devtrack.R
import com.filippi.devtrack.data.local.database.AppDatabase
import com.filippi.devtrack.data.local.entity.GoalEntity
import com.filippi.devtrack.data.local.entity.ProjectEntity
import com.filippi.devtrack.data.backup.BackupDocument
import com.filippi.devtrack.data.backup.BackupFormatException
import com.filippi.devtrack.data.backup.BackupService
import com.filippi.devtrack.data.backup.UnsupportedBackupVersionException
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
import java.io.IOException

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

sealed interface BackupState {
    data object Idle : BackupState
    data object Working : BackupState
    data class RestoreReady(val document: BackupDocument) : BackupState
    data class Message(val text: Int) : BackupState
}

class ProjectsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = Room.databaseBuilder(application, AppDatabase::class.java, "devtrack.db")
        .addMigrations(AppDatabase.Migration1To2, AppDatabase.Migration2To3)
        .build()
    private val repository = LocalProjectRepository(database.projectDao())
    private val goalRepository = GoalRepository(database)
    private val backupService = BackupService(application.contentResolver, database)
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
    private val _backupState = MutableStateFlow<BackupState>(BackupState.Idle)
    val backupState = _backupState.asStateFlow()

    fun clearError() { _writeError.value = null }
    fun clearBackupState() { _backupState.value = BackupState.Idle }

    fun exportBackup(uri: Uri) = backupOperation {
        backupService.export(uri)
        BackupState.Message(R.string.backup_export_success)
    }

    fun prepareImport(uri: Uri) = backupOperation {
        BackupState.RestoreReady(backupService.readAndValidate(uri))
    }

    fun confirmRestore() {
        val ready = _backupState.value as? BackupState.RestoreReady ?: return
        _backupState.value = BackupState.Working
        viewModelScope.launch {
            try {
                backupService.restore(ready.document)
                DeadlineReminders.start(getApplication())
                _backupState.value = BackupState.Message(R.string.backup_import_success)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _backupState.value = BackupState.Message(R.string.backup_import_failed)
            }
        }
    }

    fun cancelRestore() { clearBackupState() }

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
                DeadlineReminders.start(getApplication())
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

    private fun backupOperation(operation: suspend () -> BackupState) {
        if (_backupState.value is BackupState.Working) return
        _backupState.value = BackupState.Working
        viewModelScope.launch {
            try {
                _backupState.value = operation()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _backupState.value = BackupState.Message(
                    when (error) {
                        is UnsupportedBackupVersionException -> R.string.backup_unsupported_version
                        is BackupFormatException -> R.string.backup_invalid_file
                        is IOException -> R.string.backup_file_error
                        else -> R.string.backup_export_failed
                    }
                )
            }
        }
    }

    override fun onCleared() { database.close() }
}
