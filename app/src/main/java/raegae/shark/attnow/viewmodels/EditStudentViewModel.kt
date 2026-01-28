package raegae.shark.attnow.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import raegae.shark.attnow.data.AppDatabase
import raegae.shark.attnow.data.Attendance
import raegae.shark.attnow.data.util.StudentKey

class EditStudentViewModel(private val database: AppDatabase, private val studentKey: StudentKey) :
        ViewModel() {

    private val studentDao = database.studentDao()
    private val attendanceDao = database.attendanceDao()

    // 1. Load Raw Data
    private val rawStudents =
            studentDao.getAllStudents().combine(MutableStateFlow(0)) { list, _ ->
                list
                        .filter { it.name == studentKey.name && it.subject == studentKey.subject }
                        .sortedBy { it.subscriptionStartDate }
            }

    // 2. Computed Merged State (similar to Listener logic)
    val mergedSubscriptions =
            rawStudents.combine(MutableStateFlow(0)) { students, _ ->
                students.map { it.subscriptionStartDate to it.subscriptionEndDate }
            }

    // We need attendance for ALL IDs
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val mergedAttendance: StateFlow<Map<Long, Boolean?>> =
            rawStudents
                    .combine(MutableStateFlow(0)) { students, _ ->
                        val ids = students.map { it.id }
                        val attList = attendanceDao.getAttendanceForStudents(ids).first()

                        // Map: Date -> Boolean (Present wins)
                        val map = mutableMapOf<Long, Boolean?>()

                        // Populate with existing data
                        attList.groupBy { it.date }.forEach { (date, list) ->
                            val isPresent = list.any { it.isPresent }
                            map[date] = isPresent
                        }

                        // Note: We don't have "Blue" (Null) in DB. Null in map means "No Record".
                        // The UI interprets "Subscribed + Null in Map" as Blue.
                        map
                    }
                    .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    // 3. Pending Changes
    val pendingChanges =
            MutableStateFlow<Map<Long, Int>>(
                    emptyMap()
            ) // Date -> PaintID (0=Green, 1=Red, 2=Grey, 3=Blue)

    fun addChange(date: Long, paintId: Int) {
        // Normalize date to midnight just in case
        val cal =
                Calendar.getInstance().apply {
                    timeInMillis = date
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
        val d = cal.timeInMillis

        pendingChanges.value = pendingChanges.value.toMutableMap().apply { put(d, paintId) }
    }

    fun saveChanges() {
        viewModelScope.launch { database.withTransaction { performSave() } }
    }

    private suspend fun performSave() {
        val students = rawStudents.first() // Current Entities
        val changes = pendingChanges.value
        if (changes.isEmpty()) return

        // Step A: Determine desired Subscription Status for every relevant day
        // -------------------------------------------------------------------
        // Relevant Range: From Min(Existing Start, Changed Date) to Max(Existing End, Changed Date)

        val allDates =
                students.flatMap { listOf(it.subscriptionStartDate, it.subscriptionEndDate) } +
                        changes.keys
        if (allDates.isEmpty()) return

        val minDate = allDates.minOrNull()!!
        val maxDate = allDates.maxOrNull()!!

        // Build a daily map of "Should Be Subscribed"
        // Default to existing coverage
        val subscriptionMap = mutableMapOf<Long, Boolean>()

        // Iterate day by day from min to max?
        // Or better: Iterate existing ranges and mark true. Then apply changes.

        val cal = Calendar.getInstance()
        cal.timeInMillis = minDate
        normalize(cal)
        val startLoop = cal.timeInMillis

        cal.timeInMillis = maxDate
        normalize(cal)
        val endLoop = cal.timeInMillis

        // Prep map with Existing
        var curr = startLoop
        while (curr <= endLoop) {
            val isCovered =
                    students.any {
                        curr >= it.subscriptionStartDate && curr <= it.subscriptionEndDate
                    }
            subscriptionMap[curr] = isCovered

            // Apply Pending Change (Grey = False, Others = True)
            if (changes.containsKey(curr)) {
                val paint = changes[curr]!!
                subscriptionMap[curr] = (paint != 2) // 2 is Grey
            }

            cal.timeInMillis = curr
            cal.add(Calendar.DAY_OF_MONTH, 1)
            curr = cal.timeInMillis
        }

        // Step B: Calculate New Ranges (Logic: Contiguous True blocks)
        // -----------------------------------------------------------
        val newRanges = mutableListOf<Pair<Long, Long>>()
        var rangeStart: Long? = null

        curr = startLoop
        while (curr <= endLoop + 86400000L) { // Go one past to close range
            val shouldSub = subscriptionMap[curr] ?: false

            if (shouldSub) {
                if (rangeStart == null) rangeStart = curr
            } else {
                if (rangeStart != null) {
                    // End of range (previous day)
                    val rangeEnd =
                            curr - 86400000L // crudely roughly previous day, but iterating by 24h
                    // is cleaner if we stick to Calendar add
                    // Actually better to track 'prev'

                    // Let's refine the loop
                    // Range found: rangeStart to (curr - 1 day)
                    // Because this loop iterates by day using Calendar math implicitly,
                    // let's trust the dates are normalized midnights.

                    // To be safe, calculate prev day
                    val c =
                            Calendar.getInstance().apply {
                                timeInMillis = curr
                                add(Calendar.DAY_OF_MONTH, -1)
                            }
                    val rangeEndSafe = c.timeInMillis

                    newRanges.add(rangeStart to rangeEndSafe)
                    rangeStart = null
                }
            }

            val c =
                    Calendar.getInstance().apply {
                        timeInMillis = curr
                        add(Calendar.DAY_OF_MONTH, 1)
                    }
            curr = c.timeInMillis
        }

        // Step C: Reconcile Entities
        // --------------------------
        // Strategy:
        // 1. Unlink attendance from ALL current entities (set studentId to temp? No, cant,
        // constrained)
        //    Actually, we can insert new entities, move attendance, then delete old entities.

        val template =
                students.maxByOrNull { it.id }
                        ?: return // Should have at least one or we can't create

        // Map: OldID -> NewID (for remapping attendance)
        // Date -> NewID (to know where to point attendance)
        val dateToNewId = mutableMapOf<Long, Int>()

        // Create New Entities for New Ranges
        for ((start, end) in newRanges) {
            // Check if an existing entity matches exactly or fits nicely?
            // To simplify: Create NEW entities for everything. Delete ALL old.
            // This is robust against splitting/fragmentation.

            val newStudent =
                    template.copy(
                            id = 0, // AutoGen
                            subscriptionStartDate = start,
                            subscriptionEndDate = end
                    )
            val newId = studentDao.insert(newStudent).toInt()

            // Record which ID covers which date
            val c = Calendar.getInstance().apply { timeInMillis = start }
            while (c.timeInMillis <= end) {
                dateToNewId[c.timeInMillis] = newId
                c.add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        // Step D: Move Attendance
        // -----------------------
        // Get all attendance for old IDs
        val oldIds = students.map { it.id }
        val attList = attendanceDao.getAttendanceForStudents(oldIds).first()

        // For each attendance record
        for (att in attList) {
            val targetId = dateToNewId[att.date]
            if (targetId != null) {
                // Insert clone pointing to new ID
                // Upsert will handle if we accidentally process duplicates
                // But wait, primary key is (studentId, date).
                attendanceDao.upsert(att.copy(studentId = targetId))
            }
            // If targetId is null (was Greyed out), we don't copy it. Effectively deleted.
        }

        // Apply NEW Paint Changes to Attendance
        // -------------------------------------
        for ((date, paint) in changes) {
            val targetId =
                    dateToNewId[date]
                            ?: continue // Should exist if paint != Grey. If Grey, we shouldn't be
            // here (removed from newRanges)

            if (paint == 3) {
                // Blue = Delete Attendance (Not Marked)
                // We must delete record if it exists for this (NewID, Date)
                // (which we might have just copied!)
                attendanceDao.deleteAttendance(targetId, date)
            } else if (paint == 0 || paint == 1) {
                // Green/Red = Present/Absent
                val isPresent = (paint == 0)
                attendanceDao.upsert(Attendance(targetId, date, isPresent))
                // Note: Insert replaces OnConflict.
            }
        }

        // Step E: Cleanup Old Entities
        // ----------------------------
        // Delete old students. (Cascade delete would kill new attendance copies if they were
        // linked?
        // No, we copied attendance to NEW IDs. Old attendance attached to Old IDs will die.
        // Correct.)
        for (old in students) {
            studentDao.delete(old)
        }

        // Clear Changes
        pendingChanges.value = emptyMap()
    }

    private fun normalize(cal: Calendar) {
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
    }
}

class EditStudentViewModelFactory(
        private val context: Context,
        private val studentKey: StudentKey
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditStudentViewModel::class.java)) {
            val db = AppDatabase.getDatabase(context)
            @Suppress("UNCHECKED_CAST") return EditStudentViewModel(db, studentKey) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
