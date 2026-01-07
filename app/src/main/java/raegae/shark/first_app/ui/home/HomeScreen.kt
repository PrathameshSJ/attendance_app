package raegae.shark.first_app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import raegae.shark.first_app.R
import raegae.shark.first_app.data.Attendance
import raegae.shark.first_app.data.Student
import raegae.shark.first_app.getApplication
import raegae.shark.first_app.ui.theme.First_appTheme
import raegae.shark.first_app.viewmodels.HomeViewModel
import raegae.shark.first_app.viewmodels.HomeViewModelFactory
import java.util.Calendar
import java.util.Locale

@Composable
fun HomeScreen(
    navController: NavController,
    homeViewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(getApplication()))
) {
    val students by homeViewModel.students.collectAsState(initial = emptyList())
    val allAttendance by homeViewModel.allAttendance.collectAsState(initial = emptyList())
    val today = Calendar.getInstance().getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault())
    val todayMillis = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val todaysStudents = students.filter { student ->
        student.daysOfWeek.any { it.equals(today, ignoreCase = true) }
    }

    val attendanceMap = allAttendance.groupBy { it.studentId }

    var showAbsentDialog by remember { mutableStateOf(false) }
    var studentToAbsent by remember { mutableStateOf<Student?>(null) }
    var showUnmarkDialog by remember { mutableStateOf(false) }
    var studentToUnmark by remember { mutableStateOf<Student?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(todaysStudents) { student ->
            val todayAttendance = attendanceMap[student.id]?.find { it.date == todayMillis }
            StudentCard(
                student = student,
                todayAttendance = todayAttendance,
                onStudentClicked = { navController.navigate("profile/${student.id}") },
                onMarkPresent = { homeViewModel.markAttendance(student.id, todayMillis, true) },
                onShowAbsentDialog = {
                    showAbsentDialog = true
                    studentToAbsent = student
                },
                onShowUnmarkDialog = {
                    showUnmarkDialog = true
                    studentToUnmark = student
                }
            )
        }
    }

    if (showAbsentDialog && studentToAbsent != null) {
        AlertDialog(
            onDismissRequest = { showAbsentDialog = false },
            title = { Text("Mark as Absent") },
            text = { Text("Are you sure you want to mark ${studentToAbsent!!.name} as absent?") },
            confirmButton = {
                TextButton(onClick = {
                    homeViewModel.markAttendance(studentToAbsent!!.id, todayMillis, false)
                    showAbsentDialog = false
                }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAbsentDialog = false }) {
                    Text("No")
                }
            }
        )
    }

    if (showUnmarkDialog && studentToUnmark != null) {
        AlertDialog(
            onDismissRequest = { showUnmarkDialog = false },
            title = { Text("Unmark Attendance") },
            text = { Text("Are you sure you want to unmark attendance for ${studentToUnmark!!.name}?") },
            confirmButton = {
                TextButton(onClick = {
                    homeViewModel.unmarkAttendance(studentToUnmark!!.id, todayMillis)
                    showUnmarkDialog = false
                }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnmarkDialog = false }) {
                    Text("No")
                }
            }
        )
    }
}

@Composable
fun StudentCard(student: Student, todayAttendance: Attendance?, onStudentClicked: () -> Unit, onMarkPresent: () -> Unit, onShowAbsentDialog: () -> Unit, onShowUnmarkDialog: () -> Unit) {
    val backgroundColor = when {
        todayAttendance?.isPresent == true -> Color.Green.copy(alpha = 0.2f)
        todayAttendance?.isPresent == false -> Color.Red.copy(alpha = 0.2f)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                when {
                    todayAttendance == null -> onMarkPresent()
                    todayAttendance.isPresent -> onShowAbsentDialog()
                    else -> onShowUnmarkDialog()
                }
            },
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(text = student.name, style = MaterialTheme.typography.titleMedium)
            Text(text = student.subject, style = MaterialTheme.typography.bodyMedium)
            Text(text = "Time: ${student.batchTime}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Days: ${student.daysOfWeek.joinToString()}", style = MaterialTheme.typography.bodySmall)
            val statusText = when {
                todayAttendance?.isPresent == true -> "Present"
                todayAttendance?.isPresent == false -> "Absent"
                else -> "Not Marked"
            }
            Text(text = "Status: $statusText", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    First_appTheme {
        val students = listOf(
            Student(id = 1, name = "John Doe", subject = "Math", subscriptionEndDate = 0, batchTime = "4:00 PM", daysOfWeek = listOf("Mon", "Wed", "Fri")),
            Student(id = 2, name = "Jane Smith", subject = "English", subscriptionEndDate = 0, batchTime = "5:00 PM", daysOfWeek = listOf("Tue", "Thu")),
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(students) { student ->
                StudentCard(student = student, todayAttendance = null, onStudentClicked = {}, onMarkPresent = {}, onShowAbsentDialog = {})
            }
        }
    }
}
