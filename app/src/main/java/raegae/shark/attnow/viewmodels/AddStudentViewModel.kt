package raegae.shark.attnow.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.first
import raegae.shark.attnow.data.AppDatabase
import raegae.shark.attnow.data.Student

class AddStudentViewModel(private val database: AppDatabase) : ViewModel() {

    suspend fun addStudentIfNotExists(
            name: String,
            subject: String,
            subscriptionStartDate: Long,
            subscriptionEndDate: Long,
            batchTimes: Map<String, String>,
            daysOfWeek: List<String>,
            maxClasses: Int,
            phoneNumber: String
    ): Boolean {
        val students = database.studentDao().getAllStudents().first()
        val existing = students.find { it.name.equals(name, ignoreCase = true) }
        if (existing != null) return false
        val student =
                Student(
                        name = name,
                        subject = subject,
                        subscriptionStartDate = subscriptionStartDate,
                        subscriptionEndDate = subscriptionEndDate,
                        batchTimes = batchTimes,
                        daysOfWeek = daysOfWeek,
                        max_classes = maxClasses,
                        phoneNumber = phoneNumber
                )
        database.studentDao().insert(student)
        return true
    }

    suspend fun addOrRenewStudent(newStudent: Student) {
        val studentDao = database.studentDao()
        val existing = studentDao.findByNameAndSubject(newStudent.name, newStudent.subject)

        if (existing.isEmpty()) {
            studentDao.insert(newStudent)
            return
        }

        val exactMatch =
                existing
                        .filter {
                            it.daysOfWeek == newStudent.daysOfWeek &&
                                    it.batchTimes == newStudent.batchTimes
                        }
                        .maxByOrNull { it.subscriptionEndDate }

        // Strict renewal:
        // 1. Attributes match (already filtered)
        // 2. Dates are contiguous or overlapping
        //    (newStart <= oldEnd + small_buffer)
        //    User said: "renew start is on the same day as the end date"
        //    We check if newStart is close to oldEnd.

        val isContiguous =
                if (exactMatch != null) {
                    val gap = newStudent.subscriptionStartDate - exactMatch.subscriptionEndDate
                    // 24 hours in millis = 86400000.
                    // Allow if gap is small (e.g. < 24h) or negative (overlap).
                    gap <= 86400000L
                } else false

        if (exactMatch != null && isContiguous) {
            // Extend existing subscription
            // The user said "subscription is increased" -> we just move the end date
            // The UI calculates newEndDate based on added months/days.
            // also update max_classes (add new to old? or just validation?
            // "max classes are reduced per present" -> implies it's a limit for the DURATION.
            // If we extend duration, we probably extend max_classes too?
            // User said: "max classes are reduced per present... if a student buys subscription for
            // 3
            // mnths for 12 classes...
            // then he can complete 12 classes in 3 mnths... if he renew"
            // If extending, we should probably ADD the new max_classes to the existing one?
            // "renew start is on the same day as the end date"...
            // If it's a renewal, we usually create a NEW entity or extend.
            // My logic merges if contiguous.
            // If I merge, I should probably sum the max_classes.
            // "max_classes is to be inputted by the user... counter for a reminder"
            // If I renew for another 12 classes, total should be old_limit + 12.

            val newMaxClasses = exactMatch.max_classes + newStudent.max_classes

            studentDao.update(
                    exactMatch.copy(
                            subscriptionEndDate = newStudent.subscriptionEndDate,
                            max_classes = newMaxClasses
                    )
            )
        } else {
            // New subscription entity for ANY other difference
            studentDao.insert(newStudent)
        }
    }
}

class AddStudentViewModelFactory(private val application: android.app.Application) :
        ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddStudentViewModel::class.java)) {
            val database = AppDatabase.getDatabase(application)
            @Suppress("UNCHECKED_CAST") return AddStudentViewModel(database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
