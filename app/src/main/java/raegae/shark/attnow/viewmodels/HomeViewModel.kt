package raegae.shark.attnow.viewmodels

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import raegae.shark.attnow.data.AppDatabase
import raegae.shark.attnow.data.Attendance
import raegae.shark.attnow.data.SettingsDataStore
import raegae.shark.attnow.data.Student
import raegae.shark.attnow.ui.home.*
import java.util.Calendar
import java.util.Locale


private val todayMillis = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

class HomeViewModel(
    private val app: Application,
    private val database: AppDatabase
) : ViewModel() {

    private val settings = SettingsDataStore(app)
    private val searchQuery = MutableStateFlow("")

    fun updateSearch(q: String) {
        searchQuery.value = q
    }

    val allStudents: StateFlow<List<Student>> =
        database.studentDao().getAllStudents()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val pinnedIds: StateFlow<Set<Int>> =
        settings.pinnedStudentsForToday()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val students: StateFlow<List<Student>> =
        combine(allStudents, pinnedIds, searchQuery) { students, pinned, query ->

            val today = Calendar.getInstance()
                .getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault())
                ?: "Mon"

            students
                .filter { s ->
                    pinned.contains(s.id) ||
                    s.daysOfWeek.any { it.equals(today, true) }
                }
                .filter { s ->
                    query.isBlank() ||
                    s.name.contains(query, true) ||
                    s.subject.contains(query, true)
                }
                .sortedWith(
                    compareByDescending<Student> { pinned.contains(it.id) }
                        .thenBy { it.name }
                )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun pinStudent(id: Int) = viewModelScope.launch {
        settings.addPinned(id)
    }

    fun unpinStudent(id: Int) = viewModelScope.launch {
        settings.removePinned(id)
    }

    val allAttendance = database.attendanceDao().getAllAttendance()

    fun markAttendance(studentId: Int, date: Long, isPresent: Boolean) =
        viewModelScope.launch {
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



/* 
class HomeViewModel(private val app: Application,private val database: AppDatabase) : ViewModel() {

    val allAttendance = database.attendanceDao().getAllAttendance()
    val allStudents = database.studentDao().getAllStudents()



    private val settings = SettingsDataStore(app)

    private val searchQuery = MutableStateFlow("")

    fun updateSearch(q: String) {
        searchQuery.value = q
    }

    val students = combine(
    database.studentDao().getAllStudents(),
    settings.pinnedStudentsForToday(),
    searchQuery
) { students, pinnedIds, query ->

    val today = Calendar.getInstance().getDisplayName(
        Calendar.DAY_OF_WEEK,
        Calendar.SHORT,
        Locale.getDefault()
    ) ?: "Mon"

    students
        .map { s ->
            StudentWithPin(
                student = s,
                isPinned = pinnedIds.contains(s.id)
            )
        }
        .filter { row ->
            row.isPinned || row.student.daysOfWeek.any { d -> d.equals(today, true) }
        }
        .filter { row ->
            row.student.name.contains(query, true) ||
            row.student.subject.contains(query, true)
        }
        .sortedWith(
            compareByDescending<StudentWithPin> { it.isPinned }
                .thenBy { it.student.name }
        )
}


        
        if (query.isBlank()) {
        students
            .map { student ->
                student.isPinned = pinnedIds.contains(student.id)
                student
            }
            .filter {
                it.isPinned || it.daysOfWeek.any { d -> d.equals(today, true) }
            }
            .filter {
                it.name.contains(query, true) ||
                it.subject.contains(query, true)
            }
            .sortedWith(
                compareByDescending<Student> { it.isPinned }
                    .thenBy { it.name }
            )
        } else {
            students.filter {
                it.name.contains(query, true) ||
                it.subject.contains(query, true)
            }
        }

    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    val allAttendance = database.attendanceDao().getAllAttendance()

    // ---------- Pinned (manual) students for Home ---------- 
    
    private val settings = SettingsDataStore(app)

    val studentsWithPin = combine(
        database.studentDao().getAllStudents(),
        settings.pinnedStudents
    ) { students, pinnedIds ->
        students.map { student ->
            student.isPinned = pinnedIds.contains(student.id)
            student
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    fun pinStudent(studentId: Int) {
        viewModelScope.launch {
            settings.addPinned(studentId)
        }
    }

    fun unpinStudent(studentId: Int) {
        viewModelScope.launch {
            settings.removePinned(studentId)
        }
    }


    fun markAttendance(studentId: Int, date: Long, isPresent: Boolean) {
        viewModelScope.launch {
            database.attendanceDao().upsert(
                Attendance(
                    studentId = studentId,
                    date = date,
                    isPresent = isPresent
                )
            )
        }
    }

    fun unmarkAttendance(studentId: Int, date: Long) {
        viewModelScope.launch {
            database.attendanceDao().deleteAttendance(studentId, date)
        }
    }
}
*/



/* ---------- Factory ---------- */
class HomeViewModelFactory(private val application: Application) :
    ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            val db = AppDatabase.getDatabase(application)
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(application,db) as T
        }
        throw IllegalArgumentException("Unknown ViewModel")
    }
}
