package com.filippi.devtrack.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.filippi.devtrack.BuildConfig
import com.filippi.devtrack.R
import androidx.core.content.ContextCompat

private enum class SettingsDialog { Theme, Language, Storage, About }

@Composable
internal fun SettingsScreen(
    theme: ThemeChoice,
    language: LanguageChoice,
    preferences: AppPreferences,
    onBack: () -> Unit,
    onTheme: (ThemeChoice) -> Unit,
    onLanguage: (LanguageChoice) -> Unit,
    backupState: BackupState,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onConfirmRestore: () -> Unit,
    onCancelRestore: () -> Unit,
    onDismissBackupMessage: () -> Unit
) {
    var dialog by rememberSaveable { mutableStateOf<SettingsDialog?>(null) }
    var privacy by rememberSaveable { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(preferences.notificationsEnabled) }
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            preferences.notificationsEnabled = true
            notificationsEnabled = true
            DeadlineReminders.start(context)
        }
    }
    val uriHandler = LocalUriHandler.current
    val backupBusy = backupState is BackupState.Working

    if (privacy) {
        BackHandler { privacy = false }
        PrivacyScreen { privacy = false }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        item {
            SettingsTitle(R.string.settings, onBack)
            Text(stringResource(R.string.settings_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
            SectionTitle(R.string.settings_appearance)
            SettingsRow(Icons.Default.Settings, R.string.settings_theme, stringResource(theme.label)) {
                dialog = SettingsDialog.Theme
            }
            SettingsRow(Icons.AutoMirrored.Filled.List, R.string.settings_language, stringResource(language.label)) {
                dialog = SettingsDialog.Language
            }
        }
        item {
            SectionTitle(R.string.settings_productivity, divider = true)
            SettingLine(Icons.Default.Info, R.string.settings_notifications,
                stringResource(R.string.settings_notifications_description)) {
                Switch(checked = notificationsEnabled, onCheckedChange = { enabled ->
                    if (!enabled) {
                        preferences.notificationsEnabled = false
                        notificationsEnabled = false
                        DeadlineReminders.stop(context)
                    } else if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                            context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        preferences.notificationsEnabled = true
                        notificationsEnabled = true
                        DeadlineReminders.start(context)
                    }
                })
            }
            Text(stringResource(R.string.settings_productivity_unavailable),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 4.dp))
        }
        item {
            SectionTitle(R.string.settings_data, divider = true)
            SettingsRow(Icons.Default.Build, R.string.settings_backup_export,
                stringResource(R.string.settings_backup_export_description), enabled = !backupBusy,
                onClick = onExportBackup)
            SettingsRow(Icons.Default.Build, R.string.settings_backup_import,
                stringResource(R.string.settings_backup_import_description), enabled = !backupBusy,
                onClick = onImportBackup)
            SettingsRow(Icons.Default.Lock, R.string.settings_local_storage, stringResource(R.string.settings_local_only)) {
                dialog = SettingsDialog.Storage
            }
        }
        item {
            SectionTitle(R.string.settings_about, divider = true)
            SettingsRow(Icons.Default.Info, R.string.settings_about_app, stringResource(R.string.app_version, BuildConfig.VERSION_NAME)) {
                dialog = SettingsDialog.About
            }
            SettingsRow(Icons.Default.Build, R.string.settings_source, stringResource(R.string.settings_open_source)) {
                uriHandler.openUri("https://github.com/LucaFilippi/DevTrack")
            }
            SettingsRow(Icons.Default.Lock, R.string.settings_privacy, stringResource(R.string.settings_privacy_summary)) {
                privacy = true
            }
        }
    }

    val dismiss = { dialog = null }
    when (dialog) {
        SettingsDialog.Theme -> ChoiceDialog(R.string.settings_theme, ThemeChoice.entries, theme, { it.label }, dismiss) {
            dismiss()
            onTheme(it)
        }
        SettingsDialog.Language -> ChoiceDialog(R.string.settings_language, LanguageChoice.entries, language, { it.label }, dismiss) {
            dismiss()
            onLanguage(it)
        }
        SettingsDialog.Storage -> InfoDialog(R.string.settings_local_storage, dismiss) {
            Text(stringResource(R.string.app_storage))
        }
        SettingsDialog.About -> InfoDialog(R.string.settings_about_app, dismiss) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.devtrack_logo), contentDescription = null, modifier = Modifier.size(64.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.app_version, BuildConfig.VERSION_NAME))
                }
            }
        }
        null -> Unit
    }

    when (val state = backupState) {
        BackupState.Idle, BackupState.Working -> Unit
        is BackupState.RestoreReady -> AlertDialog(
            onDismissRequest = onCancelRestore,
            title = { Text(stringResource(R.string.backup_restore_title)) },
            text = {
                Text(stringResource(R.string.backup_restore_message,
                    state.document.projects.size, state.document.goals.size))
            },
            confirmButton = {
                TextButton(onClick = onConfirmRestore) { Text(stringResource(R.string.backup_restore_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = onCancelRestore) { Text(stringResource(R.string.cancel)) }
            }
        )
        is BackupState.Message -> InfoDialog(state.text, onDismissBackupMessage) {
            Text(stringResource(state.text))
        }
    }
}

@Composable
private fun SettingsTitle(title: Int, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.settings_back))
        }
        Text(stringResource(title), style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun SectionTitle(title: Int, divider: Boolean = false) {
    if (divider) HorizontalDivider(Modifier.padding(top = 10.dp, bottom = 12.dp))
    Text(stringResource(title), Modifier.padding(top = 8.dp, bottom = 4.dp),
        color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
}

@Composable
private fun SettingsRow(icon: ImageVector, title: Int, subtitle: String, enabled: Boolean = true, onClick: () -> Unit) {
    SettingLine(icon, title, subtitle, Modifier.clickable(enabled = enabled, onClick = onClick, role = Role.Button)) {
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingLine(
    icon: ImageVector,
    title: Int,
    subtitle: String,
    modifier: Modifier = Modifier,
    end: @Composable () -> Unit
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 60.dp).padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall)
        }
        end()
    }
}

@Composable
private fun <T> ChoiceDialog(
    title: Int,
    choices: List<T>,
    selected: T,
    label: (T) -> Int,
    dismiss: () -> Unit,
    choose: (T) -> Unit
) {
    InfoDialog(title, dismiss) {
        Column(Modifier.selectableGroup()) {
            choices.forEach { choice ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp)
                        .selectable(selected = choice == selected, role = Role.RadioButton) { choose(choice) },
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = choice == selected, onClick = null)
                    Text(stringResource(label(choice)))
                }
            }
        }
    }
}

@Composable
private fun InfoDialog(title: Int, dismiss: () -> Unit, content: @Composable () -> Unit) {
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(stringResource(title)) },
        text = content,
        confirmButton = { TextButton(onClick = dismiss) { Text(stringResource(R.string.settings_close)) } }
    )
}

private val privacySections = listOf(
    R.string.privacy_storage_title to R.string.privacy_storage_body,
    R.string.privacy_policy_title to R.string.privacy_policy_body,
    R.string.privacy_responsibility_title to R.string.privacy_responsibility_body,
    R.string.privacy_availability_title to R.string.privacy_availability_body,
    R.string.privacy_updates_title to R.string.privacy_updates_body
)

@Composable
private fun PrivacyScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        SettingsTitle(R.string.settings_privacy, onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(stringResource(R.string.privacy_title), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.privacy_version), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.privacy_intro))
            privacySections.forEach { (title, body) ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(body))
                }
            }
            Text(stringResource(R.string.privacy_acknowledgment))
        }
    }
}
