package com.filippi.devtrack.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filippi.devtrack.R
import com.filippi.devtrack.data.local.entity.GoalEntity
import com.filippi.devtrack.data.local.entity.ProjectEntity
import java.time.LocalDate

@Composable
fun GoalsScreen(projectState: ProjectsState, model: ProjectsViewModel, onProjects: () -> Unit) {
    val goalState by model.goals.collectAsStateWithLifecycle()
    val busy by model.busy.collectAsStateWithLifecycle()
    val error by model.writeError.collectAsStateWithLifecycle()
    val projects = (projectState as? ProjectsState.Ready)?.projects.orEmpty()
    val projectNames = projects.associate { it.id to it.name }
    val eligible = projects.filter { it.deadlineAt != null }
    val goals = (goalState as? GoalsState.Ready)?.goals.orEmpty()
    var adding by rememberSaveable { mutableStateOf(false) }
    var editId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (goalState is GoalsState.Ready && eligible.isNotEmpty()) {
                FloatingActionButton(onClick = { model.clearError(); adding = true }) {
                    Icon(Icons.Default.Add, stringResource(R.string.new_goal))
                }
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Header(R.string.goals, R.string.goals_subtitle) }
            when {
                projectState == ProjectsState.Failed || goalState == GoalsState.Failed ->
                    item { Text(stringResource(R.string.goals_load_failed)) }
                projectState == ProjectsState.Loading || goalState == GoalsState.Loading ->
                    item { CircularProgressIndicator() }
                eligible.isEmpty() -> item {
                    Panel(Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.goals_need_project))
                        TextButton(onClick = onProjects) { Text(stringResource(R.string.view_projects)) }
                    }
                }
                goals.isEmpty() -> item {
                    Panel(Modifier.fillMaxWidth()) { Text(stringResource(R.string.first_goal)) }
                }
            }
            items(goals, key = { it.id }) { goal ->
                GoalCard(goal, projectNames[goal.projectId].orEmpty(),
                    onEdit = { model.clearError(); editId = goal.id },
                    onDelete = { model.clearError(); deleteId = goal.id })
            }
        }
    }

    val edited = goals.firstOrNull { it.id == editId }
    if (adding || edited != null) {
        GoalEditor(edited, eligible, busy, error,
            onDismiss = { adding = false; editId = null },
            onSave = { model.saveGoal(it, adding) { adding = false; editId = null } })
    }
    goals.firstOrNull { it.id == deleteId }?.let { goal ->
        AlertDialog(
            onDismissRequest = { if (!busy) deleteId = null },
            title = { Text(stringResource(R.string.delete_goal)) },
            text = { Text(error?.let { stringResource(it) } ?: stringResource(R.string.delete_goal_confirm, goal.name)) },
            confirmButton = {
                TextButton(enabled = !busy, onClick = { model.deleteGoal(goal) { deleteId = null } }) {
                    Text(stringResource(R.string.delete_action), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = { deleteId = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun GoalCard(goal: GoalEntity, projectName: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Panel(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(goal.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(projectName, color = MaterialTheme.colorScheme.primary)
            }
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Default.MoreVert, stringResource(R.string.goal_actions))
                }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem({ Text(stringResource(R.string.edit_goal)) }, { menu = false; onEdit() })
                    DropdownMenuItem({ Text(stringResource(R.string.delete_goal)) }, { menu = false; onDelete() })
                }
            }
        }
        Text(goal.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Text(dateLabel(goal.deadlineAt), Modifier.weight(1f))
            if (goal.completed) Text(stringResource(R.string.status_completed), color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun GoalEditor(
    goal: GoalEntity?,
    projects: List<ProjectEntity>,
    busy: Boolean,
    error: Int?,
    onDismiss: () -> Unit,
    onSave: (GoalEntity) -> Unit
) {
    var name by rememberSaveable(goal?.id) { mutableStateOf(goal?.name.orEmpty()) }
    var description by rememberSaveable(goal?.id) { mutableStateOf(goal?.description.orEmpty()) }
    var projectId by rememberSaveable(goal?.id) { mutableStateOf(goal?.projectId ?: projects.singleOrNull()?.id) }
    var deadline by rememberSaveable(goal?.id) { mutableStateOf(goal?.deadlineAt) }
    var completed by rememberSaveable(goal?.id) { mutableStateOf(goal?.completed ?: false) }
    var choosingProject by remember { mutableStateOf(false) }
    val project = projects.firstOrNull { it.id == projectId }
    val projectDeadline = project?.deadlineAt
    val selectedDeadline = deadline
    val validDeadline = selectedDeadline != null && projectDeadline != null && selectedDeadline < projectDeadline
    val context = LocalContext.current
    val lightPicker = MaterialTheme.colorScheme.background.luminance() > 0.5f
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(stringResource(if (goal == null) R.string.new_goal else R.string.edit_goal)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), enabled = !busy,
                    singleLine = true, label = { Text(stringResource(R.string.goal_name)) })
                OutlinedTextField(description, { description = it }, Modifier.fillMaxWidth(), enabled = !busy,
                    label = { Text(stringResource(R.string.goal_description)) })
                Box {
                    OutlinedButton(onClick = { choosingProject = true }, enabled = !busy && projects.isNotEmpty()) {
                        Text(project?.name ?: stringResource(R.string.choose_project))
                    }
                    DropdownMenu(choosingProject, { choosingProject = false }, Modifier.heightIn(max = 280.dp)) {
                        projects.forEach { candidate ->
                            DropdownMenuItem(text = { Text(candidate.name) }, onClick = {
                                projectId = candidate.id
                                choosingProject = false
                            })
                        }
                    }
                }
                if (projectDeadline != null) {
                    Text(stringResource(R.string.goal_deadline_before, dateLabel(projectDeadline)),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedButton(enabled = !busy, onClick = {
                        val lastAllowed = projectDeadline.toDate().minusDays(1)
                        val initial = minOf(deadline?.toDate() ?: LocalDate.now(), lastAllowed)
                        showDeadlinePicker(context, initial, lightPicker) { deadline = it }
                    }) {
                        Text(selectedDeadline?.let { dateLabel(it) } ?: stringResource(R.string.choose_deadline))
                    }
                }
                if (selectedDeadline != null && !validDeadline && projectDeadline != null) {
                    Text(stringResource(R.string.goal_deadline_rule), color = MaterialTheme.colorScheme.error)
                }
                if (goal != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = completed, onCheckedChange = { completed = it }, enabled = !busy)
                        Text(stringResource(R.string.goal_completed))
                    }
                }
                if (error != null) Text(stringResource(error), color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(enabled = !busy && validDeadline && name.isNotBlank() && description.isNotBlank(),
                onClick = {
                    if (project != null && selectedDeadline != null) {
                        val updated = goal?.copy(projectId = project.id, name = name, description = description,
                            deadlineAt = selectedDeadline, completed = completed)
                            ?: GoalEntity(projectId = project.id, name = name, description = description,
                                deadlineAt = selectedDeadline)
                        onSave(updated)
                    }
                }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(enabled = !busy, onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
