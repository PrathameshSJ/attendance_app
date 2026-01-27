package raegae.shark.attnow.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import raegae.shark.attnow.data.AppDatabase
import raegae.shark.attnow.data.export.AttendanceExcelManager
import raegae.shark.attnow.data.AppGlobalState

class SettingsViewModel(
    application: Application
) : AndroidViewModel(application) {

    /* ---------- Dependencies ---------- */

    private val context = application.applicationContext
    private val database = AppDatabase.getDatabase(application)
    private val excelManager = AttendanceExcelManager(context,database)

    /* ---------- UI State ---------- */

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _importSuccess = MutableStateFlow(false)
    val importSuccess: StateFlow<Boolean> = _importSuccess

    /* ---------- EXPORT ---------- */

    fun exportAttendance(uri: Uri) {
        viewModelScope.launch {
            _isBusy.value = true
            _error.value = null

            try {
                val students = database.studentDao().getAllStudents()
                val attendance = database.attendanceDao().getAllAttendance()
                

                excelManager.exportAll(
                    uri = uri,
                    student = students,
                    attendance = attendance
                )
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isBusy.value = false
            }
        }
    }

    /* ---------- IMPORT ---------- */

    /**
     * Current behavior:
     * - call the fun, it handles everything
     */

    fun importAttendance(uri: Uri) {
        viewModelScope.launch {
            _isBusy.value = true
            _error.value = null
            AppGlobalState.setImporting(true)

            try {
                excelManager.import(uri)
                _importSuccess.value = true
            } catch (e: Exception) {
                _error.value = e.message
                AppGlobalState.setImporting(false) // Only unfreeze on error
            } finally {
                _isBusy.value = false
            }
        }
    }


}
/*
    fun importAttendance(uri: Uri) {
        viewModelScope.launch {
            _isBusy.value = true
            _error.value = null

            try {
                val importedAttendance = excelManager.import(uri)

                importedAttendance.forEach {
                    database.attendanceDao().upsert(it)
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isBusy.value = false
            }
        }
    }
}
*/