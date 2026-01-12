package raegae.shark.first_app.ui.home

import androidx.compose.material.icons.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import raegae.shark.first_app.data.Student
import raegae.shark.first_app.getApplication
import raegae.shark.first_app.viewmodels.HomeViewModel
import raegae.shark.first_app.viewmodels.HomeViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExistingStudentScreen(
    navController: NavController,
    onStudentSelected: (Int) -> Unit
) {
    val homeViewModel: HomeViewModel =
        viewModel(factory = HomeViewModelFactory(getApplication()))

    val students by homeViewModel.students.collectAsState(initial = emptyList())
    var query by remember { mutableStateOf("") }

    val filtered = students.filter {
        it.name.contains(query, true) || it.subject.contains(query, true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add student to today") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .padding(16.dp)
        ) {

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            LazyColumn {
                items(students) { student ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp)
                            .clickable {
                                onStudentSelected(student.id)
                                navController.popBackStack()
                            },
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(student.name, style = MaterialTheme.typography.titleMedium)
                            Text(student.subject, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

        }
    }
}
