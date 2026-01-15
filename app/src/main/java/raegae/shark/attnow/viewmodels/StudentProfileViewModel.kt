package raegae.shark.attnow.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import raegae.shark.attnow.data.AppDatabase
import raegae.shark.attnow.data.Student
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch


class StudentProfileViewModel(private val database: AppDatabase, private val studentId: Int) : ViewModel() {

    private val _navigateBack = MutableSharedFlow<Unit>()
    val navigateBack = _navigateBack.asSharedFlow()


    private val _deleted = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = _deleted

    val student: StateFlow<Student?> =
    database.studentDao()
        .getStudentById(studentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    //val student = database.studentDao().getStudentById(studentId)
    val attendance = database.attendanceDao().getAttendanceForStudent(studentId)

    suspend fun deleteStudent() {
        viewModelScope.launch {studentId?.let{
            database.studentDao().deleteStudentById(studentId)
            _deleted.value = true}
            _navigateBack.emit(Unit)
        }
    }

    suspend fun updateStudent(student: Student) {
        database.studentDao().update(student)
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