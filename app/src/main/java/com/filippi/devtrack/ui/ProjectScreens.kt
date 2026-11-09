package com.filippi.devtrack.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filippi.devtrack.R
import com.filippi.devtrack.data.local.entity.ProjectEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.text.NumberFormat

private enum class ProjectStatus(val key: String, val label: Int) {
    Todo("todo", R.string.status_todo),
    Active("active", R.string.status_active),
    Completed("completed", R.string.status_completed),
    Archived("archived", R.string.status_archived);

    companion object {
        fun of(project: ProjectEntity) = entries.firstOrNull { it.key == project.status } ?: Active
    }
}

private fun Long.toDate(): LocalDate = Instant.ofEpochMilli(this).atOffset(ZoneOffset.UTC).toLocalDate()

@Composable
private fun dateLabel(value: Long): String =
    value.toDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(LocalConfiguration.current.locales[0]))

@Composable
private fun Header(title: Int, subtitle: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(stringResource(R.string.brand), color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        Text(stringResource(title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp), content = content)
    }
}

@Composable
fun DashboardScreen(state: ProjectsState, onProjects: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Header(R.string.dashboard, R.string.dashboard_subtitle) }
        when (state) {
            ProjectsState.Loading -> item { CircularProgressIndicator() }
            ProjectsState.Failed -> item { Text(stringResource(R.string.load_failed)) }
            is ProjectsState.Ready -> {
                val current = state.projects.filter { ProjectStatus.of(it) != ProjectStatus.Archived }
                val completed = current.count { ProjectStatus.of(it) == ProjectStatus.Completed }
                val today = LocalDate.now()
                val upcoming = current.filter {
                    ProjectStatus.of(it) != ProjectStatus.Completed &&
                        it.deadlineAt?.toDate()?.isBefore(today) == false
                }.sortedBy { it.deadlineAt }
                val progress = if (current.isEmpty()) 0f else completed.toFloat() / current.size
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Metric(R.string.active_projects, current.count { ProjectStatus.of(it) == ProjectStatus.Active },
                            Icons.AutoMirrored.Filled.List, Modifier.weight(1f))
                        Metric(R.string.status_completed, completed, Icons.Default.CheckCircle, Modifier.weight(1f))
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Metric(R.string.status_todo, current.count { ProjectStatus.of(it) == ProjectStatus.Todo },
                            Icons.AutoMirrored.Filled.List, Modifier.weight(1f))
                        Metric(R.string.upcoming, upcoming.size, Icons.Default.DateRange, Modifier.weight(1f))
                    }
                }
                item {
                    Panel(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.project_progress), style = MaterialTheme.typography.titleMedium)
                            Text(NumberFormat.getPercentInstance(LocalConfiguration.current.locales[0]).format(progress),
                                color = MaterialTheme.colorScheme.primary)
                        }
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                        Text(stringResource(R.string.progress_detail, completed, current.size),
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.next_deadlines), Modifier.weight(1f),
                            style = MaterialTheme.typography.titleLarge)
                        TextButton(onClick = onProjects) { Text(stringResource(R.string.view_projects)) }
                    }
                    if (upcoming.isEmpty()) Text(stringResource(R.string.no_deadlines))
                }
                items(upcoming.take(3), key = { it.id }) { project ->
                    Panel(Modifier.fillMaxWidth()) {
                        Text(project.name, style = MaterialTheme.typography.titleMedium)
                        project.deadlineAt?.let { Text(dateLabel(it), color = MaterialTheme.colorScheme.secondary) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Metric(label: Int, count: Int, icon: ImageVector, modifier: Modifier) {
    Panel(modifier) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(stringResource(label), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(count.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ProjectsScreen(state: ProjectsState, model: ProjectsViewModel) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("") }
    var editId by rememberSaveable { mutableStateOf<String?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
    val busy by model.busy.collectAsStateWithLifecycle()
    val failed by model.writeFailed.collectAsStateWithLifecycle()
    val projects = (state as? ProjectsState.Ready)?.projects.orEmpty()
    val visible = projects.filter {
        val matchesStatus = filter.isEmpty() || ProjectStatus.of(it).key == filter
        matchesStatus && (it.name.contains(query.trim(), ignoreCase = true) ||
            it.description.contains(query.trim(), ignoreCase = true))
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (state is ProjectsState.Ready) {
                FloatingActionButton(onClick = { model.clearError(); adding = true }) {
                    Icon(Icons.Default.Add, stringResource(R.string.new_project))
                }
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Header(R.string.projects, R.string.projects_subtitle) }
            when (state) {
                ProjectsState.Loading -> item { CircularProgressIndicator() }
                ProjectsState.Failed -> item { Text(stringResource(R.string.load_failed)) }
                is ProjectsState.Ready -> {
                    item {
                        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true,
                            label = { Text(stringResource(R.string.search_projects)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            shape = RoundedCornerShape(16.dp))
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(filter.isEmpty(), { filter = "" }, { Text(stringResource(R.string.filter_all)) })
                            ProjectStatus.entries.forEach {
                                FilterChip(filter == it.key, { filter = it.key }, { Text(stringResource(it.label)) })
                            }
                        }
                    }
                    if (visible.isEmpty()) item {
                        Panel(Modifier.fillMaxWidth()) {
                            Text(stringResource(if (projects.isEmpty()) R.string.first_project else R.string.no_matches))
                        }
                    }
                    items(visible, key = { it.id }) { project ->
                        ProjectCard(project, onEdit = { model.clearError(); editId = project.id },
                            onDelete = { model.clearError(); deleteId = project.id })
                    }
                }
            }
        }
    }
    val edited = projects.firstOrNull { it.id == editId }
    if (adding || edited != null) {
        ProjectEditor(edited, busy, failed, onDismiss = { adding = false; editId = null },
            onSave = { model.save(it, adding) { adding = false; editId = null } })
    }
    val deleted = projects.firstOrNull { it.id == deleteId }
    if (deleted != null) {
        AlertDialog(
            onDismissRequest = { if (!busy) deleteId = null },
            title = { Text(stringResource(R.string.delete_project)) },
            text = {
                Text(if (failed) stringResource(R.string.write_failed)
                    else stringResource(R.string.delete_confirm, deleted.name))
            },
            confirmButton = {
                TextButton(enabled = !busy, onClick = { model.delete(deleted) { deleteId = null } }) {
                    Text(stringResource(R.string.delete_action), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { deleteId = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
private fun ProjectCard(project: ProjectEntity, onEdit: () -> Unit, onDelete: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val status = ProjectStatus.of(project)
    Panel(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(project.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Default.MoreVert, stringResource(R.string.project_actions)) }
                DropdownMenu(menu, { menu = false }) {
                    DropdownMenuItem({ Text(stringResource(R.string.edit_project)) }, { menu = false; onEdit() })
                    DropdownMenuItem({ Text(stringResource(R.string.delete_project)) }, { menu = false; onDelete() })
                }
            }
        }
        if (project.description.isNotBlank()) Text(project.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
            Text(stringResource(status.label), Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.onSecondaryContainer, style = MaterialTheme.typography.labelLarge)
        }
        project.deadlineAt?.let {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Text(dateLabel(it), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ProjectEditor(project: ProjectEntity?, busy: Boolean, failed: Boolean,
    onDismiss: () -> Unit, onSave: (ProjectEntity) -> Unit) {
    var name by rememberSaveable(project?.id) { mutableStateOf(project?.name.orEmpty()) }
    var description by rememberSaveable(project?.id) { mutableStateOf(project?.description.orEmpty()) }
    var status by rememberSaveable(project?.id) { mutableStateOf(project?.let { ProjectStatus.of(it).key } ?: ProjectStatus.Todo.key) }
    var deadline by rememberSaveable(project?.id) { mutableStateOf(project?.deadlineAt) }
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(stringResource(if (project == null) R.string.new_project else R.string.edit_project)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), enabled = !busy, singleLine = true,
                    label = { Text(stringResource(R.string.project_name)) })
                OutlinedTextField(description, { description = it }, Modifier.fillMaxWidth(), enabled = !busy,
                    label = { Text(stringResource(R.string.project_description)) })
                Text(stringResource(R.string.project_status))
                ProjectStatus.entries.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach {
                            FilterChip(status == it.key, { status = it.key }, { Text(stringResource(it.label)) }, enabled = !busy)
                        }
                    }
                }
                OutlinedButton(enabled = !busy, onClick = {
                    val initial = deadline?.toDate() ?: LocalDate.now()
                    DatePickerDialog(context, { _, year, month, day ->
                        deadline = LocalDate.of(year, month + 1, day).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
                    }, initial.year, initial.monthValue - 1, initial.dayOfMonth).show()
                }) { Text(deadline?.let { dateLabel(it) } ?: stringResource(R.string.choose_deadline)) }
                if (deadline != null) TextButton(enabled = !busy, onClick = { deadline = null }) {
                    Text(stringResource(R.string.clear_deadline))
                }
                if (failed) Text(stringResource(R.string.write_failed), color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(enabled = !busy && name.isNotBlank(), onClick = {
                onSave((project ?: ProjectEntity(name = name)).copy(name = name, description = description,
                    status = status, deadlineAt = deadline, updatedAt = System.currentTimeMillis()))
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}
