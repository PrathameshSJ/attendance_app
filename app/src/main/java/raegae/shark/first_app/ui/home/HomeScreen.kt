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
import androidx.navigation.compose.*
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import raegae.shark.first_app.data.Attendance
import raegae.shark.first_app.data.Student
import raegae.shark.first_app.getApplication
import raegae.shark.first_app.ui.theme.First_appTheme
import raegae.shark.first_app.viewmodels.HomeViewModel
import raegae.shark.first_app.viewmodels.HomeViewModelFactory
import java.util.Calendar
import java.util.Locale

/* ---------- SINGLE SOURCE OF TRUTH ---------- */
enum class AttendanceAction {
    MARK_ABSENT,
    UNMARK
}

@Composable
fun HomeScreen(
    navController: NavController,
    homeViewModel: HomeViewModel =
        viewModel(factory = HomeViewModelFactory(getApplication()))
) {

    val backStackEntry = navController.currentBackStackEntryAsState().value
    val addedStudentId =
        backStackEntry?.savedStateHandle?.get<Int>("added_student")


    val students by homeViewModel.students.collectAsState(initial = emptyList())
    val allAttendance by homeViewModel.allAttendance.collectAsState(initial = emptyList())
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
        Locale.getDefault()
    ) ?: "Mon"

    /* ---------- Scheduled students ---------- */
    val scheduled = students.filter {
        it.daysOfWeek.any { it.equals(today, true) }
    }

    /* ---------- Pinned students (added manually) ---------- */
    val pinnedStudents by homeViewModel.pinnedStudents.collectAsState(initial = emptySet())


    LaunchedEffect(addedStudentId) {
        if (addedStudentId != null) {
            homeViewModel.pinStudent(addedStudentId)
            backStackEntry?.savedStateHandle?.remove<Int>("added_student")
        }
    }


    val pinned: List<Student> = pinnedStudents.mapNotNull { id ->
        students.find { s -> s.id == id }
    }




    val todaysStudents = (scheduled + pinned).distinctBy { it.id }

    val attendanceMap = allAttendance.groupBy { it.studentId }

    /* ---------- Dialog state ---------- */
    var showConfirmDialog by remember { mutableStateOf(false) }
    var selectedStudent by remember { mutableStateOf<Student?>(null) }
    var pendingAction by remember { mutableStateOf<AttendanceAction?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(todaysStudents) { student ->
                val todayAttendance =
                    attendanceMap[student.id]?.find { it.date == todayMillis }

                StudentCard(
                    student = student,
                    todayAttendance = todayAttendance,
                    today = today,
                    onMarkPresent = {
                        scope.launch {
                            homeViewModel.markAttendance(
                                student.id,
                                todayMillis,
                                true
                            )
                        }
                    },
                    onRequestAbsent = {
                        selectedStudent = student
                        pendingAction = AttendanceAction.MARK_ABSENT
                        showConfirmDialog = true
                    },
                    onRequestUnmark = {
                        selectedStudent = student
                        pendingAction = AttendanceAction.UNMARK
                        showConfirmDialog = true
                    }
                )
            }
        }
    }

    /* ---------- Confirmation dialog ---------- */
    if (showConfirmDialog && selectedStudent != null && pendingAction != null) {

        val studentId = selectedStudent!!.id
        val studentName = selectedStudent!!.name
        val action = pendingAction!!

        AlertDialog(
            onDismissRequest = {
                showConfirmDialog = false
                selectedStudent = null
                pendingAction = null
            },
            title = {
                Text(
                    when (action) {
                        AttendanceAction.MARK_ABSENT -> "Mark Absent"
                        AttendanceAction.UNMARK -> "Unmark Attendance"
                    }
                )
            },
            text = {
                Text(
                    when (action) {
                        AttendanceAction.MARK_ABSENT ->
                            "Mark $studentName as absent?"
                        AttendanceAction.UNMARK ->
                            "Unmark attendance for $studentName?"
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {

                    showConfirmDialog = false
                    selectedStudent = null
                    pendingAction = null

                    scope.launch {
                        when (action) {
                            AttendanceAction.MARK_ABSENT ->
                                homeViewModel.markAttendance(
                                    studentId,
                                    todayMillis,
                                    false
                                )

                            AttendanceAction.UNMARK ->
                                homeViewModel.unmarkAttendance(
                                    studentId,
                                    todayMillis
                                )
                        }
                    }
                }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    selectedStudent = null
                    pendingAction = null
                }) {
                    Text("No")
                }
            }
        )
    }
}

@Composable
fun StudentCard(
    student: Student,
    todayAttendance: Attendance?,
    today: String,
    onMarkPresent: () -> Unit,
    onRequestAbsent: () -> Unit,
    onRequestUnmark: () -> Unit
) {
    val backgroundColor = when (todayAttendance?.isPresent) {
        true -> Color.Green.copy(alpha = 0.2f)
        false -> Color.Red.copy(alpha = 0.2f)
        null -> Color.Gray.copy(alpha = 0.15f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                when {
                    todayAttendance == null -> onMarkPresent()
                    todayAttendance.isPresent -> onRequestAbsent()
                    else -> onRequestUnmark()
                }
            },
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(student.name, style = MaterialTheme.typography.headlineSmall)
            Text(student.subject)
            Text("Time: ${student.batchTimes[today] ?: "N/A"}")
            Text("Days: ${student.daysOfWeek.joinToString()}")
            Text(
                "Status: ${
                    when (todayAttendance?.isPresent) {
                        true -> "Present"
                        false -> "Absent"
                        null -> "Not Marked"
                    }
                }"
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    First_appTheme {
        StudentCard(
            student = Student(
                id = 1,
                name = "John Doe",
                subject = "Math",
                subscriptionStartDate = 0,
                subscriptionEndDate = 0,
                batchTimes = mapOf("Mon" to "4:00 PM"),
                daysOfWeek = listOf("Mon")
            ),
            todayAttendance = null,
            today = "Mon",
            onMarkPresent = {},
            onRequestAbsent = {},
            onRequestUnmark = {}
        )
    }
}
