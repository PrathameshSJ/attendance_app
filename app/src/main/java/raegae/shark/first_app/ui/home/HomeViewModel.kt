package raegae.shark.first_app.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import raegae.shark.first_app.data.AppDatabase
import raegae.shark.first_app.data.Attendance
import raegae.shark.first_app.data.Student
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val studentDao = db.studentDao()
    private val attendanceDao = db.attendanceDao()

    private val _pinnedStudents = MutableStateFlow<Set<Int>>(emptySet())
    val pinnedStudents = _pinnedStudents.asStateFlow()

    

    fun pinStudent(studentId: Int) {
        _pinnedStudents.value = _pinnedStudents.value + studentId
    }

    fun unpinStudent(studentId: Int) {
        _pinnedStudents.value = _pinnedStudents.value - studentId
    }


    val students: Flow<List<Student>> = studentDao.getAllStudents()

    fun markAttendance(studentId: Int, isPresent: Boolean) {
        viewModelScope.launch {
            val today = Calendar.getInstance().timeInMillis
            attendanceDao.upsert(Attendance(studentId = studentId, date = today, isPresent = isPresent))
        }
    }
    fun unmarkAttendance(studentId: Int, date: Long) {
        viewModelScope.launch {
            db.attendanceDao().deleteAttendance(studentId, date)
        }
    }
}
