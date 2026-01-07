package raegae.shark.first_app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import raegae.shark.first_app.data.AppDatabase

class StudentProfileViewModel(private val database: AppDatabase, private val studentId: Int) : ViewModel() {

    val student = database.studentDao().getStudentById(studentId)
    val attendance = database.attendanceDao().getAttendanceForStudent(studentId)

    suspend fun deleteStudent() {
        database.studentDao().deleteById(studentId)
    }
}

class StudentProfileViewModelFactory(private val application: android.app.Application, private val studentId: Int) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StudentProfileViewModel::class.java)) {
            val database = AppDatabase.getDatabase(application)
            @Suppress("UNCHECKED_CAST")
            return StudentProfileViewModel(database, studentId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}