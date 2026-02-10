package raegae.shark.attnow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import raegae.shark.attnow.data.AppDatabase
import raegae.shark.attnow.data.AppGlobalState
import raegae.shark.attnow.data.SettingsDataStore
import raegae.shark.attnow.data.export.AttendanceExcelManager
import raegae.shark.attnow.data.util.StudentKey
import raegae.shark.attnow.ui.add.AddStudentScreen
import raegae.shark.attnow.ui.animation.scaledOffsetTween
import raegae.shark.attnow.ui.edit.EditStudentScreen
import raegae.shark.attnow.ui.home.AddExistingStudentScreen
import raegae.shark.attnow.ui.home.HomeScreen
import raegae.shark.attnow.ui.profile.StudentProfileScreen
import raegae.shark.attnow.ui.profiles.ProfilesScreen
import raegae.shark.attnow.ui.settings.SettingsScreen
import raegae.shark.attnow.ui.theme.LocalAnimationSpeed
import raegae.shark.attnow.ui.theme.attnowTheme

class MainActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {

                super.onCreate(savedInstanceState)
                startImportWatcher()
                enableEdgeToEdge()
                setContent {
                        val context = LocalContext.current
                        val settings = remember { SettingsDataStore(context) }
                        val speed by settings.animationSpeed.collectAsState(initial = 1f)

                        CompositionLocalProvider(LocalAnimationSpeed provides speed) {
                                attnowTheme {
                                        Box(Modifier.fillMaxSize()) {
                                                FirstApp()

                                                val isImporting by
                                                        AppGlobalState.isImporting.collectAsState()
                                                if (isImporting) {
                                                        Box(
                                                                Modifier.fillMaxSize()
                                                                        .background(
                                                                                androidx.compose.ui
                                                                                        .graphics
                                                                                        .Color.Black
                                                                                        .copy(
                                                                                                alpha =
                                                                                                        0.7f
                                                                                        )
                                                                        )
                                                                        .clickable(
                                                                                enabled = true,
                                                                                interactionSource =
                                                                                        remember {
                                                                                                androidx.compose
                                                                                                        .foundation
                                                                                                        .interaction
                                                                                                        .MutableInteractionSource()
                                                                                        },
                                                                                indication = null
                                                                        ) { /* Intercept Touch */},
                                                                contentAlignment =
                                                                        androidx.compose.ui
                                                                                .Alignment.Center
                                                        ) {
                                                                Column(
                                                                        horizontalAlignment =
                                                                                androidx.compose.ui
                                                                                        .Alignment
                                                                                        .CenterHorizontally
                                                                ) {
                                                                        CircularProgressIndicator(
                                                                                color =
                                                                                        androidx.compose
                                                                                                .ui
                                                                                                .graphics
                                                                                                .Color
                                                                                                .White
                                                                        )
                                                                        Spacer(
                                                                                Modifier.height(
                                                                                        16.dp
                                                                                )
                                                                        )
                                                                        Text(
                                                                                "Importing Data...",
                                                                                style =
                                                                                        MaterialTheme
                                                                                                .typography
                                                                                                .titleLarge,
                                                                                color =
                                                                                        androidx.compose
                                                                                                .ui
                                                                                                .graphics
                                                                                                .Color
                                                                                                .White
                                                                        )
                                                                        Text(
                                                                                "Please wait, do not close the app.",
                                                                                style =
                                                                                        MaterialTheme
                                                                                                .typography
                                                                                                .bodyMedium,
                                                                                color =
                                                                                        androidx.compose
                                                                                                .ui
                                                                                                .graphics
                                                                                                .Color
                                                                                                .White
                                                                                                .copy(
                                                                                                        alpha =
                                                                                                                0.8f
                                                                                                )
                                                                        )
                                                                }
                                                        }
                                                }
                                        }
                                }
                        }
                }
        }

        private fun startImportWatcher() {
                lifecycleScope.launch(Dispatchers.IO) {
                        while (isActive) {
                                delay(3000) // Check every 3 seconds
                                try {
                                        val baseDir = getExternalFilesDir(null) ?: filesDir
                                        val importsDir = File(baseDir, "imports")
                                        val file = File(importsDir, "pending_import.xlsx")

                                        if (file.exists() && file.length() > 0) {
                                                android.util.Log.d(
                                                        "Watcher",
                                                        "Found pending import file!"
                                                )
                                                // Wait a bit to ensure write is done
                                                delay(1000)

                                                AppGlobalState.setImporting(true)
                                                val db = AppDatabase.getDatabase(applicationContext)
                                                val manager =
                                                        AttendanceExcelManager(
                                                                applicationContext,
                                                                db
                                                        )

                                                val result = manager.import(file)

                                                // Delete file
                                                if (file.exists()) file.delete()

                                                AppGlobalState.setImportResult(result.message)
                                                AppGlobalState.setImporting(false)
                                                android.util.Log.d(
                                                        "Watcher",
                                                        "Import processed. Result posted."
                                                )
                                        }
                                } catch (e: Exception) {
                                        android.util.Log.e("Watcher", "Watcher Error", e)
                                        // Ensure we unlock on error
                                        AppGlobalState.setImporting(false)
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
                        ) { Icon(Icons.Filled.Add, contentDescription = "Add to today") }
                }
                AppDestinations.PROFILES.route -> {
                        // Add new student
                        FloatingActionButton(onClick = { navController.navigate("add_student") }) {
                                Icon(Icons.Filled.Add, contentDescription = "Add student")
                        }
                }
                else -> {}
        }
}

val TabIndex =
        mapOf(
                AppDestinations.HOME.route to 0,
                AppDestinations.PROFILES.route to 1,
                AppDestinations.NOTIFICATIONS.route to 2,
                AppDestinations.SETTINGS.route to 3
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
                        popUpTo(navController.graph.findStartDestination().id) { saveState = false }
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
                                                if (currentRoute != dest.route) {
                                                        if (dest.route != currentTab) {
                                                                previousTab = currentTab
                                                                currentTab = dest.route
                                                        }
                                                        navigateToRoot(dest.route)
                                                }
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
                                                        onClick = {
                                                                navController.navigate(
                                                                        "add_existing"
                                                                )
                                                        }
                                                ) { Icon(Icons.Filled.Add, "Add Existing") }
                                        }
                                        AppDestinations.PROFILES.route -> {
                                                FloatingActionButton(
                                                        onClick = {
                                                                navController.navigate(
                                                                        "add_student"
                                                                )
                                                        }
                                                ) { Icon(Icons.Filled.Add, "Add Student") }
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
                                        if (to > from)
                                                slideInHorizontally(
                                                        animationSpec =
                                                                scaledOffsetTween(
                                                                        300,
                                                                        animationSpeed
                                                                )
                                                ) { it }
                                        else
                                                slideInHorizontally(
                                                        animationSpec =
                                                                scaledOffsetTween(
                                                                        300,
                                                                        animationSpeed
                                                                )
                                                ) { -it }
                                },
                                exitTransition = {
                                        val from = TabIndex[previousTab] ?: 0
                                        val to = TabIndex[currentTab] ?: 0
                                        if (to > from)
                                                slideOutHorizontally(
                                                        animationSpec =
                                                                scaledOffsetTween(
                                                                        300,
                                                                        animationSpeed
                                                                )
                                                ) { -it }
                                        else
                                                slideOutHorizontally(
                                                        animationSpec =
                                                                scaledOffsetTween(
                                                                        300,
                                                                        animationSpeed
                                                                )
                                                ) { it }
                                },
                                popEnterTransition = {
                                        val from = TabIndex[previousTab] ?: 0
                                        val to = TabIndex[currentTab] ?: 0
                                        if (to > from)
                                                slideInHorizontally(
                                                        animationSpec =
                                                                scaledOffsetTween(
                                                                        300,
                                                                        animationSpeed
                                                                )
                                                ) { it }
                                        else
                                                slideInHorizontally(
                                                        animationSpec =
                                                                scaledOffsetTween(
                                                                        300,
                                                                        animationSpeed
                                                                )
                                                ) { -it }
                                },
                                popExitTransition = {
                                        val from = TabIndex[previousTab] ?: 0
                                        val to = TabIndex[currentTab] ?: 0
                                        if (to > from)
                                                slideOutHorizontally(
                                                        animationSpec =
                                                                scaledOffsetTween(
                                                                        300,
                                                                        animationSpeed
                                                                )
                                                ) { -it }
                                        else
                                                slideOutHorizontally(
                                                        animationSpec =
                                                                scaledOffsetTween(
                                                                        300,
                                                                        animationSpeed
                                                                )
                                                ) { it }
                                }
                        ) {
                                composable(AppDestinations.HOME.route) { HomeScreen(navController) }

                                composable(AppDestinations.PROFILES.route) {
                                        ProfilesScreen(navController)
                                }

                                composable(AppDestinations.NOTIFICATIONS.route) {
                                        raegae.shark.attnow.ui.notifications.NotificationsScreen(
                                                navController
                                        )
                                }

                                composable(
                                        "notification_detail/{id}",
                                        arguments =
                                                listOf(
                                                        navArgument("id") {
                                                                type = NavType.StringType
                                                        }
                                                )
                                ) { entry ->
                                        val id = entry.arguments?.getString("id") ?: ""

                                        val viewModel:
                                                raegae.shark.attnow.viewmodels.NotificationsViewModel =
                                                androidx.lifecycle.viewmodel.compose.viewModel()

                                        raegae.shark.attnow.ui.notifications
                                                .NotificationDetailScreen(
                                                        navController,
                                                        id,
                                                        viewModel
                                                )
                                }

                                composable(AppDestinations.SETTINGS.route) {
                                        SettingsScreen(navController)
                                }

                                composable("account") {
                                        val context = LocalContext.current
                                        val driveViewModel:
                                                raegae.shark.attnow.viewmodels.DriveViewModel =
                                                androidx.lifecycle.viewmodel.compose.viewModel()

                                        raegae.shark.attnow.ui.settings.AccountScreen(
                                                navController = navController,
                                                viewModel = driveViewModel,
                                                onRestore = { fileId ->
                                                        val baseDir =
                                                                context.getExternalFilesDir(null)
                                                                        ?: context.filesDir
                                                        val importsDir = File(baseDir, "imports")
                                                        if (!importsDir.exists())
                                                                importsDir.mkdirs()
                                                        val target =
                                                                File(
                                                                        importsDir,
                                                                        "pending_import.xlsx"
                                                                )

                                                        driveViewModel.performRestore(
                                                                fileId,
                                                                target
                                                        ) {
                                                                // Watcher will pick it up
                                                                navController.navigate(
                                                                        AppDestinations.SETTINGS
                                                                                .route
                                                                ) {
                                                                        popUpTo(
                                                                                AppDestinations
                                                                                        .SETTINGS
                                                                                        .route
                                                                        ) { inclusive = true }
                                                                }
                                                        }
                                                }
                                        )
                                }

                                composable("add_student") { AddStudentScreen(navController) }

                                composable("add_existing") { _ ->
                                        AddExistingStudentScreen(
                                                navController = navController,
                                                onStudentSelected = { key: StudentKey ->
                                                        navController.previousBackStackEntry
                                                                ?.savedStateHandle?.set(
                                                                "added_student",
                                                                key
                                                        )
                                                }
                                        )
                                }

                                composable(
                                        "profile/{name}/{subject}",
                                        arguments =
                                                listOf(
                                                        navArgument("name") {
                                                                type = NavType.StringType
                                                        },
                                                        navArgument("subject") {
                                                                type = NavType.StringType
                                                        }
                                                )
                                ) {
                                        val name = it.arguments?.getString("name") ?: ""
                                        val subject = it.arguments?.getString("subject") ?: ""

                                        StudentProfileScreen(
                                                navController,
                                                StudentKey(name, subject)
                                        )
                                }

                                composable(
                                        "edit_student/{name}/{subject}",
                                        arguments =
                                                listOf(
                                                        navArgument("name") {
                                                                type = NavType.StringType
                                                        },
                                                        navArgument("subject") {
                                                                type = NavType.StringType
                                                        }
                                                )
                                ) {
                                        val name = it.arguments?.getString("name") ?: ""
                                        val subject = it.arguments?.getString("subject") ?: ""

                                        EditStudentScreen(navController, StudentKey(name, subject))
                                }

                                composable(
                                        "edit_entities/{name}/{subject}",
                                        arguments =
                                                listOf(
                                                        navArgument("name") {
                                                                type = NavType.StringType
                                                        },
                                                        navArgument("subject") {
                                                                type = NavType.StringType
                                                        }
                                                )
                                ) { entry ->
                                        val name = entry.arguments?.getString("name") ?: ""
                                        val subject = entry.arguments?.getString("subject") ?: ""

                                        val context = LocalContext.current
                                        val viewModel:
                                                raegae.shark.attnow.viewmodels.EditEntitiesViewModel =
                                                androidx.lifecycle.viewmodel.compose.viewModel(
                                                        factory =
                                                                raegae.shark.attnow.viewmodels
                                                                        .EditEntitiesViewModelFactory(
                                                                                context,
                                                                                StudentKey(
                                                                                        name,
                                                                                        subject
                                                                                )
                                                                        )
                                                )

                                        // Observe Result from Detail Screen
                                        val savedStateHandle = entry.savedStateHandle
                                        val updatedStudent =
                                                savedStateHandle.get<
                                                        raegae.shark.attnow.data.Student>(
                                                        "updated_student"
                                                )

                                        LaunchedEffect(updatedStudent) {
                                                updatedStudent?.let {
                                                        viewModel.updateEntity(it)
                                                        savedStateHandle.remove<
                                                                raegae.shark.attnow.data.Student>(
                                                                "updated_student"
                                                        )
                                                }
                                        }

                                        raegae.shark.attnow.ui.edit.EditEntitiesListScreen(
                                                navController,
                                                viewModel
                                        )
                                }

                                composable(
                                        "edit_entity_detail/{id}",
                                        arguments =
                                                listOf(navArgument("id") { type = NavType.IntType })
                                ) { entry ->
                                        val id = entry.arguments?.getInt("id") ?: 0
                                        raegae.shark.attnow.ui.edit.EditEntityDetailScreen(
                                                navController,
                                                id
                                        )
                                }
                        }
                }
        }
}

/* ---------- Bottom Tabs ---------- */

enum class AppDestinations(val route: String, val label: String, val icon: ImageVector) {
        HOME("home", "Home", Icons.Filled.Home),
        PROFILES("profiles", "Students", Icons.Filled.Person),
        NOTIFICATIONS("notifications", "Alerts", Icons.Filled.Notifications),
        SETTINGS("settings", "Settings", Icons.Filled.Settings)
}

@Preview(showBackground = true)
@Composable
fun FirstAppPreview() {

        attnowTheme { FirstApp() }
}
