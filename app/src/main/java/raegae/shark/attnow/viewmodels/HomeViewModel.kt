package raegae.shark.attnow.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import raegae.shark.attnow.data.AppDatabase
import raegae.shark.attnow.data.Attendance
import raegae.shark.attnow.data.SettingsDataStore
import raegae.shark.attnow.data.logic.LogicalStudentMerger
import raegae.shark.attnow.data.model.LogicalStudent
import raegae.shark.attnow.data.util.StudentKey
import java.util.Calendar
import java.util.Locale

class HomeViewModel(
    private val app: Application,
    private val database: AppDatabase
) : ViewModel() {

    private val settings = SettingsDataStore(app)

    private val searchQuery = MutableStateFlow("")

    fun updateSearch(query: String) {
        searchQuery.value = query
    }

    /* ---------- Students ---------- */

    private val rawStudents: StateFlow<List<LogicalStudent>> =
        database.studentDao()
            .getAllStudents()
            .map { LogicalStudentMerger.merge(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val pinnedKeys: StateFlow<Set<StudentKey>> =
        settings.pinnedStudents
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val students: StateFlow<List<LogicalStudent>> =
        combine(rawStudents, pinnedKeys, searchQuery) { students, pinned, query ->

            val today =
                Calendar.getInstance()
                    .getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault())
                    ?: "Mon"

            students
                .filter { student ->
                    pinned.contains(student.key) ||
                            student.daysOfWeek.any { it.equals(today, true) }
                }
                .filter { student ->
                    query.isBlank() ||
                            student.name.contains(query, true) ||
                            student.subject.contains(query, true)
                }
                .sortedWith(
                    compareByDescending<LogicalStudent> { pinned.contains(it.key) }
                        .thenBy { it.name }
                )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val availableForPinning: StateFlow<List<LogicalStudent>> =
        combine(rawStudents, pinnedKeys, searchQuery) { students, pinned, query ->

            val today =
                Calendar.getInstance()
                    .getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault())
                    ?: "Mon"

            students
                .filter { student ->
                    // Exclude if Pinned OR Batch is Today (i.e. exclude if already on home)
                    val isOnHome = pinned.contains(student.key) ||
                            student.daysOfWeek.any { it.equals(today, true) }
                    !isOnHome
                }
                .filter { student ->
                    query.isBlank() ||
                            student.name.contains(query, true) ||
                            student.subject.contains(query, true)
                }
                .sortedBy { it.name }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /* ---------- Pinning ---------- */

    fun pinStudent(student: LogicalStudent) =
        viewModelScope.launch {
            settings.addPinned(student.key)
        }

    fun pinStudentByKey(key: StudentKey) =
        viewModelScope.launch {
            settings.addPinned(key)
        }

    fun unpinStudent(student: LogicalStudent) =
        viewModelScope.launch {
            settings.removePinned(student.key)
        }

    /* ---------- Attendance ---------- */

    val allAttendance =
        database.attendanceDao().getAllAttendance()

    fun markAttendance(
        studentId: Int,
        date: Long,
        isPresent: Boolean
    ) = viewModelScope.launch {

        // enforce single record per day
        database.attendanceDao().deleteAttendance(studentId, date)

        database.attendanceDao().upsert(
            Attendance(
                studentId = studentId,
                date = date,
                isPresent = isPresent
            )
        )
    }

    fun unmarkAttendance(studentId: Int, date: Long) =
        viewModelScope.launch {
            database.attendanceDao().deleteAttendance(studentId, date)
        }
}

/* ---------- Factory ---------- */

class HomeViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            val db = AppDatabase.getDatabase(application)
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(application, db) as T
        }
        throw IllegalArgumentException("Unknown ViewModel")
    }
}
