package raegae.shark.first_app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import raegae.shark.first_app.data.Attendance
import raegae.shark.first_app.data.Student
import raegae.shark.first_app.getApplication
import raegae.shark.first_app.ui.theme.First_appTheme
import raegae.shark.first_app.viewmodels.HomeViewModel
import raegae.shark.first_app.viewmodels.HomeViewModelFactory
import java.util.*

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(getApplication())
    )
) {
    val students by homeViewModel.students.collectAsState(initial = emptyList())
    val attendance by homeViewModel.allAttendance.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    val calendar = Calendar.getInstance()
    val todayMillis = calendar.apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val today = calendar.getDisplayName(
        Calendar.DAY_OF_WEEK,
        Calendar.SHORT,
        Locale.ENGLISH
    ) ?: "Mon"

    val attendanceMap = attendance.groupBy { it.studentId }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(students) { student ->
            val todayAttendance =
                attendanceMap[student.id]?.find { it.date == todayMillis }

            StudentCard(
                student = student,
                todayAttendance = todayAttendance,
                today = today,
                onMarkPresent = {
                    scope.launch {
                        homeViewModel.markAttendance(student.id, todayMillis, true)
                    }
                },
                onShowAbsentDialog = {},
                onShowUnmarkDialog = {}
            )
        }
    }
}

@Composable
fun StudentCard(
    student: Student,
    todayAttendance: Attendance?,
    today: String,
    onMarkPresent: () -> Unit,
    onShowAbsentDialog: () -> Unit,
    onShowUnmarkDialog: () -> Unit
) {
    val color = when (todayAttendance?.isPresent) {
        true -> Color.Green.copy(0.2f)
        false -> Color.Red.copy(0.2f)
        null -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onMarkPresent() },
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(student.name, style = MaterialTheme.typography.headlineSmall)
            Text(student.subject)
            Text("Time: ${student.batchTimes[today] ?: "N/A"}")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    First_appTheme {
        HomeScreen()
    }
}
