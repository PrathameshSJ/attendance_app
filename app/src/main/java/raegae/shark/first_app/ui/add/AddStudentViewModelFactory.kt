package raegae.shark.first_app.ui.add

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class AddStudentViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddStudentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddStudentViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
