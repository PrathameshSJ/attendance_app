package raegae.shark.first_app.ui.add

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import raegae.shark.first_app.getApplication
import raegae.shark.first_app.viewmodels.AddStudentViewModel
import raegae.shark.first_app.viewmodels.AddStudentViewModelFactory
import java.util.Calendar
import java.util.Locale

fun parseTime(time: String){}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStudentScreen(
    navController: NavController,
    addStudentViewModel: AddStudentViewModel =
        viewModel(factory = AddStudentViewModelFactory(getApplication()))
) {
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }

    val weekDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val dayEnabledStates = remember { weekDays.map { mutableStateOf(false) } }
    val dayStartTimesStates = remember { weekDays.map { mutableStateOf("9:00 AM") } }
    val dayEndTimesStates = remember { weekDays.map { mutableStateOf("10:00 AM") } }

    /* ---------- Subscription duration ---------- */

    var months by remember { mutableStateOf("1") }
    var days by remember { mutableStateOf("0") }
    var expandedMonths by remember { mutableStateOf(false) }
    var expandedDays by remember { mutableStateOf(false) }

    /* ---------- Time picker control ---------- */

    var showTimePicker by remember { mutableStateOf(false) }
    var activeDayIndex by remember { mutableStateOf(0) }
    var pickingStart by remember { mutableStateOf(true) }

    var pickerHour by remember { mutableStateOf(9) }
    var pickerMinute by remember { mutableStateOf(0) }

    val timePickerState = rememberTimePickerState(
        initialHour = pickerHour,
        initialMinute = pickerMinute,
        is24Hour = false
    )

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(Calendar.MINUTE, timePickerState.minute)
                    }

                    val formatted = String.format(
                        Locale.getDefault(),
                        "%d:%02d %s",
                        if (cal.get(Calendar.HOUR) == 0) 12 else cal.get(Calendar.HOUR),
                        cal.get(Calendar.MINUTE),
                        if (cal.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
                    )

                    if (pickingStart) {
                        dayStartTimesStates[activeDayIndex].value = formatted
                    } else {
                        dayEndTimesStates[activeDayIndex].value = formatted
                    }

                    showTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Select Time") },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Student") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    Button(onClick = {
                        val monthsInt = months.toIntOrNull() ?: 0
                        val daysInt = days.toIntOrNull() ?: 0

                        val cal = Calendar.getInstance()
                        val startDate = cal.timeInMillis
                        cal.add(Calendar.MONTH, monthsInt)
                        cal.add(Calendar.DAY_OF_MONTH, daysInt)
                        val endDate = cal.timeInMillis

                        val selectedDays = weekDays.filterIndexed { i, _ ->
                            dayEnabledStates[i].value
                        }

                        val batchTimes = selectedDays.associateWith { day ->
                            val i = weekDays.indexOf(day)
                            "${dayStartTimesStates[i].value} - ${dayEndTimesStates[i].value}"
                        }

                        scope.launch {
                            addStudentViewModel.addStudentIfNotExists(
                                name = name,
                                subject = subject,
                                subscriptionStartDate = startDate,
                                subscriptionEndDate = endDate,
                                batchTimes = batchTimes,
                                daysOfWeek = selectedDays
                            )
                            navController.popBackStack()
                        }
                    }) {
                        Text("Add")
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Student Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text("Subject") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Select Batch Days", style = MaterialTheme.typography.titleMedium)

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                weekDays.forEachIndexed { index, day ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = dayEnabledStates[index].value,
                            onCheckedChange = { dayEnabledStates[index].value = it }
                        )

                        Spacer(Modifier.width(12.dp))
                        Text(day, modifier = Modifier.width(40.dp))
                        Spacer(Modifier.width(16.dp))

                        if (dayEnabledStates[index].value) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                                AssistChip(
                                    onClick = {
                                        activeDayIndex = index
                                        pickingStart = true
                                        parseTime(dayStartTimesStates[index].value)
                                        showTimePicker = true
                                    },
                                    label = { Text(dayStartTimesStates[index].value) }
                                )

                                AssistChip(
                                    onClick = {
                                        activeDayIndex = index
                                        pickingStart = false
                                        parseTime(dayEndTimesStates[index].value)
                                        showTimePicker = true
                                    },
                                    label = { Text(dayEndTimesStates[index].value) }
                                )
                            }
                        }
                    }
                }
            }

            /* ---------- Months / Days ---------- */

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {

                ExposedDropdownMenuBox(
                    expanded = expandedMonths,
                    onExpandedChange = { expandedMonths = !expandedMonths }
                ) {
                    OutlinedTextField(
                        value = months,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Months") },
                        modifier = Modifier
                            .width(90.dp)
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedMonths,
                        onDismissRequest = { expandedMonths = false }
                    ) {
                        (1..12).forEach {
                            DropdownMenuItem(
                                text = { Text(it.toString()) },
                                onClick = {
                                    months = it.toString()
                                    expandedMonths = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.width(16.dp))

                ExposedDropdownMenuBox(
                    expanded = expandedDays,
                    onExpandedChange = { expandedDays = !expandedDays }
                ) {
                    OutlinedTextField(
                        value = days,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Days") },
                        modifier = Modifier
                            .width(90.dp)
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedDays,
                        onDismissRequest = { expandedDays = false }
                    ) {
                        (0..30).forEach {
                            DropdownMenuItem(
                                text = { Text(it.toString()) },
                                onClick = {
                                    days = it.toString()
                                    expandedDays = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    /* ---------- Helper ---------- */
    fun parseTime(time: String) {
        val parts = time.split(" ")
        val hm = parts[0].split(":")
        var hour = hm[0].toInt()
        val minute = hm[1].toInt()
        if (parts[1] == "PM" && hour != 12) hour += 12
        if (parts[1] == "AM" && hour == 12) hour = 0
        pickerHour = hour
        pickerMinute = minute
    }
}
