package com.filippi.devtrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme = darkColorScheme()) { DevTrackApp() } }
    }
}

private enum class Destination(val route: String, val label: Int, val emptyMessage: Int) {
    Dashboard("dashboard", R.string.dashboard, R.string.dashboard_empty),
    Projects("projects", R.string.projects, R.string.projects_empty),
    Goals("goals", R.string.goals, R.string.goals_empty),
    Settings("settings", R.string.settings, R.string.settings_empty)
}

@Composable
private fun DevTrackApp() {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    Scaffold(bottomBar = {
        NavigationBar {
            Destination.entries.forEach { destination ->
                NavigationBarItem(
                    selected = currentRoute == destination.route,
                    onClick = {
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    label = { Text(stringResource(destination.label)) },
                    icon = { Text(destination.name.take(1)) }
                )
            }
        }
    }) { padding ->
        NavHost(navController, startDestination = Destination.Dashboard.route, modifier = Modifier.padding(padding)) {
            Destination.entries.forEach { destination ->
                composable(destination.route) { PlaceholderScreen(destination) }
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(destination: Destination) {
    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(stringResource(destination.label), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(destination.emptyMessage), style = MaterialTheme.typography.bodyLarge)
        }
    }
}
