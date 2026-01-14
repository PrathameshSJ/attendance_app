package raegae.shark.attnow.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import raegae.shark.attnow.data.AppDatabase
import raegae.shark.attnow.data.Attendance
import raegae.shark.attnow.data.Student

class StudentProfileViewModel(application: Application, studentId: Int) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val studentDao = db.studentDao()
    private val attendanceDao = db.attendanceDao()

    val student: Flow<Student> = studentDao.getStudentById(studentId).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Student(id = 0, name = "", subject = "", subscriptionStartDate = 0, subscriptionEndDate = 0, batchTimes = emptyMap(), daysOfWeek = emptyList())
    )

    val attendance: Flow<List<Attendance>> = attendanceDao.getAttendanceForStudent(studentId).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    suspend fun updateStudent(student: Student) {
        studentDao.update(student)
    }
}
