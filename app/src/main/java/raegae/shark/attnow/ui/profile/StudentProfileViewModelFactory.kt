package raegae.shark.attnow.ui.profile

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class StudentProfileViewModelFactory(private val application: Application, private val studentId: Int) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StudentProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StudentProfileViewModel(application, studentId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
