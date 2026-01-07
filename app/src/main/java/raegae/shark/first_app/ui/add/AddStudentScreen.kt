package raegae.shark.first_app.ui.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import raegae.shark.first_app.getApplication
import raegae.shark.first_app.viewmodels.AddStudentViewModel
import raegae.shark.first_app.viewmodels.AddStudentViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStudentScreen(
    navController: NavController,
    addStudentViewModel: AddStudentViewModel = viewModel(factory = AddStudentViewModelFactory(getApplication()))
) {
    var name by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    val weekDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val selectedDays = remember { mutableStateListOf<String>() }
    var showTimePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    val startTimeState = rememberTimePickerState()
    val endTimeState = rememberTimePickerState()
    var months by remember { mutableStateOf("1") }
    var days by remember { mutableStateOf("0") }
    var expandedMonths by remember { mutableStateOf(false) }
    var expandedDays by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val formattedStartTime = remember(startTimeState.hour, startTimeState.minute) {
        val hour = if (startTimeState.hour > 12) startTimeState.hour - 12 else if (startTimeState.hour == 0) 12 else startTimeState.hour
        val minute = String.format("%02d", startTimeState.minute)
        val amPm = if (startTimeState.hour >= 12) "PM" else "AM"
        "$hour:$minute $amPm"
    }
    val formattedEndTime = remember(endTimeState.hour, endTimeState.minute) {
        val hour = if (endTimeState.hour > 12) endTimeState.hour - 12 else if (endTimeState.hour == 0) 12 else endTimeState.hour
        val minute = String.format("%02d", endTimeState.minute)
        val amPm = if (endTimeState.hour >= 12) "PM" else "AM"
        "$hour:$minute $amPm"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Student") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(onClick = {
                        val monthsInt = months.toIntOrNull() ?: 0
                        val daysInt = days.toIntOrNull() ?: 0
                        val calendar = java.util.Calendar.getInstance()
                        calendar.add(java.util.Calendar.MONTH, monthsInt)
                        calendar.add(java.util.Calendar.DAY_OF_MONTH, daysInt)
                        val endDate = calendar.timeInMillis
                        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                            val success = addStudentViewModel.addStudentIfNotExists(
                                name = name,
                                subject = subject,
                                subscriptionEndDate = endDate,
                                batchTime = "$formattedStartTime - $formattedEndTime",
                                daysOfWeek = selectedDays
                            )
                            if (success) {
                                navController.popBackStack()
                            } else {
                                errorMessage = "A student with this name already exists."
                            }
                        }
                    }) {
                        Text("Add")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
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

        Text("Select Batch Days:", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            weekDays.forEach { day ->
                FilterChip(
                    selected = day in selectedDays,
                    onClick = {
                        if (day in selectedDays) selectedDays.remove(day)
                        else selectedDays.add(day)
                    },
                    label = { Text(day) }
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Start Time: $formattedStartTime", style = MaterialTheme.typography.titleMedium)
            Button(onClick = { showStartTimePicker = true }) {
                Text("Select")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("End Time: $formattedEndTime", style = MaterialTheme.typography.titleMedium)
            Button(onClick = { showEndTimePicker = true }) {
                Text("Select")
            }
        }


        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
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
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMonths) },
                    modifier = Modifier
                        .weight(1f)
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expandedMonths,
                    onDismissRequest = { expandedMonths = false }
                ) {
                    (1..12).forEach { month ->
                        DropdownMenuItem(
                            text = { Text(month.toString()) },
                            onClick = {
                                months = month.toString()
                                expandedMonths = false
                            }
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = expandedDays,
                onExpandedChange = { expandedDays = !expandedDays }
            ) {
                OutlinedTextField(
                    value = days,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Days") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDays) },
                    modifier = Modifier
                        .weight(1f)
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expandedDays,
                    onDismissRequest = { expandedDays = false }
                ) {
                    (0..30).forEach { day ->
                        DropdownMenuItem(
                            text = { Text(day.toString()) },
                            onClick = {
                                days = day.toString()
                                expandedDays = false
                            }
                        )
                    }
                }
            }
        }
    }

    if (showStartTimePicker) {
        TimePickerDialog(
            onCancel = { showStartTimePicker = false },
            onConfirm = {
                showStartTimePicker = false
            },
        ) {
            TimePicker(state = startTimeState)
        }
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            onCancel = { showEndTimePicker = false },
            onConfirm = {
                showEndTimePicker = false
            },
        ) {
            TimePicker(state = endTimeState)
        }
    }
}
}

// A simple TimePickerDialog to wrap the TimePicker
@Composable
fun TimePickerDialog(
    title: String = "Select Time",
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    toggle: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(text = title) },
        text = {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                toggle()
                content()
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    )
}
