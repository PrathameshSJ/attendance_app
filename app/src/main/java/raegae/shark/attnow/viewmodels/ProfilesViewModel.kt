package raegae.shark.attnow.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.map
import raegae.shark.attnow.data.AppDatabase

class ProfilesViewModel(private val database: AppDatabase) : ViewModel() {

    val students =
            database.studentDao().getAllStudents().map { list ->
                list.sortedByDescending { it.id }.distinctBy { it.name to it.subject }
            }
}

class ProfilesViewModelFactory(private val application: android.app.Application) :
        ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfilesViewModel::class.java)) {
            val database = AppDatabase.getDatabase(application)
            @Suppress("UNCHECKED_CAST") return ProfilesViewModel(database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
