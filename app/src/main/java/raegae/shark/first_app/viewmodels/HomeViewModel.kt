package raegae.shark.first_app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import raegae.shark.first_app.data.AppDatabase
import raegae.shark.first_app.data.Attendance

class HomeViewModel(private val database: AppDatabase) : ViewModel() {

    val students = database.studentDao().getAllStudents()
    val allAttendance = database.attendanceDao().getAllAttendance()

    /* ---------- Pinned (manual) students for Home ---------- */
    private val _pinnedStudents = MutableStateFlow<List<Int>>(emptyList())
    val pinnedStudents: StateFlow<List<Int>> = _pinnedStudents.asStateFlow()

    fun pinStudent(studentId: Int) {
        if (!_pinnedStudents.value.contains(studentId)) {
            _pinnedStudents.value = _pinnedStudents.value + studentId
        }
    }

    fun unpinStudent(studentId: Int) {
        _pinnedStudents.value = _pinnedStudents.value.filterNot { it == studentId }
    }

    fun clearPinned() {
        _pinnedStudents.value = emptyList()
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
class HomeViewModelFactory(private val application: android.app.Application) :
    ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            val db = AppDatabase.getDatabase(application)
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(db) as T
        }
        throw IllegalArgumentException("Unknown ViewModel")
    }
}
