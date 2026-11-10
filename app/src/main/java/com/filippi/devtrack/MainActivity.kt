package com.filippi.devtrack

import android.os.Bundle
import android.os.Build
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.filippi.devtrack.ui.DashboardScreen
import com.filippi.devtrack.ui.GoalsScreen
import com.filippi.devtrack.ui.ProjectsScreen
import com.filippi.devtrack.ui.ProjectsViewModel
import com.filippi.devtrack.ui.AppPreferences
import com.filippi.devtrack.ui.LanguageChoice
import com.filippi.devtrack.ui.SettingsScreen
import com.filippi.devtrack.ui.ThemeChoice

private val DevTrackColors = darkColorScheme(
    primary = Color(0xFF54BEFF),
    onPrimary = Color(0xFF052034),
    secondary = Color(0xFF54DFCA),
    background = Color(0xFF0D121A),
    surface = Color(0xFF151C27),
    surfaceVariant = Color(0xFF252F40),
    onSurface = Color(0xFFF0F4FC),
    onSurfaceVariant = Color(0xFFB7C4D8),
    secondaryContainer = Color(0xFF1B3851),
    onSecondaryContainer = Color(0xFF9DD9FF),
    outlineVariant = Color(0xFF293447)
)

private val DevTrackLightColors = lightColorScheme(
    primary = Color(0xFF005D90),
    secondary = Color(0xFF006B60),
    background = Color(0xFFF4F8FC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE2EBF4),
    onSurface = Color(0xFF152131),
    onSurfaceVariant = Color(0xFF44566B),
    secondaryContainer = Color(0xFFD1EAFF),
    onSecondaryContainer = Color(0xFF084267),
    outlineVariant = Color(0xFFCCD9E7)
)

class MainActivity : ComponentActivity() {
    private val projects by viewModels<ProjectsViewModel>()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppPreferences(newBase).localizedContext())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = AppPreferences(this)
        setContent {
            var theme by remember { mutableStateOf(preferences.theme) }
            val dark = when (theme) {
                ThemeChoice.System -> isSystemInDarkTheme()
                ThemeChoice.Light -> false
                ThemeChoice.Dark -> true
            }
            LaunchedEffect(dark) {
                val transparent = android.graphics.Color.TRANSPARENT
                val bars = if (dark) SystemBarStyle.dark(transparent)
                    else SystemBarStyle.light(transparent, android.graphics.Color.BLACK)
                enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
            }
            MaterialTheme(colorScheme = if (dark) DevTrackColors else DevTrackLightColors) {
                DevTrackApp(projects, settings = {
                    SettingsScreen(theme, preferences.language,
                        onTheme = { preferences.theme = it; theme = it },
                        onLanguage = {
                            if (preferences.language != it) {
                                preferences.language = it
                                if (Build.VERSION.SDK_INT < 33) recreate()
                            }
                        })
                })
            }
        }
    }
}

private enum class Destination(val route: String, val label: Int, val icon: ImageVector) {
    Dashboard("dashboard", R.string.dashboard, Icons.Default.Home),
    Projects("projects", R.string.projects, Icons.AutoMirrored.Filled.List),
    Goals("goals", R.string.goals, Icons.Default.Star),
    Settings("settings", R.string.settings, Icons.Default.Settings)
}

@Composable
private fun DevTrackApp(projects: ProjectsViewModel, settings: @Composable () -> Unit) {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val state by projects.state.collectAsStateWithLifecycle()
    fun open(destination: Destination) {
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    Scaffold(containerColor = MaterialTheme.colorScheme.background, bottomBar = {
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
            Destination.entries.forEach { destination ->
                NavigationBarItem(
                    selected = currentRoute == destination.route,
                    onClick = { open(destination) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    ),
                    label = { Text(stringResource(destination.label)) },
                    icon = { Icon(destination.icon, contentDescription = null) }
                )
            }
        }
    }) { padding ->
        NavHost(navController, startDestination = Destination.Dashboard.route, modifier = Modifier.padding(padding)) {
            composable(Destination.Dashboard.route) {
                DashboardScreen(state, onProjects = { open(Destination.Projects) })
            }
            composable(Destination.Projects.route) { ProjectsScreen(state, projects) }
            composable(Destination.Goals.route) {
                GoalsScreen(state, projects, onProjects = { open(Destination.Projects) })
            }
            composable(Destination.Settings.route) {
                settings()
            }
        }
    }
}
