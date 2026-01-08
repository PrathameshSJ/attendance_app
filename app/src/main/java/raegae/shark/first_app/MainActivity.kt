package raegae.shark.first_app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import raegae.shark.first_app.ui.add.AddStudentScreen
import raegae.shark.first_app.ui.home.HomeScreen
import raegae.shark.first_app.ui.profile.StudentProfileScreen
import raegae.shark.first_app.ui.profiles.ProfilesScreen
import raegae.shark.first_app.ui.settings.SettingsScreen
import raegae.shark.first_app.ui.theme.First_appTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            First_appTheme {
                FirstApp()
            }
        }
    }
}

@Composable
fun FabForRoute(
    currentRoute: String?,
    navController: NavController
) {
    when (currentRoute) {
        AppDestinations.HOME.route -> {
            FloatingActionButton(
                onClick = { navController.navigate("add_student") }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add student")
            }
        }

        AppDestinations.PROFILES.route -> {
            FloatingActionButton(
                onClick = { navController.navigate("settings") }
            ) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun FirstApp() {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach { destination ->
                item(
                    icon = {
                        Icon(
                            destination.icon,
                            contentDescription = destination.label
                        )
                    },
                    label = { Text(destination.label) },
                    selected = currentRoute == destination.route,
                    onClick = {
                        navController.navigate(destination.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) {
        Scaffold(
            floatingActionButton = {
                FabForRoute(currentRoute, navController)
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = AppDestinations.HOME.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(AppDestinations.HOME.route) {
                    HomeScreen()
                }

                composable(AppDestinations.PROFILES.route) {
                    ProfilesScreen(navController)
                }

                composable("settings") {
                    SettingsScreen()
                }

                composable(
                    route = "profile/{studentId}",
                    arguments = listOf(
                        navArgument("studentId") { type = NavType.IntType }
                    )
                ) { backStackEntry ->
                    StudentProfileScreen(
                        navController = navController,
                        studentId = backStackEntry.arguments?.getInt("studentId") ?: 0
                    )
                }

                composable("add_student") {
                    AddStudentScreen(navController)
                }
            }
        }
    }
}

enum class AppDestinations(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    HOME("home", "Home", Icons.Filled.Home),
    PROFILES("profiles", "Students", Icons.Filled.Person)
}

@Preview(showBackground = true)
@Composable
fun FirstAppPreview() {
    First_appTheme {
        FirstApp()
    }
}
