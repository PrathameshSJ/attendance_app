package raegae.shark.attnow.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
import raegae.shark.attnow.viewmodels.DriveViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
        navController: androidx.navigation.NavController,
        viewModel: DriveViewModel,
        onRestore: (String) -> Unit
) {
    val account by viewModel.authManager.signedInAccount.collectAsState()
    val backups by viewModel.backups.collectAsState()
    val isBusy by viewModel.isBusy.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { viewModel.refreshBackups() }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Google Account") },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                            }
                        },
                        actions = {
                            IconButton(onClick = { viewModel.refreshBackups() }) {
                                Icon(Icons.Default.Refresh, "Refresh")
                            }
                        }
                )
            }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            // Header
            account?.let { acct ->
                Card(
                        Modifier.fillMaxWidth().padding(16.dp),
                        colors =
                                CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                        text = acct.displayName ?: "User",
                                        style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                        text = acct.email ?: "",
                                        style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Button(
                                onClick = {
                                    scope.launch {
                                        viewModel.authManager.signOut()
                                        // Navigate back to Settings to refresh state
                                        navController.popBackStack()
                                    }
                                },
                                colors =
                                        ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error
                                        ),
                                modifier = Modifier.fillMaxWidth()
                        ) { Text("Log Out") }
                    }
                }
            }

            HorizontalDivider()

            Text(
                    "Cloud Backups (Tap to Restore)",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
            )

            if (isBusy && backups.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (backups.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No backups found in 'Attendance' folder.")
                }
            } else {
                LazyColumn { items(backups) { file -> BackupItem(file) { onRestore(file.id) } } }
            }
        }

        if (isBusy) {
            // Overlay blocking interaction
            Box(
                    Modifier.fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                            .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        }
    }
}

@Composable
fun BackupItem(file: com.google.api.services.drive.model.File, onClick: () -> Unit) {
    val date =
            file.createdTime?.value?.let {
                SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(it))
            }
                    ?: "Unknown Date"

    ListItem(
            modifier = Modifier.clickable(onClick = onClick),
            headlineContent = { Text(file.name) },
            supportingContent = { Text(date) },
            trailingContent = { Icon(Icons.Default.CloudDownload, "Restore") }
    )
    HorizontalDivider()
}
