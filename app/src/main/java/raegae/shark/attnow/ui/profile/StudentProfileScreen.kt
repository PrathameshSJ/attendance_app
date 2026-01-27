package raegae.shark.attnow.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import kotlinx.coroutines.launch
import raegae.shark.attnow.data.Attendance
import raegae.shark.attnow.data.Student
import raegae.shark.attnow.data.util.StudentKey
import raegae.shark.attnow.getApplication
import raegae.shark.attnow.viewmodels.StudentProfileViewModel
import raegae.shark.attnow.viewmodels.StudentProfileViewModelFactory
import java.text.SimpleDateFormat
import raegae.shark.attnow.data.model.LogicalStudent
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Composable
fun StudentProfileScreen(
    navController: NavController,
    studentKey: StudentKey
) {
    val context = getApplication()

    val viewModel: StudentProfileViewModel = viewModel(
        factory = StudentProfileViewModelFactory(
            application = context,
            studentKey = studentKey
        )
    )

    StudentProfileContent(
        navController = navController,
        viewModel = viewModel
    )
}


@OptIn(ExperimentalMaterial3Api::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Composable
private fun StudentProfileContent(
    navController: NavController,
    viewModel: StudentProfileViewModel
    )
     {
    val student by viewModel.student.collectAsState(initial = null)
    val deleted by viewModel.deleted.collectAsState()

    val attendance by viewModel.attendance.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()

    val scope = rememberCoroutineScope()

    var currentMonth by remember { mutableStateOf(Calendar.getInstance()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRenewDialog by remember { mutableStateOf(false) }
/* 
    val weekDays = listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun")

    // -------- Renew State --------
    
    var renewMonths by remember { mutableStateOf("1") }
    var renewDays by remember { mutableStateOf("0") }
    var expandedMonths by remember { mutableStateOf(false) }
    var expandedDays by remember { mutableStateOf(false) }

    val renewEnabled = remember(student) {
        weekDays.map { mutableStateOf(student.daysOfWeek.contains(it)) }
    }
    val renewStart = remember(student) {
        weekDays.map {
            mutableStateOf(student.batchTimes[it]?.split(" - ")?.getOrNull(0) ?: "9:00 AM")
        }
    }
    val renewEnd = remember(student) {
        weekDays.map {
            mutableStateOf(student.batchTimes[it]?.split(" - ")?.getOrNull(1) ?: "10:00 AM")
        }
    }*/

    // ---------------- UI ----------------
/* 
    if (student == null) {
        // loading state
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
*/

    LaunchedEffect(Unit) {
        viewModel.navigateBack.collect {
            navController.popBackStack()
        }
    }

    if (student == null) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }

            Text(
                text = "Student Profile",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(student!!.name, style = MaterialTheme.typography.headlineMedium)
        Text(student!!.subject, style = MaterialTheme.typography.titleMedium)

        Spacer(Modifier.height(16.dp))

        Button(onClick = { showDeleteDialog = true }) {
            Text("Delete Student")
        }

        Button(onClick = { showRenewDialog = true }) {
            Text("Renew Subscription")
        }

        Spacer(Modifier.height(24.dp))

        AttendanceCalendar(
            currentMonth = currentMonth,
            onMonthChange = { currentMonth = it },
            attendance = attendance,
            subscriptionRanges = student!!.subscriptionRanges
        )
    }

    if (deleted) {
        LaunchedEffect(Unit) {
            navController.popBackStack()
        }
        return
    }

    


    // ---------------- RENEW ----------------
    if (showRenewDialog) {

        var subject by remember { mutableStateOf(student!!.subject) }

        val weekDays = listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun")

        val renewEnabled = remember {
            weekDays.map { mutableStateOf(student!!.daysOfWeek.contains(it)) }
        }

        // Store time internally as hour+minute (NO strings)
        val renewStart = remember {
            weekDays.map {
                val t = student!!.batchTimes[it]?.split(" - ")?.firstOrNull() ?: "9:00 AM"
                parseTime(t)
            }.map { mutableStateOf(it) }
        }

        val renewEnd = remember {
            weekDays.map {
                val t = student!!.batchTimes[it]?.split(" - ")?.getOrNull(1) ?: "10:00 AM"
                parseTime(t)
            }.map { mutableStateOf(it) }
        }

        var renewMonths by remember { mutableStateOf("1") }
        var renewDays by remember { mutableStateOf("0") }
        var expandedMonths by remember { mutableStateOf(false) }
        var expandedDays by remember { mutableStateOf(false) }

        var showPicker by remember { mutableStateOf(false) }
        var editingIndex by remember { mutableStateOf(0) }
        var editingStart by remember { mutableStateOf(true) }
        var pickerHour by remember { mutableStateOf(9) }
        var pickerMinute by remember { mutableStateOf(0) }

        fun openPicker(index: Int, start: Boolean) {
            editingIndex = index
            editingStart = start
            val (h,m) = if(start) renewStart[index].value else renewEnd[index].value
            pickerHour = h
            pickerMinute = m
            showPicker = true
        }

        AlertDialog(
            onDismissRequest = { showRenewDialog = false },
            title = { Text("Renew Subscription") },
            text = {
                Column(modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)   // keeps dialog from growing too big
                .verticalScroll(rememberScrollState()),verticalArrangement = Arrangement.spacedBy(16.dp)) {

                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    weekDays.forEachIndexed { i, day ->

                        Column(
                            Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Switch(
                                    checked = renewEnabled[i].value,
                                    onCheckedChange = { renewEnabled[i].value = it }
                                )

                                Text(day)
                            }

                            if (renewEnabled[i].value) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedButton(
                                        modifier = Modifier.weight(1f),
                                        onClick = { openPicker(i, true) }
                                    ) {
                                        Text(formatTime(renewStart[i].value))
                                    }

                                    OutlinedButton(
                                        modifier = Modifier.weight(1f),
                                        onClick = { openPicker(i, false) }
                                    ) {
                                        Text(formatTime(renewEnd[i].value))
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
                                value = renewMonths,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Months") },
                                modifier = Modifier.width(90.dp).menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )

                            ExposedDropdownMenu(
                                expanded = expandedMonths,
                                onDismissRequest = { expandedMonths = false }
                            ) {
                                (1..12).forEach {
                                    DropdownMenuItem(
                                        text = { Text(it.toString()) },
                                        onClick = {
                                            renewMonths = it.toString()
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
                                value = renewDays,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Days") },
                                modifier = Modifier.width(90.dp).menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )

                            ExposedDropdownMenu(
                                expanded = expandedDays,
                                onDismissRequest = { expandedDays = false }
                            ) {
                                (0..30).forEach {
                                    DropdownMenuItem(
                                        text = { Text(it.toString()) },
                                        onClick = {
                                            renewDays = it.toString()
                                            expandedDays = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch {
                        val now = Calendar.getInstance()
                        val oldEnd = student!!.subscriptionEndDate
                        val inputSubject = subject.trim()
                        val subjectChanged = inputSubject != student!!.subject.trim()
                        
                        // If renewing significantly after expiration OR changing subject, start fresh from NOW.
                        // Otherwise start from old end (contiguous).
                        val start = if (subjectChanged || now.timeInMillis > oldEnd + 86400000L) {
                            now.timeInMillis
                        } else {
                            oldEnd
                        }

                        val cal = Calendar.getInstance().apply {
                            timeInMillis = start
                        }
                        cal.add(Calendar.MONTH, renewMonths.toInt())
                        cal.add(Calendar.DAY_OF_MONTH, renewDays.toInt())

                        val newDays = weekDays.filterIndexed { i,_ -> renewEnabled[i].value }
                        val newTimes = newDays.associateWith { day ->
                            val i = weekDays.indexOf(day)
                            "${formatTime(renewStart[i].value)} - ${formatTime(renewEnd[i].value)}"
                        }

                        viewModel.renewStudent(
                            newSubject = inputSubject,
                            newStartDate = start,
                            newEndDate = cal.timeInMillis,
                            newDaysOfWeek = newDays,
                            newBatchTimes = newTimes
                        )

                    }
                    showRenewDialog = false
                }) { Text("Renew") }
            },
            dismissButton = {
                TextButton(onClick = { showRenewDialog = false }) {
                    Text("Cancel")
                }
            }
        )

        if (showPicker) {
            val pickerState = rememberTimePickerState(
                initialHour = pickerHour,
                initialMinute = pickerMinute,
                is24Hour = false
            )

            AlertDialog(
                onDismissRequest = { showPicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        val v = pickerState.hour to pickerState.minute
                        if (editingStart) renewStart[editingIndex].value = v
                        else renewEnd[editingIndex].value = v
                        showPicker = false
                    }) { Text("OK") }
                },
                dismissButton = {
                    TextButton(onClick = { showPicker = false }) { Text("Cancel") }
                },
                text = {
                    TimePicker(state = pickerState)
                }
            )
        }
    }


    // ---------------- DELETE ----------------

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Student") },
            text = { Text("Delete ${student!!.name}? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        /*navController.popBackStack()*/
                        viewModel.deleteStudent()
                        
                    }
                    showDeleteDialog = false
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}
@Composable
fun AttendanceCalendar(
    currentMonth: Calendar,
    onMonthChange: (Calendar) -> Unit,
    attendance: List<Attendance>,
    subscriptionRanges: List<Pair<Long, Long>>
) {
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
                    
                    // Check if date is within ANY active subscription range
                    val isActive = subscriptionRanges.any { (start, end) ->
                        date.timeInMillis in start..end
                    }

                    val attendanceForDay = if (isActive) attendance.find { attendanceDate ->
                        val cal = Calendar.getInstance().apply { timeInMillis = attendanceDate.date }
                        cal.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
                                cal.get(Calendar.MONTH) == date.get(Calendar.MONTH) &&
                                cal.get(Calendar.DAY_OF_MONTH) == date.get(Calendar.DAY_OF_MONTH)
                    } else null

                    val isExpired = !isActive

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
/* 
@Preview(showBackground = true)
@Composable
fun StudentProfileScreenPreview() {
    attnowTheme {
        StudentProfileScreen(navController = rememberNavController(), studentId = 1)
    }
}
    */

private fun parseTime(text: String): Pair<Int, Int> {
    return try {
        val parts = text.trim().split(" ")
        val hm = parts[0].split(":")
        var h = hm[0].toInt()
        val m = hm.getOrNull(1)?.toInt() ?: 0
        val pm = parts.getOrNull(1)?.uppercase() == "PM"

        if (pm && h != 12) h += 12
        if (!pm && h == 12) h = 0

        h to m
    } catch (e: Exception) {
        // fallback safe time
        9 to 0
    }
}


private fun formatTime(t: Pair<Int, Int>): String {
    val (h, m) = t
    val am = h < 12
    val hr = if (h == 0) 12 else if (h > 12) h - 12 else h
    return "%d:%02d %s".format(hr, m, if (am) "AM" else "PM")
}
