package raegae.shark.attnow.ui.edit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.*
import raegae.shark.attnow.data.Student
import raegae.shark.attnow.viewmodels.EditEntitiesViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EditEntitiesListScreen(navController: NavController, viewModel: EditEntitiesViewModel) {
    val entities by viewModel.entities.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()

    var showSaveDialog by remember { mutableStateOf(false) }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Edit Entities") },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                            }
                        },
                        actions = { Button(onClick = { showSaveDialog = true }) { Text("Save") } }
                )
            }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {

            // Action Bar
            Row(
                    Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                        onClick = { viewModel.selectAll() },
                        modifier = Modifier.weight(1f)
                ) { Text("Select All") }
                OutlinedButton(
                        onClick = { viewModel.inverseSelection() },
                        modifier = Modifier.weight(1f)
                ) { Text("Inverse") }
            }

            if (selectedIds.isNotEmpty()) {
                Button(
                        onClick = { viewModel.deleteSelected() },
                        colors =
                                ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                ),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.Delete, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Delete Selected (${selectedIds.size})")
                }
            }

            LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(entities, key = { it.id }) { student ->
                    val isSelected = selectedIds.contains(student.id)

                    EntityCard(
                            student = student,
                            isSelected = isSelected,
                            onClick = {
                                if (selectedIds.isNotEmpty()) {
                                    viewModel.toggleSelection(student.id)
                                } else {
                                    navController.navigate("edit_entity_detail/${student.id}")
                                }
                            },
                            onLongClick = { viewModel.toggleSelection(student.id) }
                    )
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                title = { Text("Save Changes?") },
                text = { Text("Applies deletions and updates permanently.") },
                confirmButton = {
                    TextButton(
                            onClick = {
                                viewModel.saveChanges()
                                showSaveDialog = false
                                navController.popBackStack()
                            }
                    ) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
                }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EntityCard(
        student: Student,
        isSelected: Boolean,
        onClick: () -> Unit,
        onLongClick: () -> Unit
) {
    val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    Card(
            modifier =
                    Modifier.fillMaxWidth()
                            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
            border =
                    if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
            colors =
                    if (isSelected)
                            CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                    else CardDefaults.cardColors()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                    "${formatter.format(Date(student.subscriptionStartDate))} - ${formatter.format(Date(student.subscriptionEndDate))}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                    "Batch: " + student.batchTimes.entries.joinToString { "${it.key} ${it.value}" },
                    style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
