package raegae.shark.first_app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import raegae.shark.first_app.data.Attendance
import raegae.shark.first_app.data.Student
import raegae.shark.first_app.getApplication
import raegae.shark.first_app.ui.theme.First_appTheme
import raegae.shark.first_app.viewmodels.StudentProfileViewModel
import raegae.shark.first_app.viewmodels.StudentProfileViewModelFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun StudentProfileScreen(
    navController: NavController,
    studentId: Int,
    viewModel: StudentProfileViewModel = viewModel(factory = StudentProfileViewModelFactory(getApplication(), studentId))
) {
    val student by viewModel.student.collectAsState(initial = Student(id = 0, name = "", subject = "", subscriptionEndDate = 0, batchTime = "", daysOfWeek = emptyList()))
    val attendance by viewModel.attendance.collectAsState(initial = emptyList())
    var currentMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = student.name, style = MaterialTheme.typography.headlineMedium)
        Text(text = student.subject, style = MaterialTheme.typography.titleMedium)
        Text(text = "Batch: ${student.batchTime} on ${student.daysOfWeek.joinToString()}", style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { showDeleteDialog = true }) {
            Text("Delete Student")
        }

        Spacer(modifier = Modifier.height(32.dp))

        AttendanceCalendar(
            currentMonth = currentMonth,
            onMonthChange = { currentMonth = it },
            attendance = attendance,
            subscriptionEndDate = student.subscriptionEndDate
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Student") },
            text = { Text("Are you sure you want to delete ${student.name}? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch {
                        viewModel.deleteStudent()
                        navController.popBackStack()
                    }
                    showDeleteDialog = false
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AttendanceCalendar(currentMonth: Calendar, onMonthChange: (Calendar) -> Unit, attendance: List<Attendance>, subscriptionEndDate: Long) {
    val monthFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                val newMonth = currentMonth.clone() as Calendar
                newMonth.add(Calendar.MONTH, -1)
                onMonthChange(newMonth)
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
            }
            Text(
                text = monthFormatter.format(currentMonth.time),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = {
                val newMonth = currentMonth.clone() as Calendar
                newMonth.add(Calendar.MONTH, 1)
                onMonthChange(newMonth)
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val daysInMonth = currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfMonth = currentMonth.clone() as Calendar
        firstDayOfMonth.set(Calendar.DAY_OF_MONTH, 1)
        val startDayOfWeek = firstDayOfMonth.get(Calendar.DAY_OF_WEEK)

        val calendarDays = (1..daysInMonth).map { it.toString() }.toMutableList()
        for (i in 1 until startDayOfWeek) {
            calendarDays.add(0, "")
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")) { day ->
                Box(contentAlignment = Alignment.Center) {
                    Text(text = day, fontWeight = FontWeight.Bold)
                }
            }
            items(calendarDays) { day ->
                if (day.isNotEmpty()) {
                    val dayNumber = day.toInt()
                    val date = (currentMonth.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, dayNumber) }
                    val attendanceForDay = attendance.find { attendanceDate ->
                        val cal = Calendar.getInstance().apply { timeInMillis = attendanceDate.date }
                        cal.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
                                cal.get(Calendar.MONTH) == date.get(Calendar.MONTH) &&
                                cal.get(Calendar.DAY_OF_MONTH) == date.get(Calendar.DAY_OF_MONTH)
                    }

                    val isFuture = isFutureDate(dayNumber, currentMonth)

                    val isExpired = date.timeInMillis > subscriptionEndDate

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(
                                when {
                                    isExpired -> Color(0xFFB0B0B0)
                                    attendanceForDay?.isPresent == true -> Color(0xFF4CAF50)
                                    attendanceForDay != null && !attendanceForDay.isPresent -> Color(0xFFF44336)
                                    else -> Color(0xFF2196F3)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = day, color = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(12.dp).background(Color(0xFFB0B0B0)))
                Text("Expired", style = MaterialTheme.typography.bodySmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(12.dp).background(Color(0xFF4CAF50)))
                Text("Present", style = MaterialTheme.typography.bodySmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(12.dp).background(Color(0xFFF44336)))
                Text("Absent", style = MaterialTheme.typography.bodySmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(12.dp).background(Color(0xFF2196F3)))
                Text("Not Marked", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun isFutureDate(day: Int, currentMonth: Calendar): Boolean {
    val today = Calendar.getInstance()
    val date = currentMonth.clone() as Calendar
    date.set(Calendar.DAY_OF_MONTH, day)
    return date.after(today) && !(today.get(Calendar.YEAR) == date.get(Calendar.YEAR) && today.get(Calendar.MONTH) == date.get(Calendar.MONTH) && today.get(Calendar.DAY_OF_MONTH) == date.get(Calendar.DAY_OF_MONTH))
}

@Preview(showBackground = true)
@Composable
fun StudentProfileScreenPreview() {
    First_appTheme {
        StudentProfileScreen(navController = rememberNavController(), studentId = 1)
    }
}
