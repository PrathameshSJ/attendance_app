package raegae.shark.first_app.ui.add

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import raegae.shark.first_app.data.AppDatabase
import raegae.shark.first_app.data.Student

class AddStudentViewModel(application: Application) : AndroidViewModel(application) {

    private val studentDao = AppDatabase.getDatabase(application).studentDao()

    fun addStudent(
        name: String,
        subject: String,
        subscriptionEndDate: Long,
        batchTime: String,
        daysOfWeek: List<String>
    ) {
        viewModelScope.launch {
            studentDao.insert(
                Student(
                    name = name,
                    subject = subject,
                    subscriptionEndDate = subscriptionEndDate,
                    batchTime = batchTime,
                    daysOfWeek = daysOfWeek
                )
            )
        }
    }
}
