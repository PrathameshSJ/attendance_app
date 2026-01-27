package raegae.shark.attnow.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import raegae.shark.attnow.data.AppDatabase
import raegae.shark.attnow.data.Attendance
import raegae.shark.attnow.data.Student
import raegae.shark.attnow.data.logic.LogicalStudentMerger
import raegae.shark.attnow.data.model.LogicalStudent
import raegae.shark.attnow.data.util.StudentKey
import java.util.Calendar

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class StudentProfileViewModel(
    private val app: Application,
    private val database: AppDatabase,
    private val studentKey: StudentKey
) : ViewModel() {

    /* ---------- navigation ---------- */

    private val _navigateBack = MutableSharedFlow<Unit>()
    val navigateBack = _navigateBack.asSharedFlow()

    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted.asStateFlow()

    /* ---------- logical student ---------- */

    val student: StateFlow<LogicalStudent?> =
        database.studentDao()
            .getAllStudents()
            .map { students ->
                LogicalStudentMerger
                    .merge(students)
                    .firstOrNull { it.key == studentKey }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                null
            )

    /* ---------- attendance (merged timeline) ---------- */

    val attendance: StateFlow<List<Attendance>> =
        student.flatMapLatest { logical ->
            if (logical == null) {
                flowOf(emptyList())
            } else {
                database.attendanceDao()
                    .getAttendanceForStudents(logical.entityIds)
                    .map { rawList ->
                        // MERGE LOGIC:
                        // 1. Group by date
                        // 2. If ANY record for that date isPresent=true -> result is True
                        rawList
                            .groupBy { it.date }
                            .map { (date, entries) ->
                                val isPresent = entries.any { it.isPresent }
                                Attendance(
                                    studentId = logical.activeEntityId, // visual only
                                    date = date,
                                    isPresent = isPresent
                                )
                            }
                            .sortedBy { it.date }
                    }
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        )

    /* ---------- actions ---------- */

    fun deleteStudent() {
        viewModelScope.launch {

            database.studentDao().deleteLogicalStudent(
                name = studentKey.name,
                subject = studentKey.subject
            )

            _deleted.value = true
            _navigateBack.emit(Unit)
        }
    }

    /**
     * Renew logic:
     * - If same subject + same days + same times → extend
     * - Else → create new entity
     */
    fun renewStudent(
        newSubject: String,
        newStartDate: Long,
        newEndDate: Long,
        newDaysOfWeek: List<String>,
        newBatchTimes: Map<String, String>
    ) {
        viewModelScope.launch {

            val entities =
                database.studentDao()
                    .findByNameAndSubject(
                        studentKey.name,
                        studentKey.subject
                    )

            // Find entity with matching attributes (days, times, subject)
            val matchingAttributes = entities.firstOrNull { s ->
                s.subject == newSubject &&
                s.daysOfWeek == newDaysOfWeek &&
                s.batchTimes == newBatchTimes
            }

            if (matchingAttributes != null) {
                // Check for gap. If new start date is significantly after old end date, create NEW.
                // Allow small buffer (e.g. up to 24h or so) for "contiguous"
                // But strictly speaking:
                // If newStartDate > subscriptionEndDate (allowing for same-day overlap), it's a gap.
                // Let's use a 48 hour buffer to be safe against timezone/late renewals, 
                // UNLESS user specifically wants gaps.
                // User said: "changing my date to smth outside... extended... didn't create new".
                // This means my previous logic extended TOO aggressively.
                
                // If newStartDate is > oldEndDate + 24h, treat as gap.
                // (24h in millis = 86400000)
                val diff = newStartDate - matchingAttributes.subscriptionEndDate
                val isGap = diff > 86400000L 

                if (isGap) {
                    // Create NEW entity
                    database.studentDao().insert(
                        Student(
                            name = studentKey.name,
                            subject = newSubject,
                            subscriptionStartDate = newStartDate,
                            subscriptionEndDate = newEndDate,
                            daysOfWeek = newDaysOfWeek,
                            batchTimes = newBatchTimes
                        )
                    )
                } else {
                    // Extend existing
                    database.studentDao().update(
                        matchingAttributes.copy(
                            subscriptionEndDate = newEndDate
                        )
                    )
                }
            } else {
                // Attributes don't match -> NEW entity
                database.studentDao().insert(
                    Student(
                        name = studentKey.name,
                        subject = newSubject,
                        subscriptionStartDate = newStartDate,
                        subscriptionEndDate = newEndDate,
                        daysOfWeek = newDaysOfWeek,
                        batchTimes = newBatchTimes
                    )
                )
            }
        }
    }
}

/* ---------- Factory ---------- */

class StudentProfileViewModelFactory(
    private val application: Application,
    private val studentKey: StudentKey
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StudentProfileViewModel::class.java)) {

            val db = AppDatabase.getDatabase(application)

            @Suppress("UNCHECKED_CAST")
            return StudentProfileViewModel(
                app = application,
                database = db,
                studentKey = studentKey
            ) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
