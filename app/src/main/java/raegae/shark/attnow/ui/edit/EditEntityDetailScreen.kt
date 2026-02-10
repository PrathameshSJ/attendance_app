package raegae.shark.attnow.ui.edit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import raegae.shark.attnow.data.AppDatabase
import raegae.shark.attnow.data.Student

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditEntityDetailScreen(navController: NavController, studentId: Int) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }

    // Load Student
    val studentState =
            produceState<Student?>(initialValue = null, key1 = studentId) {
                value =
                        withContext(Dispatchers.IO) {
                            db.studentDao().getStudentByIdOnce(studentId)
                        }
            }

    val student = studentState.value

    if (student == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // State
    var startMillis by remember(student) { mutableStateOf(student.subscriptionStartDate) }
    var endMillis by remember(student) { mutableStateOf(student.subscriptionEndDate) }
    var maxClasses by remember(student) { mutableStateOf(student.max_classes.toString()) }
    var phoneNumber by remember(student) { mutableStateOf(student.phoneNumber) }
    var expandedMaxClasses by remember { mutableStateOf(false) }

    // Batch Logic Copied/Adapted
    val weekDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    // Initialize from student batchTimes
    val enabled =
            remember(student) {
                weekDays.map { day -> mutableStateOf(student.daysOfWeek.contains(day)) }
            }

    fun parseTime(t: String): Pair<Int, Int> {
        return try {
            val parts = t.trim().split(" ") // "9:00 AM"
            val hm = parts[0].split(":")
            var h = hm[0].toInt()
            val m = hm.getOrNull(1)?.toInt() ?: 0
            val pm = parts.getOrNull(1)?.uppercase() == "PM"
            if (pm && h != 12) h += 12
            if (!pm && h == 12) h = 0
            h to m
        } catch (e: Exception) {
            9 to 0
        }
    }

    val startTimes =
            remember(student) {
                weekDays.map { day ->
                    val t = student.batchTimes[day]?.split(" - ")?.firstOrNull() ?: "9:00 AM"
                    mutableStateOf(parseTime(t))
                }
            }
    val endTimes =
            remember(student) {
                weekDays.map { day ->
                    val t = student.batchTimes[day]?.split(" - ")?.getOrNull(1) ?: "10:00 AM"
                    mutableStateOf(parseTime(t))
                }
            }

    // Pickers State
    var showDatePickerStart by remember { mutableStateOf(false) }
    var showDatePickerEnd by remember { mutableStateOf(false) }

    var showTimePicker by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf(0) }
    var editingStart by remember { mutableStateOf(true) }
    var pickerHour by remember { mutableStateOf(9) }
    var pickerMinute by remember { mutableStateOf(0) }

    val dateFormatter = remember { SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault()) }

    fun formatTime(h: Int, m: Int): String {
        val cal =
                Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, h)
                    set(Calendar.MINUTE, m)
                }
        return SimpleDateFormat("h:mm a", Locale.getDefault()).format(cal.time)
    }

    fun openTimePicker(index: Int, isStart: Boolean) {
        editingIndex = index
        editingStart = isStart
        val current = if (isStart) startTimes[index].value else endTimes[index].value
        pickerHour = current.first
        pickerMinute = current.second
        showTimePicker = true
    }

    // Handlers for Date Picker UTC conversion
    fun localToUtc(localMillis: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = localMillis
        val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utcCalendar.set(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        )
        return utcCalendar.timeInMillis
    }

    fun utcToLocal(utcMillis: Long): Long {
        val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        utcCalendar.timeInMillis = utcMillis
        val calendar = Calendar.getInstance()
        calendar.set(
                utcCalendar.get(Calendar.YEAR),
                utcCalendar.get(Calendar.MONTH),
                utcCalendar.get(Calendar.DAY_OF_MONTH),
                0,
                0,
                0 // Reset time part for consistency
        )
        // Reset millis to 0 just in case
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Edit Details") },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) { // Cancel
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                            }
                        },
                        actions = {
                            Button(
                                    onClick = {
                                        // Construct Modified Student
                                        val selectedDays =
                                                weekDays.filterIndexed { i, _ -> enabled[i].value }
                                        val newBatchTimes =
                                                selectedDays.associateWith { day ->
                                                    val i = weekDays.indexOf(day)
                                                    "${formatTime(startTimes[i].value.first, startTimes[i].value.second)} - " +
                                                            "${formatTime(endTimes[i].value.first, endTimes[i].value.second)}"
                                                }

                                        val modifiedStudent =
                                                student.copy(
                                                        subscriptionStartDate = startMillis,
                                                        subscriptionEndDate = endMillis,
                                                        daysOfWeek = selectedDays,
                                                        batchTimes = newBatchTimes,
                                                        max_classes = maxClasses.toIntOrNull() ?: 0,
                                                        phoneNumber = phoneNumber
                                                )

                                        // Pass back to List Screen
                                        navController.previousBackStackEntry?.savedStateHandle?.set(
                                                "updated_student",
                                                modifiedStudent
                                        )
                                        navController.popBackStack()
                                    }
                            ) { Text("Done") }
                        }
                )
            }
    ) { pad ->
        Column(
                Modifier.padding(pad)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Phone Number
            var phoneNumberError by remember { mutableStateOf(false) }
            OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = {
                        if (it.length <= 10 && it.all { char -> char.isDigit() }) {
                            phoneNumber = it
                        }
                    },
                    label = { Text("Phone Number (10 digits)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
            )

            // Subscription Dates
            Text("Subscription Period", style = MaterialTheme.typography.titleMedium)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                        onClick = { showDatePickerStart = true },
                        modifier = Modifier.weight(1f)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Start Date", style = MaterialTheme.typography.labelSmall)
                        Text(dateFormatter.format(Date(startMillis)))
                    }
                }

                OutlinedButton(
                        onClick = { showDatePickerEnd = true },
                        modifier = Modifier.weight(1f)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("End Date", style = MaterialTheme.typography.labelSmall)
                        Text(dateFormatter.format(Date(endMillis)))
                    }
                }
            }

            // Max Classes
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ExposedDropdownMenuBox(
                        expanded = expandedMaxClasses,
                        onExpandedChange = { expandedMaxClasses = !expandedMaxClasses }
                ) {
                    OutlinedTextField(
                            value = maxClasses,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Max Classes (0 = Unlimited)") },
                            modifier =
                                    Modifier.fillMaxWidth()
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )

                    ExposedDropdownMenu(
                            expanded = expandedMaxClasses,
                            onDismissRequest = { expandedMaxClasses = false }
                    ) {
                        (0..50).forEach {
                            DropdownMenuItem(
                                    text = { Text(it.toString()) },
                                    onClick = {
                                        maxClasses = it.toString()
                                        expandedMaxClasses = false
                                    }
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // Batch Times
            Text("Batch Days & Times", style = MaterialTheme.typography.titleMedium)

            weekDays.forEachIndexed { i, day ->
                Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Switch(checked = enabled[i].value, onCheckedChange = { enabled[i].value = it })
                    Text(day, Modifier.width(48.dp))

                    if (enabled[i].value) {
                        OutlinedButton(
                                onClick = { openTimePicker(i, true) },
                                modifier = Modifier.weight(1f)
                        ) {
                            Text(formatTime(startTimes[i].value.first, startTimes[i].value.second))
                        }
                        Text("-")
                        OutlinedButton(
                                onClick = { openTimePicker(i, false) },
                                modifier = Modifier.weight(1f)
                        ) { Text(formatTime(endTimes[i].value.first, endTimes[i].value.second)) }
                    }
                }
            }
        }
    }

    // Material3 Date Picker
    if (showDatePickerStart) {
        val datePickerState =
                rememberDatePickerState(initialSelectedDateMillis = localToUtc(startMillis))
        DatePickerDialog(
                onDismissRequest = { showDatePickerStart = false },
                confirmButton = {
                    TextButton(
                            onClick = {
                                datePickerState.selectedDateMillis?.let {
                                    startMillis = utcToLocal(it)
                                }
                                showDatePickerStart = false
                            }
                    ) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePickerStart = false }) { Text("Cancel") }
                }
        ) { DatePicker(state = datePickerState) }
    }

    if (showDatePickerEnd) {
        val datePickerState =
                rememberDatePickerState(initialSelectedDateMillis = localToUtc(endMillis))
        DatePickerDialog(
                onDismissRequest = { showDatePickerEnd = false },
                confirmButton = {
                    TextButton(
                            onClick = {
                                datePickerState.selectedDateMillis?.let {
                                    endMillis = utcToLocal(it)
                                }
                                showDatePickerEnd = false
                            }
                    ) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePickerEnd = false }) { Text("Cancel") }
                }
        ) { DatePicker(state = datePickerState) }
    }

    // Time Picker
    if (showTimePicker) {
        val pickerState =
                rememberTimePickerState(
                        initialHour = pickerHour,
                        initialMinute = pickerMinute,
                        is24Hour = false
                )
        AlertDialog(
                onDismissRequest = { showTimePicker = false },
                confirmButton = {
                    TextButton(
                            onClick = {
                                val v = pickerState.hour to pickerState.minute
                                if (editingStart) startTimes[editingIndex].value = v
                                else endTimes[editingIndex].value = v
                                showTimePicker = false
                            }
                    ) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                },
                text = { TimePicker(state = pickerState) }
        )
    }
}
