package raegae.shark.attnow.ui.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.*
import raegae.shark.attnow.data.util.StudentKey
import raegae.shark.attnow.getApplication
import raegae.shark.attnow.viewmodels.EditStudentViewModel
import raegae.shark.attnow.viewmodels.EditStudentViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditStudentScreen(navController: NavController, studentKey: StudentKey) {
    val context = getApplication()
    val viewModel: EditStudentViewModel =
            viewModel(factory = EditStudentViewModelFactory(context, studentKey))

    val changes by viewModel.pendingChanges.collectAsState()
    val hasChanges = changes.isNotEmpty()

    val mergedAttendance by
            viewModel.mergedAttendance.collectAsState(initial = emptyMap()) // Map<Long, Boolean?>
    val mergedSubscriptions by
            viewModel.mergedSubscriptions.collectAsState(
                    initial = emptyList()
            ) // List<Pair<Long, Long>>

    // Paint State: 0=Green, 1=Red, 2=Grey, 3=Blue
    // We map: Green->Present, Red->Absent, Grey->No Sub, Blue->Sub Only (Null attendance)
    var selectedPaint by remember {
        mutableStateOf<Int?>(null)
    } // null = no paint selected? User said "clicking paint buttons shows white outline"

    var currentMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showBackDialog by remember { mutableStateOf(false) }

    fun handleBack() {
        if (hasChanges) showBackDialog = true else navController.popBackStack()
    }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = {
                            Column {
                                Text("Edit Student")
                                Text(studentKey.name, style = MaterialTheme.typography.labelMedium)
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { handleBack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                            }
                        },
                        actions = {
                            if (hasChanges) {
                                Button(onClick = { showSaveDialog = true }) { Text("Save") }
                            }
                        }
                )
            }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {

            // PAINT CONTROLS
            Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
            ) {
                PaintButton(Color(0xFF4CAF50), "Present", selectedPaint == 0) { selectedPaint = 0 }
                PaintButton(Color(0xFFF44336), "Absent", selectedPaint == 1) { selectedPaint = 1 }
                PaintButton(Color(0xFFB0B0B0), "No Sub", selectedPaint == 2) { selectedPaint = 2 }
                PaintButton(Color(0xFF2196F3), "Sub", selectedPaint == 3) { selectedPaint = 3 }
                PaintButton(Color.Black, "None", selectedPaint == null) { selectedPaint = null }
            }

            Divider()

            // CALENDAR
            Spacer(Modifier.height(16.dp))
            EditAttendanceCalendar(
                    currentMonth = currentMonth,
                    onMonthChange = { currentMonth = it },
                    mergedAttendance = mergedAttendance,
                    mergedSubscriptions = mergedSubscriptions,
                    pendingChanges = changes,
                    onDayClick = { dateInMillis ->
                        selectedPaint?.let { paintId -> viewModel.addChange(dateInMillis, paintId) }
                    }
            )

            Spacer(Modifier.height(16.dp))
            Button(
                    onClick = {
                        navController.navigate(
                                "edit_entities/${studentKey.name}/${studentKey.subject}"
                        )
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            ) { Text("Edit Entities") }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                title = { Text("Save Changes?") },
                text = {
                    Text(
                            "This will modify subscription periods and attendance records. Cannot be undone."
                    )
                },
                confirmButton = {
                    TextButton(
                            onClick = {
                                viewModel.saveChanges()
                                showSaveDialog = false
                                navController.popBackStack()
                            }
                    ) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
                }
        )
    }

    if (showBackDialog) {
        AlertDialog(
                onDismissRequest = { showBackDialog = false },
                title = { Text("Discard Changes?") },
                text = { Text("You have unsaved changes. Are you sure you want to go back?") },
                confirmButton = {
                    TextButton(
                            onClick = {
                                showBackDialog = false
                                navController.popBackStack()
                            }
                    ) { Text("Discard") }
                },
                dismissButton = {
                    TextButton(onClick = { showBackDialog = false }) { Text("Cancel") }
                }
        )
    }
}

@Composable
fun PaintButton(color: Color, text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
            Modifier.size(60.dp)
                    .clip(CircleShape)
                    .background(color)
                    .clickable { onClick() }
                    .then(
                            if (selected)
                                    Modifier.border(4.dp, Color.White, CircleShape)
                                            .border(5.dp, Color.Black, CircleShape)
                            else Modifier
                    ),
            contentAlignment = Alignment.Center
    ) {
        Text(
                text,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun EditAttendanceCalendar(
        currentMonth: Calendar,
        onMonthChange: (Calendar) -> Unit,
        mergedAttendance: Map<Long, Boolean?>, // Date -> Boolean (Pres/Abs), Null (Not Marked)
        mergedSubscriptions: List<Pair<Long, Long>>, // Active Ranges
        pendingChanges: Map<Long, Int>, // Date -> PaintID
        onDayClick: (Long) -> Unit
) {
    val monthFormatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
        // Month Nav
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                    onClick = {
                        val newMonth = currentMonth.clone() as Calendar
                        newMonth.add(Calendar.MONTH, -1)
                        onMonthChange(newMonth)
                    }
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month") }
            Text(
                    text = monthFormatter.format(currentMonth.time),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
            )
            IconButton(
                    onClick = {
                        val newMonth = currentMonth.clone() as Calendar
                        newMonth.add(Calendar.MONTH, 1)
                        onMonthChange(newMonth)
                    }
            ) { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Grid
        val daysInMonth = currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfMonth = currentMonth.clone() as Calendar
        firstDayOfMonth.set(Calendar.DAY_OF_MONTH, 1)

        // Reset time to midnight for consistent comparisons
        firstDayOfMonth.set(Calendar.HOUR_OF_DAY, 0)
        firstDayOfMonth.set(Calendar.MINUTE, 0)
        firstDayOfMonth.set(Calendar.SECOND, 0)
        firstDayOfMonth.set(Calendar.MILLISECOND, 0)

        val startDayOfWeek = firstDayOfMonth.get(Calendar.DAY_OF_WEEK) // Sun=1

        val calendarDays = (1..daysInMonth).map { it }.toMutableList()
        val emptySlots = startDayOfWeek - 1

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

            items(emptySlots) { Box(Modifier.size(40.dp)) }

            items(calendarDays) { day ->
                val dateCal = firstDayOfMonth.clone() as Calendar
                dateCal.set(Calendar.DAY_OF_MONTH, day)
                val dateInMillis = dateCal.timeInMillis

                // Determine State
                // Priority: Pending Change > Existing Data

                var color = Color.White

                if (pendingChanges.containsKey(dateInMillis)) {
                    // 0=Green, 1=Red, 2=Grey, 3=Blue
                    color =
                            when (pendingChanges[dateInMillis]) {
                                0 -> Color(0xFF4CAF50)
                                1 -> Color(0xFFF44336)
                                2 -> Color(0xFFB0B0B0)
                                3 -> Color(0xFF2196F3)
                                else -> Color.White
                            }
                } else {
                    // Existing Data
                    val isSubscribed =
                            mergedSubscriptions.any { (start, end) ->
                                dateInMillis in start..end
                            } // Need strict midnight comparison but range usually covers full days.
                    // Wait, subscription ranges are ms timestamps.
                    // We need to check if the FULL day is covered? Or just overlap?
                    // Usually start is midnight, end is midnight + 1 day? Or arbitrary.
                    // Let's assume ranges are start..end inclusive.

                    if (!isSubscribed) {
                        color = Color(0xFFB0B0B0) // Grey (Expired/No Sub)
                    } else {
                        // Check exact date match for attendance
                        // mergedAttendance key should be normalized to midnight
                        val att = mergedAttendance[dateInMillis]
                        color =
                                when (att) {
                                    true -> Color(0xFF4CAF50)
                                    false -> Color(0xFFF44336)
                                    null -> Color(0xFF2196F3) // Subscribed but not marked
                                }
                    }
                }

                Box(
                        modifier =
                                Modifier.size(40.dp)
                                        .clip(MaterialTheme.shapes.small)
                                        .background(color)
                                        .clickable { onDayClick(dateInMillis) },
                        contentAlignment = Alignment.Center
                ) { Text(text = day.toString(), color = Color.White) }
            }
        }
    }
}
