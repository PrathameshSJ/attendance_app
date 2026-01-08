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
import androidx.compose.material.ExposedDropdownMenuBox
import androidx.compose.material3.TextButton

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.MaterialTheme
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

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun StudentProfileScreen(
    navController: NavController,
    studentId: Int,
    viewModel: StudentProfileViewModel = viewModel(factory = StudentProfileViewModelFactory(getApplication(), studentId))
) {
    val student by viewModel.student.collectAsState(initial = Student(id = 0, name = "", subject = "", subscriptionStartDate = 0, subscriptionEndDate = 0, batchTimes = emptyMap(), daysOfWeek = emptyList()))
    val attendance by viewModel.attendance.collectAsState(initial = emptyList())
    var currentMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRenewDialog by remember { mutableStateOf(false) }
    val weekDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    var renewMonths by remember { mutableStateOf("1") }
    var renewDays by remember { mutableStateOf("0") }
    var renewExpandedMonths by remember { mutableStateOf(false) }
    var renewExpandedDays by remember { mutableStateOf(false) }
    var renewDayEnabledStates = remember { weekDays.map { mutableStateOf(student.daysOfWeek.contains(it)) } }
    var renewDayStartTimesStates = remember { weekDays.map { mutableStateOf(student.batchTimes[it]?.split(" - ")?.get(0) ?: "9:00 AM") } }
    var renewDayEndTimesStates = remember { weekDays.map { mutableStateOf(student.batchTimes[it]?.split(" - ")?.get(1) ?: "10:00 AM") } }
    val coroutineScope = rememberCoroutineScope()

    val subscriptionStartDate = student.subscriptionStartDate
    val subscriptionEndDate = student.subscriptionEndDate

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = student.name, style = MaterialTheme.typography.headlineMedium)
        Text(text = student.subject, style = MaterialTheme.typography.titleMedium)
        Text(text = "Batch Times: ${student.batchTimes.entries.joinToString { "${it.key}: ${it.value}" }}", style = MaterialTheme.typography.bodyMedium)

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { showDeleteDialog = true }) {
            Text("Delete Student")
        }

        Button(onClick = { showRenewDialog = true }) {
            Text("Renew Subscription")
        }

        Spacer(modifier = Modifier.height(32.dp))

        AttendanceCalendar(
            currentMonth = currentMonth,
            onMonthChange = { currentMonth = it },
            attendance = attendance,
            subscriptionStartDate = student.subscriptionStartDate,
            subscriptionEndDate = student.subscriptionEndDate
        )
    }
    if (showRenewDialog) {
        AlertDialog(
            onDismissRequest = { showRenewDialog = false },
            title = { Text("Renew Subscription") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Extend subscription by:")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = renewExpandedMonths,
                            onExpandedChange = { renewExpandedMonths = !renewExpandedMonths }
                        ) {
                            OutlinedTextField(
                                value = renewMonths,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Months") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = renewExpandedMonths) },
                                modifier = Modifier.weight(1f)
                            )
                            ExposedDropdownMenu(
                                expanded = renewExpandedMonths,
                                onDismissRequest = { renewExpandedMonths = false }
                            ) {
                                (1..12).forEach { month ->
                                    DropdownMenuItem(
                                        text = { Text(month.toString()) },
                                        onClick = {
                                            renewMonths = month.toString()
                                            renewExpandedMonths = false
                                        }
                                    )
                                }
                            }
                        }
                        ExposedDropdownMenuBox(
                            expanded = renewExpandedDays,
                            onExpandedChange = { renewExpandedDays = !renewExpandedDays }
                        ) {
                            OutlinedTextField(
                                value = renewDays,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Days") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = renewExpandedDays) },
                                modifier = Modifier.weight(1f)
                            )
                            ExposedDropdownMenu(
                                expanded = renewExpandedDays,
                                onDismissRequest = { renewExpandedDays = false }
                            ) {
                                (0..30).forEach { day ->
                                    DropdownMenuItem(
                                        text = { Text(day.toString()) },
                                        onClick = {
                                            renewDays = day.toString()
                                            renewExpandedDays = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Text("Update Batch Days:")
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        weekDays.forEach { day ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Switch(
                                    checked = renewDayEnabledStates[weekDays.indexOf(day)].value,
                                    onCheckedChange = { renewDayEnabledStates[weekDays.indexOf(day)].value = it }
                                )
                                Text(day, modifier = Modifier.weight(1f))
                                if (renewDayEnabledStates[weekDays.indexOf(day)].value) {
                                    Row(
                                        modifier = Modifier.weight(2f),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = renewDayStartTimesStates[weekDays.indexOf(day)].value,
                                            onValueChange = { renewDayStartTimesStates[weekDays.indexOf(day)].value = it },
                                            label = { Text("Start") },
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = renewDayEndTimesStates[weekDays.indexOf(day)].value,
                                            onValueChange = { renewDayEndTimesStates[weekDays.indexOf(day)].value = it },
                                            label = { Text("End") },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val monthsInt = renewMonths.toIntOrNull() ?: 0
                    val daysInt = renewDays.toIntOrNull() ?: 0
                    val calendar = Calendar.getInstance().apply { timeInMillis = subscriptionEndDate }
                    calendar.add(Calendar.MONTH, monthsInt)
                    calendar.add(Calendar.DAY_OF_MONTH, daysInt)
                    val newEndDate = calendar.timeInMillis
                    val newBatchTimes = weekDays.filterIndexed { index, _ -> renewDayEnabledStates[index].value }.associateWith { "${renewDayStartTimesStates[weekDays.indexOf(it)].value} - ${renewDayEndTimesStates[weekDays.indexOf(it)].value}" }
                    val newDays = newBatchTimes.keys.toList()
                    val updatedStudent = student.copy(subscriptionEndDate = newEndDate, batchTimes = newBatchTimes, daysOfWeek = newDays)
                    coroutineScope.launch {
                        viewModel.updateStudent(updatedStudent)
                    }
                    showRenewDialog = false
                }) {
                    Text("Renew")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenewDialog = false }) {
                    Text("Cancel")
                }
            }
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
fun AttendanceCalendar(currentMonth: Calendar, onMonthChange: (Calendar) -> Unit, attendance: List<Attendance>, subscriptionStartDate: Long, subscriptionEndDate: Long) {
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
                    val attendanceForDay = if (date.timeInMillis >= subscriptionStartDate && date.timeInMillis <= subscriptionEndDate) attendance.find { attendanceDate ->
                        val cal = Calendar.getInstance().apply { timeInMillis = attendanceDate.date }
                        cal.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
                                cal.get(Calendar.MONTH) == date.get(Calendar.MONTH) &&
                                cal.get(Calendar.DAY_OF_MONTH) == date.get(Calendar.DAY_OF_MONTH)
                    } else null

                    val isExpired = date.timeInMillis > subscriptionEndDate || date.timeInMillis < subscriptionStartDate

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
