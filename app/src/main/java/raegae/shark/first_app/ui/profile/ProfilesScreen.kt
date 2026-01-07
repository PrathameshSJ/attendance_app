package raegae.shark.first_app.ui.profile

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import raegae.shark.first_app.data.Student
import raegae.shark.first_app.getApplication
import raegae.shark.first_app.ui.theme.First_appTheme

@Composable
fun ProfilesScreen(
    navController: NavController,
    viewModel: ProfilesViewModel = viewModel(factory = ProfilesViewModelFactory(getApplication()))
) {
    val students by viewModel.students.collectAsState(initial = emptyList())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(students) { student ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate("profile/${student.id}") }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = student.name, style = MaterialTheme.typography.titleMedium)
                    Text(text = student.subject, style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Batch: ${student.daysOfWeek.joinToString()}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProfilesScreenPreview() {
    First_appTheme {
        ProfilesScreen(navController = rememberNavController())
    }
}