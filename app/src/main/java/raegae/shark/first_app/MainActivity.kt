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
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
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

/* ---------- Floating Button Logic ---------- */

@Composable
fun FabForRoute(currentRoute: String?, navController: NavController) {
    when (currentRoute) {
        AppDestinations.HOME.route -> {
            // Add existing student to today
            FloatingActionButton(
                onClick = { navController.navigate("home_add_existing") }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add to today")
            }
        }

        AppDestinations.PROFILES.route -> {
            // Add new student
            FloatingActionButton(
                onClick = { navController.navigate("add_student") }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add student")
            }
        }

        else -> {}
    }
}

/* ---------- App Scaffold ---------- */

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
                    icon = { Icon(destination.icon, destination.label) },
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

                composable(AppDestinations.SETTINGS.route) {
                    SettingsScreen()
                }

                composable("add_student") {
                    AddStudentScreen(navController)
                }

                composable(
                    route = "profile/{studentId}",
                    arguments = listOf(navArgument("studentId") { type = NavType.IntType })
                ) { backStackEntry ->
                    StudentProfileScreen(
                        navController = navController,
                        studentId = backStackEntry.arguments?.getInt("studentId") ?: 0
                    )
                }

                // This will be implemented next
                composable("home_add_existing") {
                    // placeholder — we wire this next
                }
            }
        }
    }
}

/* ---------- Bottom Tabs ---------- */

enum class AppDestinations(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    HOME("home", "Home", Icons.Filled.Home),
    PROFILES("profiles", "Students", Icons.Filled.Person),
    SETTINGS("settings", "Settings", Icons.Filled.Settings)
}

@Preview(showBackground = true)
@Composable
fun FirstAppPreview() {
    First_appTheme {
        FirstApp()
    }
}
