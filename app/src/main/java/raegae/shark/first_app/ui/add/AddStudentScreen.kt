package raegae.shark.first_app.ui.add

import androidx.compose.ui.Modifier
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import raegae.shark.first_app.getApplication
import raegae.shark.first_app.viewmodels.AddStudentViewModel
import raegae.shark.first_app.viewmodels.AddStudentViewModelFactory
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStudentScreen(
    navController: NavController,
    addStudentViewModel: AddStudentViewModel =
        viewModel(factory = AddStudentViewModelFactory(getApplication()))
) {
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }

    val weekDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val dayEnabledStates = remember { weekDays.map { mutableStateOf(false) } }
    val dayStartTimesStates = remember { weekDays.map { mutableStateOf("9:00 AM") } }
    val dayEndTimesStates = remember { weekDays.map { mutableStateOf("10:00 AM") } }

    var months by remember { mutableStateOf("1") }
    var days by remember { mutableStateOf("0") }
    var expandedMonths by remember { mutableStateOf(false) }
    var expandedDays by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val selectedDays = weekDays.filterIndexed { index, _ ->
        dayEnabledStates[index].value
    }

    val batchTimes = selectedDays.associateWith { day ->
        val index = weekDays.indexOf(day)
        "${dayStartTimesStates[index].value} - ${dayEndTimesStates[index].value}"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Student") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    Button(onClick = {
                        val monthsInt = months.toIntOrNull() ?: 0
                        val daysInt = days.toIntOrNull() ?: 0

                        val calendar = Calendar.getInstance()
                        val startDate = calendar.timeInMillis
                        calendar.add(Calendar.MONTH, monthsInt)
                        calendar.add(Calendar.DAY_OF_MONTH, daysInt)
                        val endDate = calendar.timeInMillis

                        coroutineScope.launch {
                            val success = addStudentViewModel.addStudentIfNotExists(
                                name = name,
                                subject = subject,
                                subscriptionStartDate = startDate,
                                subscriptionEndDate = endDate,
                                batchTimes = batchTimes,
                                daysOfWeek = selectedDays
                            )

                            if (success) {
                                navController.popBackStack()
                            } else {
                                errorMessage =
                                    "A student with this name already exists."
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

            Text(
                "Select Batch Days:",
                style = MaterialTheme.typography.titleMedium
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                weekDays.forEachIndexed { index, day ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = dayEnabledStates[index].value,
                            onCheckedChange = {
                                dayEnabledStates[index].value = it
                            }
                        )

                        Text(
                            day,
                            modifier = Modifier.weight(1f)
                        )

                        if (dayEnabledStates[index].value) {
                            Row(
                                modifier = Modifier.weight(2f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = dayStartTimesStates[index].value,
                                    onValueChange = {
                                        dayStartTimesStates[index].value = it
                                    },
                                    label = { Text("Start") },
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = dayEndTimesStates[index].value,
                                    onValueChange = {
                                        dayEndTimesStates[index].value = it
                                    },
                                    label = { Text("End") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
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
                    onExpandedChange = {
                        expandedMonths = !expandedMonths
                    }
                ) {
                    OutlinedTextField(
                        value = months,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Months") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expandedMonths)
                        },
                        modifier = Modifier
                            .width(80.dp)
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )

                    ExposedDropdownMenu(
                        expanded = expandedMonths,
                        onDismissRequest = {
                            expandedMonths = false
                        }
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

                HorizontalDivider(
                    modifier = Modifier
                        .width(1.dp)
                        .height(48.dp)
                )

                ExposedDropdownMenuBox(
                    expanded = expandedDays,
                    onExpandedChange = {
                        expandedDays = !expandedDays
                    }
                ) {
                    OutlinedTextField(
                        value = days,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Days") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expandedDays)
                        },
                        modifier = Modifier
                            .width(80.dp)
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )

                    ExposedDropdownMenu(
                        expanded = expandedDays,
                        onDismissRequest = {
                            expandedDays = false
                        }
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

            if (errorMessage.isNotEmpty()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
