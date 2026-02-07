package raegae.shark.attnow.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import raegae.shark.attnow.data.AppDatabase
import raegae.shark.attnow.data.AppGlobalState
import raegae.shark.attnow.data.export.AttendanceExcelManager

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    /* ---------- Dependencies ---------- */

    private val context = application.applicationContext
    private val database = AppDatabase.getDatabase(application)
    private val excelManager = AttendanceExcelManager(context, database)

    /* ---------- UI State ---------- */

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _importSuccess = MutableStateFlow(false)
    val importSuccess: StateFlow<Boolean> = _importSuccess

    private val _importLog = MutableStateFlow<String?>(null)
    val importLog: StateFlow<String?> = _importLog

    val importReport = AppGlobalState.importResultLog

    fun confirmRestart() {
        _importSuccess.value = true
        _importLog.value = null
        AppGlobalState.clearImportResult()
    }

    fun clearLocalLog() {
        _importLog.value = null
    }

    fun clearError() {
        _error.value = null
    }

    /* ---------- EXPORT ---------- */

    fun exportAttendance(uri: Uri) {
        viewModelScope.launch {
            _isBusy.value = true
            _error.value = null

            try {
                val students = database.studentDao().getAllStudents()
                val attendance = database.attendanceDao().getAllAttendance()

                excelManager.exportAll(uri = uri, student = students, attendance = attendance)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isBusy.value = false
            }
        }
    }

    suspend fun exportToTempFile(): java.io.File? =
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    _isBusy.value = true
                    val file = java.io.File(context.cacheDir, "backup_temp.xlsx")
                    if (file.exists()) file.delete()

                    val students = database.studentDao().getAllStudents()
                    val attendance = database.attendanceDao().getAllAttendance()

                    excelManager.exportAll(
                            Uri.fromFile(file),
                            student = students,
                            attendance = attendance
                    )

                    if (!file.exists()) {
                        _error.value = "No data to backup"
                        return@withContext null
                    }
                    file
                } catch (e: Exception) {
                    _error.value = "Export failed: ${e.message}"
                    null
                } finally {
                    _isBusy.value = false
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

                // Copy to local file to prevent read errors and allow background processing
                val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
                val importsDir = java.io.File(baseDir, "imports")
                if (!importsDir.exists()) importsDir.mkdirs()
                val file = java.io.File(importsDir, "pending_import.xlsx")

                android.util.Log.d("SettingsVM", "Starting Copy to ${file.absolutePath}")

                context.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
                        ?: throw Exception("Could not read source file")

                android.util.Log.d("SettingsVM", "Copy Success. Queued for Background Watcher.")

                // Show log to user
                _importLog.value =
                        "File Queued for Background Import.\nPlease wait up to 5 seconds for the process to start..."
            } catch (t: Throwable) {
                android.util.Log.e("SettingsVM", "Queue Failed", t)
                _error.value = "Queue Failed: ${t.message}"
                // AppGlobalState.setImporting(false) // Not setting it here
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
