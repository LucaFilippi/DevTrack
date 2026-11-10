package com.filippi.devtrack.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.filippi.devtrack.BuildConfig
import com.filippi.devtrack.R

@Composable
internal fun SettingsScreen(
    theme: ThemeChoice,
    language: LanguageChoice,
    onTheme: (ThemeChoice) -> Unit,
    onLanguage: (LanguageChoice) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Header(R.string.settings, R.string.settings_subtitle) }
        item {
            PreferenceGroup(R.string.settings_appearance) {
                ThemeChoice.entries.forEach { choice ->
                    PreferenceOption(choice.label, choice == theme) { onTheme(choice) }
                }
            }
        }
        item {
            PreferenceGroup(R.string.settings_language) {
                LanguageChoice.entries.forEach { choice ->
                    PreferenceOption(choice.label, choice == language) { onLanguage(choice) }
                }
            }
        }
        item {
            Panel(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_about), style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.devtrack_logo), contentDescription = null, modifier = Modifier.size(64.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.app_version, BuildConfig.VERSION_NAME))
                        Text(stringResource(R.string.app_storage), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun PreferenceGroup(title: Int, options: @Composable () -> Unit) {
    Panel(Modifier.fillMaxWidth()) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
        Column(Modifier.selectableGroup()) { options() }
    }
}

@Composable
private fun PreferenceOption(label: Int, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp)
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(stringResource(label))
    }
}
