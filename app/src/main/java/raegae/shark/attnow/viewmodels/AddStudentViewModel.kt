package raegae.shark.attnow.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import raegae.shark.attnow.data.AppDatabase
import raegae.shark.attnow.data.Student
import raegae.shark.attnow.data.StudentDao

class AddStudentViewModel(private val database: AppDatabase) : ViewModel() {

    suspend fun addStudentIfNotExists(name: String, subject: String, subscriptionStartDate: Long, subscriptionEndDate: Long, batchTimes: Map<String, String>, daysOfWeek: List<String>): Boolean {
        val students = database.studentDao().getAllStudents().first()
        val existing = students.find { it.name.equals(name, ignoreCase = true) }
        if (existing != null) return false
        val student = Student(
            name = name,
            subject = subject,
            subscriptionStartDate = subscriptionStartDate,
            subscriptionEndDate = subscriptionEndDate,
            batchTimes = batchTimes,
            daysOfWeek = daysOfWeek
        )
        database.studentDao().insert(student)
        return true
    }

    suspend fun addOrRenewStudent(
        newStudent: Student
    ) {
        val studentDao = database.studentDao()
        val existing = studentDao.findByNameAndSubject(
            newStudent.name,
            newStudent.subject
        )

        if (existing.isEmpty()) {
            studentDao.insert(newStudent)
            return
        }

        val exactMatch = existing.filter {
            it.daysOfWeek == newStudent.daysOfWeek &&
            it.batchTimes == newStudent.batchTimes
        }.maxByOrNull { it.subscriptionEndDate }

        // Strict renewal:
        // 1. Attributes match (already filtered)
        // 2. Dates are contiguous or overlapping
        //    (newStart <= oldEnd + small_buffer)
        //    User said: "renew start is on the same day as the end date"
        //    We check if newStart is close to oldEnd.
        
        val isContiguous = if (exactMatch != null) {
            val gap = newStudent.subscriptionStartDate - exactMatch.subscriptionEndDate
            // 24 hours in millis = 86400000. 
            // Allow if gap is small (e.g. < 24h) or negative (overlap).
            gap <= 86400000L 
        } else false

        if (exactMatch != null && isContiguous) {
            // Extend existing subscription
            // The user said "subscription is increased" -> we just move the end date
            // The UI calculates newEndDate based on added months/days.
            studentDao.update(
                exactMatch.copy(
                    subscriptionEndDate = newStudent.subscriptionEndDate
                )
            )
        } else {
            // New subscription entity for ANY other difference
            studentDao.insert(newStudent)
        }
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