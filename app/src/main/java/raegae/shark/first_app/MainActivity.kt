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
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import raegae.shark.first_app.ui.add.AddStudentScreen
import raegae.shark.first_app.ui.home.HomeScreen
import raegae.shark.first_app.ui.home.AddExistingStudentScreen
import raegae.shark.first_app.ui.profile.StudentProfileScreen
import raegae.shark.first_app.ui.profiles.ProfilesScreen
import raegae.shark.first_app.ui.settings.SettingsScreen
import raegae.shark.first_app.ui.theme.First_appTheme
import raegae.shark.first_app.ui.theme.ProvideAnimationSpeed
import raegae.shark.first_app.data.SettingsDataStore
import raegae.shark.first_app.ui.theme.LocalAnimationSpeed
import androidx.navigation.compose.*
import androidx.compose.animation.*
import raegae.shark.first_app.ui.animation.scaledOffsetTween



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            
            val context = LocalContext.current
            val settings = remember { SettingsDataStore(context) }
            val speed by settings.animationSpeed.collectAsState(initial = 1f)

            CompositionLocalProvider(
                LocalAnimationSpeed provides speed
            ) {
                First_appTheme {
                    FirstApp()
                }
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

val TabIndex = mapOf(
    AppDestinations.HOME.route to 0,
    AppDestinations.PROFILES.route to 1,
    AppDestinations.SETTINGS.route to 2
)


/* ---------- App Scaffold ---------- */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun FirstApp() {
    val navController = rememberNavController()
    val animationSpeed = LocalAnimationSpeed.current
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    var currentTab by remember { mutableStateOf(AppDestinations.HOME.route) }
    var previousTab by remember { mutableStateOf(AppDestinations.HOME.route) }



    fun navigateToRoot(root: String) {
        navController.navigate(root) {
            // This clears any nested screens (AddStudent, Profile, etc)
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = false
            }
            launchSingleTop = true
            restoreState = false
        }
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach { dest ->
                item(
                    icon = { Icon(dest.icon, dest.label) },
                    label = { Text(dest.label) },
                    selected = currentRoute == dest.route,
                    onClick = {
                        if (dest.route != currentTab) {
                        previousTab = currentTab
                        currentTab = dest.route
                    }
                    navigateToRoot(dest.route)
                    }
                )
            }
        }
    ) {
        Scaffold(
            floatingActionButton = {
                when (currentRoute) {
                    AppDestinations.HOME.route -> {
                        FloatingActionButton(
                            onClick = { navController.navigate("add_existing") }
                        ) {
                            Icon(Icons.Filled.Add, "Add Existing")
                        }
                    }

                    AppDestinations.PROFILES.route -> {
                        FloatingActionButton(
                            onClick = { navController.navigate("add_student") }
                        ) {
                            Icon(Icons.Filled.Add, "Add Student")
                        }
                    }
                }
            }
        ) { padding ->

            NavHost(
                navController,
                startDestination = AppDestinations.HOME.route,
                Modifier.padding(padding),
                enterTransition = {
                    val from = TabIndex[previousTab] ?: 0
                    val to = TabIndex[currentTab] ?: 0
                    if (to > from) slideInHorizontally(animationSpec = scaledOffsetTween(300, animationSpeed)) { it } else slideInHorizontally(animationSpec = scaledOffsetTween(300, animationSpeed)) { -it }
                
                },
                exitTransition = {
                    val from = TabIndex[previousTab] ?: 0
                    val to = TabIndex[currentTab] ?: 0
                    if (to > from) slideOutHorizontally(animationSpec = scaledOffsetTween(300, animationSpeed)) { -it } else slideOutHorizontally(animationSpec = scaledOffsetTween(300, animationSpeed)) { it }
                },
                popEnterTransition = {
                    val from = TabIndex[previousTab] ?: 0
                    val to = TabIndex[currentTab] ?: 0
                    if (to > from) slideInHorizontally(animationSpec = scaledOffsetTween(300, animationSpeed)) { it } else slideInHorizontally(animationSpec = scaledOffsetTween(300, animationSpeed)) { -it }
                },
                popExitTransition = {
                    val from = TabIndex[previousTab] ?: 0
                    val to = TabIndex[currentTab] ?: 0
                    if (to > from) slideOutHorizontally(animationSpec = scaledOffsetTween(300, animationSpeed)) { -it } else slideOutHorizontally(animationSpec = scaledOffsetTween(300, animationSpeed)) { it }
                }
                
            ) {
                composable(AppDestinations.HOME.route) {
                    HomeScreen(navController)
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

                composable("add_existing") { backStack ->
                    val nav = navController

                    AddExistingStudentScreen(
                        navController = nav,
                        onStudentSelected = { studentId ->
                            nav.previousBackStackEntry
                                ?.savedStateHandle
                                ?.set("added_student", studentId)
                        }
                    )
                }

                composable(
                    "profile/{studentId}",
                    arguments = listOf(navArgument("studentId") { type = NavType.IntType })
                ) {
                    StudentProfileScreen(
                        navController,
                        it.arguments!!.getInt("studentId")
                    )
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
