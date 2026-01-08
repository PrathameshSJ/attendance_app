package raegae.shark.first_app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import raegae.shark.first_app.data.AppDatabase
import raegae.shark.first_app.data.Attendance

class HomeViewModel(private val database: AppDatabase) : ViewModel() {

    val students = database.studentDao().getAllStudents()
    val allAttendance = database.attendanceDao().getAllAttendance()

    fun markAttendance(
        studentId: Int,
        date: Long,
        isPresent: Boolean
    ) {
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
            database.attendanceDao()
                .deleteAttendance(studentId, date)
        }
    }
}

class HomeViewModelFactory(
    private val application: android.app.Application
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            val database = AppDatabase.getDatabase(application)
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
