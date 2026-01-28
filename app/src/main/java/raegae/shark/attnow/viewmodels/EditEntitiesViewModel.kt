package raegae.shark.attnow.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import raegae.shark.attnow.data.AppDatabase
import raegae.shark.attnow.data.Student
import raegae.shark.attnow.data.util.StudentKey

class EditEntitiesViewModel(private val database: AppDatabase, private val studentKey: StudentKey) :
        ViewModel() {

    private val studentDao = database.studentDao()

    // Working State
    // We load once and then manage purely in memory until Save
    private val _entities = MutableStateFlow<List<Student>>(emptyList())
    val entities = _entities

    private val _selectedIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedIds = _selectedIds

    private val _deletedIds = MutableStateFlow<Set<Int>>(emptySet())

    init {
        loadEntities()
    }

    private fun loadEntities() {
        viewModelScope.launch {
            val list = studentDao.findByNameAndSubject(studentKey.name, studentKey.subject)
            _entities.value = list.sortedBy { it.subscriptionStartDate }
        }
    }

    /* ---------- Selection Logic ---------- */

    fun toggleSelection(id: Int) {
        val current = _selectedIds.value
        if (current.contains(id)) {
            _selectedIds.value = current - id
        } else {
            _selectedIds.value = current + id
        }
    }

    fun selectAll() {
        _selectedIds.value = _entities.value.map { it.id }.toSet()
    }

    fun inverseSelection() {
        val all = _entities.value.map { it.id }.toSet()
        val current = _selectedIds.value
        _selectedIds.value = all - current
    }

    /* ---------- Modification Logic ---------- */

    fun deleteSelected() {
        val selected = _selectedIds.value
        if (selected.isEmpty()) return

        val currentList = _entities.value

        // Remove from list
        _entities.value = currentList.filter { !selected.contains(it.id) }

        // Add to deleted queue
        _deletedIds.value = _deletedIds.value + selected

        // Clear selection
        _selectedIds.value = emptySet()
    }

    fun updateEntity(updatedStudent: Student) {
        val list = _entities.value.toMutableList()
        val index = list.indexOfFirst { it.id == updatedStudent.id }
        if (index != -1) {
            list[index] = updatedStudent
            _entities.value = list.sortedBy { it.subscriptionStartDate } // Keep sorted
        }
    }

    fun getEntity(id: Int): Student? {
        return _entities.value.find { it.id == id }
    }

    /* ---------- Save Logic ---------- */

    fun saveChanges() {
        viewModelScope.launch {
            database.withTransaction {
                // 1. Delete
                _deletedIds.value.forEach { id -> studentDao.deleteById(id) }

                // 2. Update all remaining (Upsert or Update)
                // Since we loaded them, they have IDs.
                // We can just update all of them or track modified ones.
                // Updating all is safer/easier.
                _entities.value.forEach { student -> studentDao.update(student) }
            }
        }
    }
}

class EditEntitiesViewModelFactory(
        private val context: Context,
        private val studentKey: StudentKey
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditEntitiesViewModel::class.java)) {
            val db = AppDatabase.getDatabase(context)
            @Suppress("UNCHECKED_CAST") return EditEntitiesViewModel(db, studentKey) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
