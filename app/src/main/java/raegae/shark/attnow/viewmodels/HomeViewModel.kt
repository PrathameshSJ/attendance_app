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
import java.util.Calendar
import java.util.Locale

private val todayMillis = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

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
        settings.pinnedStudents,
        searchQuery
    ) { students, pinnedIds, query ->

        val today = Calendar.getInstance().getDisplayName(
            Calendar.DAY_OF_WEEK,
            Calendar.SHORT,
            Locale.getDefault()
        ) ?: "Mon"

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

    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

/* 
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
*/

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
