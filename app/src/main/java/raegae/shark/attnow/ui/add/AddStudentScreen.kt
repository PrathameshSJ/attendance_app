package raegae.shark.attnow.ui.add

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
import java.util.Calendar
import kotlinx.coroutines.launch
import raegae.shark.attnow.getApplication
import raegae.shark.attnow.viewmodels.AddStudentViewModel
import raegae.shark.attnow.viewmodels.AddStudentViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStudentScreen(
        navController: NavController,
        addStudentViewModel: AddStudentViewModel =
                viewModel(factory = AddStudentViewModelFactory(getApplication()))
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }

    var nameError by remember { mutableStateOf(false) }
    var subjectError by remember { mutableStateOf(false) }

    val weekDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val enabled = remember { weekDays.map { mutableStateOf(false) } }
    val startTimes = remember { weekDays.map { mutableStateOf(9 to 0) } }
    val endTimes = remember { weekDays.map { mutableStateOf(10 to 0) } }

    var months by remember { mutableStateOf("1") }
    var days by remember { mutableStateOf("0") }
    var expandedMonths by remember { mutableStateOf(false) }
    var expandedDays by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    var showPicker by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf(0) }
    var editingStart by remember { mutableStateOf(true) }
    var pickerHour by remember { mutableStateOf(9) }
    var pickerMinute by remember { mutableStateOf(0) }

    fun openPicker(index: Int, start: Boolean) {
        editingIndex = index
        editingStart = start
        val (h, m) = if (start) startTimes[index].value else endTimes[index].value
        pickerHour = h
        pickerMinute = m
        showPicker = true
    }

    fun format(h: Int, m: Int): String {
        val am = h < 12
        val hr = if (h == 0) 12 else if (h > 12) h - 12 else h
        return "%d:%02d %s".format(hr, m, if (am) "AM" else "PM")
    }

    val selectedDays = weekDays.filterIndexed { i, _ -> enabled[i].value }
    val batchTimes =
            selectedDays.associateWith {
                val i = weekDays.indexOf(it)
                "${format(startTimes[i].value.first,startTimes[i].value.second)} - " +
                        format(endTimes[i].value.first, endTimes[i].value.second)
            }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Add Student") },
                        navigationIcon = {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                            }
                        },
                        actions = {
                            Button(
                                    onClick = {
                                        nameError = name.isBlank()
                                        subjectError = subject.isBlank()

                                        if (nameError || subjectError) return@Button

                                        if (selectedDays.isEmpty()) {
                                            android.widget.Toast.makeText(
                                                            context,
                                                            "Error: No Batch Selected!",
                                                            android.widget.Toast.LENGTH_LONG
                                                    )
                                                    .show()
                                            return@Button
                                        }

                                        val cal = Calendar.getInstance()
                                        // Normalize to Midnight (Start of Day) to prevent
                                        // timezone/offset issues
                                        cal.set(Calendar.HOUR_OF_DAY, 0)
                                        cal.set(Calendar.MINUTE, 0)
                                        cal.set(Calendar.SECOND, 0)
                                        cal.set(Calendar.MILLISECOND, 0)

                                        val start = cal.timeInMillis
                                        cal.add(Calendar.MONTH, months.toInt())
                                        cal.add(Calendar.DAY_OF_MONTH, days.toInt())

                                        scope.launch {
                                            val ok =
                                                    addStudentViewModel.addStudentIfNotExists(
                                                            name,
                                                            subject,
                                                            start,
                                                            cal.timeInMillis,
                                                            batchTimes,
                                                            selectedDays
                                                    )
                                            if (ok) navController.popBackStack()
                                            else error = "Student already exists"
                                        }
                                    }
                            ) { Text("Add") }
                        }
                )
            }
    ) { pad ->
        Column(
                Modifier.fillMaxSize()
                        .padding(pad)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (nameError) nameError = false
                    },
                    label = { Text("Student Name") },
                    isError = nameError,
                    modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                    value = subject,
                    onValueChange = {
                        subject = it
                        if (subjectError) subjectError = false
                    },
                    label = { Text("Subject") },
                    isError = subjectError,
                    modifier = Modifier.fillMaxWidth()
            )

            Text("Batch Days", style = MaterialTheme.typography.titleMedium)

            weekDays.forEachIndexed { i, day ->
                Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Switch(checked = enabled[i].value, onCheckedChange = { enabled[i].value = it })

                    Text(day, Modifier.width(48.dp))

                    if (enabled[i].value) {
                        OutlinedButton(onClick = { openPicker(i, true) }) {
                            Text(format(startTimes[i].value.first, startTimes[i].value.second))
                        }
                        Text("to")
                        OutlinedButton(onClick = { openPicker(i, false) }) {
                            Text(format(endTimes[i].value.first, endTimes[i].value.second))
                        }
                    }
                }
            }

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
                            modifier =
                                    Modifier.width(90.dp)
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
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
                            modifier =
                                    Modifier.width(90.dp)
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
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

            if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
        }
    }

    if (showPicker) {
        val pickerState =
                rememberTimePickerState(
                        initialHour = pickerHour,
                        initialMinute = pickerMinute,
                        is24Hour = false
                )

        AlertDialog(
                onDismissRequest = { showPicker = false },
                confirmButton = {
                    TextButton(
                            onClick = {
                                val v = pickerState.hour to pickerState.minute
                                if (editingStart) startTimes[editingIndex].value = v
                                else endTimes[editingIndex].value = v
                                showPicker = false
                            }
                    ) { Text("OK") }
                },
                dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Cancel") } },
                text = { TimePicker(state = pickerState) }
        )
    }
}
