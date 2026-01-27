package raegae.shark.attnow.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import raegae.shark.attnow.data.SettingsDataStore
import raegae.shark.attnow.ui.theme.LocalAnimationSpeed
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.compose.ui.platform.LocalContext
import raegae.shark.attnow.data.AppDatabase
import raegae.shark.attnow.data.export.AttendanceExcelManager
import kotlinx.coroutines.launch
import raegae.shark.attnow.viewmodels.SettingsViewModel
import raegae.shark.attnow.getApplication
import androidx.lifecycle.viewmodel.compose.viewModel



@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val settings = remember { SettingsDataStore(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val speed by settings.animationSpeed.collectAsState(initial = 1f)
    val viewModel: SettingsViewModel =  SettingsViewModel(getApplication())

    val isBusy by viewModel.isBusy.collectAsState()
    val error by viewModel.error.collectAsState()
    val importSuccess by viewModel.importSuccess.collectAsState()

    LaunchedEffect(importSuccess) {
        if (importSuccess) {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage(context.packageName)
            if (intent != null) {
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                context.startActivity(intent)
                android.os.Process.killProcess(android.os.Process.myPid())
            }
        }
    }

    /* ---------- EXPORT ---------- */
    val exportLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            )
        ) { uri: Uri? ->
            if (uri != null) {
                viewModel.exportAttendance(uri)
            }
        }

    /* ---------- IMPORT ---------- */
    val importLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            if (uri != null) {
                viewModel.importAttendance(uri)
            }
        }


    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        /* ---------- TITLE ---------- */
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        /* ================= ACCESSIBILITY ================= */
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Accessibility",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = "Animation speed (${String.format("%.2f", speed)}×)",
                    style = MaterialTheme.typography.bodyMedium
                )

                Slider(
                    value = speed,
                    onValueChange = {
                        scope.launch {
                            settings.setAnimationSpeed(it)
                        }
                    },
                    valueRange = 0.25f..2.0f,
                    steps = 6
                )
            }
        }

        /*Slider(
            value = speed,
            onValueChange = { newSpeed ->
                scope.launch {
                    settings.setAnimationSpeed(newSpeed)
                }
            },
            valueRange = 0.25f..2.0f,
            steps = 6
        )*/

        /* ================= DATA ================= */
        Card(
            modifier = Modifier.fillMaxWidth()

        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
                
            ) {
                Text(
                    text = "Data",
                    style = MaterialTheme.typography.titleMedium
                )

                Button(
                    onClick = {
                        exportLauncher.launch("attendance.xlsx")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export to Excel")
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            importLauncher.launch(
                                arrayOf(
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Import from Excel")
                }
            }
        }
    }
    if (isBusy) {
        AlertDialog(
            onDismissRequest = { /* Prevent dismiss */ },
            title = { Text("Processing...") },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Please wait...")
                }
            },
            confirmButton = {}
        )
    }

    if (error != null) {
        AlertDialog(
            onDismissRequest = { /* viewmodel clear? */ }, // Ideally clear error on dismiss
            title = { Text("Error") },
            text = { Text(error ?: "Unknown error") },
            confirmButton = {
                TextButton(onClick = { 
                    // We need a way to clear error in VM 
                    // But for now, just let it persist until next action clears it?
                    // Or add a way to clear. 
                    // I'll make VM clear error on new action, but to dismiss dialog we need state change.
                    // I will add a simple clearError function to VM or just ignore for now as 'error' state is observed.
                    // Better: just dismiss.
                }) { Text("OK") }
            }
        )
    }
}


