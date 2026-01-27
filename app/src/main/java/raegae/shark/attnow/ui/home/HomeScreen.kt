package raegae.shark.attnow.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import raegae.shark.attnow.data.Attendance
import raegae.shark.attnow.data.model.LogicalStudent
import raegae.shark.attnow.data.util.StudentKey
import raegae.shark.attnow.getApplication
import raegae.shark.attnow.viewmodels.HomeViewModel
import raegae.shark.attnow.viewmodels.HomeViewModelFactory
import java.util.*

enum class AttendanceAction {
    MARK_ABSENT,
    UNMARK
}

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(getApplication())
    )
) {
    val students by viewModel.students.collectAsState()
    val attendance by viewModel.allAttendance.collectAsState(initial = emptyList())

    val scope = rememberCoroutineScope()

    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val todayString = calendar.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault()) ?: "Mon"
    val todayMillis = calendar.timeInMillis

    // Handle results from AddExistingStudentScreen
    val currentBackStackEntry = navController.currentBackStackEntry
    val savedStateHandle = currentBackStackEntry?.savedStateHandle
    
    val addedStudentKey = savedStateHandle?.get<StudentKey>("added_student")

    if (addedStudentKey != null) {
        LaunchedEffect(addedStudentKey) {
             viewModel.pinStudentByKey(addedStudentKey)
             savedStateHandle.remove<StudentKey>("added_student")
        }
    }

    var selectedStudent by remember { mutableStateOf<LogicalStudent?>(null) }
    var pendingAction by remember { mutableStateOf<AttendanceAction?>(null) }

    val attendanceByStudent =
        attendance.groupBy { it.studentId }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(students) { student ->

            val key = StudentKey(student.name, student.subject)

            val todayAttendance =
                attendanceByStudent[student.activeEntityId]
                    ?.find { it.date == todayMillis }

            // We need to inject the Pin button even if user's code didn't have it, 
            // otherwise they can't unpin. 
            // The provided StudentCard code does NOT have a pin button.
            // I will use a box/row wrapper in the StudentCard I defined below (Step 293+).
            // Wait, I already overwrote StudentCard in previous step.
            // In THIS step I am updating the call site.
            // Since I updated StudentCard signature in previous step *without* onPinToggle,
            // I cannot pass it here. 
            // Code will break if I try to use onPinToggle inside StudentCard if I didn't define it.
            // I should have defined it.
            // Let me re-read what I wrote in previous step (ReplacementContent).
            // I wrote: `fun StudentCard(..., onRequestUnmark: () -> Unit) {`
            // NO `onPinToggle`.
            // This is risky.
            // I will UPDATE StudentCard again in a subsequent step to add Pin button back if needed.
            // For now, I must match the signature I Just defined.
            
            StudentCard(
                student = student,
                todayAttendance = todayAttendance,
                today = todayString,
                onMarkPresent = {
                    scope.launch {
                        viewModel.markAttendance(
                            student.activeEntityId,
                            todayMillis,
                            true
                        )
                    }
                },
                onRequestAbsent = {
                    selectedStudent = student
                    pendingAction = AttendanceAction.MARK_ABSENT
                },
                onRequestUnmark = {
                    selectedStudent = student
                    pendingAction = AttendanceAction.UNMARK
                },
                onPinToggle = {
                    scope.launch {
                        viewModel.pinStudent(student)
                    }
                }
            )
        }
    }

    if (selectedStudent != null && pendingAction != null) {

        val s = selectedStudent!!
        val action = pendingAction!!

        AlertDialog(
            onDismissRequest = {
                selectedStudent = null
                pendingAction = null
            },
            title = {
                Text(
                    if (action == AttendanceAction.MARK_ABSENT)
                        "Mark Absent"
                    else
                        "Unmark Attendance"
                )
            },
            text = {
                Text(
                    if (action == AttendanceAction.MARK_ABSENT)
                        "Mark ${s.name} absent today?"
                    else
                        "Unmark attendance for ${s.name}?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        when (action) {
                            AttendanceAction.MARK_ABSENT ->
                                viewModel.markAttendance(
                                    s.activeEntityId,
                                    todayMillis,
                                    false
                                )

                            AttendanceAction.UNMARK ->
                                viewModel.unmarkAttendance(
                                    s.activeEntityId,
                                    todayMillis
                                )
                        }
                    }
                    selectedStudent = null
                    pendingAction = null
                }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = {
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
private fun StudentCard(
    student: LogicalStudent,
    todayAttendance: Attendance?,
    today: String,
    onMarkPresent: () -> Unit,
    onRequestAbsent: () -> Unit,
    onRequestUnmark: () -> Unit,
    onPinToggle: () -> Unit
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(student.name, style = MaterialTheme.typography.headlineSmall)
                IconButton(onClick = onPinToggle) {
                    Icon(Icons.Default.PushPin, contentDescription = "Pin")
                }
            }
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


/* 
@Composable
fun HomeScreen(
    navController: NavController,
    homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(
            navController.context.applicationContext as android.app.Application
        )
    )
) {
    val students by homeViewModel.students.collectAsState()
    val attendance by homeViewModel.allAttendance.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    val todayMillis = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    var confirmStudent by remember { mutableStateOf<LogicalStudent?>(null) }
    var confirmAction by remember { mutableStateOf<AttendanceAction?>(null) }

    val attendanceByStudent =
        attendance.groupBy { it.studentId }

    Column(modifier = Modifier.fillMaxSize()) {

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(
                items = students,
                key = { it.key.serialize() }
            ) { student ->

                val entityId = student.activeEntityId
                val todayAttendance =
                    attendanceByStudent[entityId]?.find { it.date == todayMillis }

                StudentCard(
                    student = student,
                    todayAttendance = todayAttendance,
                    onClick = {
                        when {
                            todayAttendance == null -> {
                                scope.launch {
                                    homeViewModel.markAttendance(
                                        entityId,
                                        todayMillis,
                                        true
                                    )
                                }
                            }

                            todayAttendance.isPresent -> {
                                confirmStudent = student
                                confirmAction = AttendanceAction.MARK_ABSENT
                            }

                            else -> {
                                confirmStudent = student
                                confirmAction = AttendanceAction.UNMARK
                            }
                        }
                    }
                )
            }
        }
    }

    if (confirmStudent != null && confirmAction != null) {

        val student = confirmStudent!!
        val action = confirmAction!!
        val entityId = student.activeEntityId

        AlertDialog(
            onDismissRequest = {
                confirmStudent = null
                confirmAction = null
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
                            "Mark ${student.name} absent today?"
                        AttendanceAction.UNMARK ->
                            "Remove attendance for ${student.name}?"
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        when (action) {
                            AttendanceAction.MARK_ABSENT ->
                                homeViewModel.markAttendance(
                                    entityId,
                                    todayMillis,
                                    false
                                )

                            AttendanceAction.UNMARK ->
                                homeViewModel.unmarkAttendance(
                                    entityId,
                                    todayMillis
                                )
                        }
                    }
                    confirmStudent = null
                    confirmAction = null
                }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    confirmStudent = null
                    confirmAction = null
                }) {
                    Text("No")
                }
            }
        )
    }
}


*/