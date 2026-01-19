package raegae.shark.attnow.data.export

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import raegae.shark.attnow.data.AppDatabase
import raegae.shark.attnow.data.Attendance
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.room.Database
import kotlinx.coroutines.flow.*
import raegae.shark.attnow.data.*
import kotlinx.coroutines.*

class AttendanceExcelManager(
    private val context: Context,
    private val database: AppDatabase
) {

    
    private val importer = AttendanceExcelImporter(context)

    /* ---------- EXPORT ---------- */
    suspend fun exportAll(uri: Uri,student: Flow<List<Student>>,attendance: Flow<List<Attendance>> ) = withContext(Dispatchers.IO) {
        AttendanceExcelExporter(
            context = context,
            database = database
        ).export(uri)
    }

    fun import(uri: Uri): List<Attendance> {
        return importer.read(uri)
    }
}
