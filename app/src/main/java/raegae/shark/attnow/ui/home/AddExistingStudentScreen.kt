package raegae.shark.attnow.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import raegae.shark.attnow.data.model.LogicalStudent
import raegae.shark.attnow.data.util.StudentKey
import raegae.shark.attnow.getApplication
import raegae.shark.attnow.viewmodels.HomeViewModel
import raegae.shark.attnow.viewmodels.HomeViewModelFactory

@Composable
fun AddExistingStudentScreen(
    navController: NavController,
    onStudentSelected: (StudentKey) -> Unit
) {
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(getApplication())
    )

    val students by viewModel.availableForPinning.collectAsState()

    var query by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp)) {

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search student") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(
                students.filter {
                    query.isBlank() ||
                    it.name.contains(query, true) ||
                    it.subject.contains(query, true)
                }
            ) { student ->

                ExistingStudentRow(
                    student = student,
                    onClick = {
                        onStudentSelected(
                            StudentKey(
                                name = student.name,
                                subject = student.subject
                            )
                        )
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

@Composable
private fun ExistingStudentRow(
    student: LogicalStudent,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(student.name, style = MaterialTheme.typography.titleMedium)
            Text(student.subject, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
