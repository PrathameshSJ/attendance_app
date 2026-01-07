package raegae.shark.first_app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import raegae.shark.first_app.data.AppDatabase
import raegae.shark.first_app.data.Student

class AddStudentViewModel(private val database: AppDatabase) : ViewModel() {

    suspend fun addStudentIfNotExists(name: String, subject: String, subscriptionEndDate: Long, batchTime: String, daysOfWeek: List<String>): Boolean {
        val students = database.studentDao().getAllStudents().first()
        val existing = students.find { it.name.equals(name, ignoreCase = true) }
        if (existing != null) return false
        val student = Student(
            name = name,
            subject = subject,
            subscriptionEndDate = subscriptionEndDate,
            batchTime = batchTime,
            daysOfWeek = daysOfWeek
        )
        database.studentDao().insert(student)
        return true
    }
}

class AddStudentViewModelFactory(private val application: android.app.Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddStudentViewModel::class.java)) {
            val database = AppDatabase.getDatabase(application)
            @Suppress("UNCHECKED_CAST")
            return AddStudentViewModel(database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}