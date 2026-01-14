package raegae.shark.attnow.ui.profiles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import raegae.shark.attnow.getApplication
import raegae.shark.attnow.viewmodels.ProfilesViewModel
import raegae.shark.attnow.viewmodels.ProfilesViewModelFactory

import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

@Composable
fun ProfilesScreen(
    navController: NavController,
    profilesViewModel: ProfilesViewModel = viewModel(factory = ProfilesViewModelFactory(getApplication()))
) {
    val students by profilesViewModel.students.collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }

    val filteredStudents = students.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.subject.contains(searchQuery, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search Students") },
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredStudents) { student ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("profile/${student.id}") }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = student.name, style = MaterialTheme.typography.headlineSmall)
                        Text(text = student.subject, style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Days: ${student.daysOfWeek.joinToString()}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}