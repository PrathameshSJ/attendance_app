package raegae.shark.attnow.data.export

import org.dhatim.fastexcel.Workbook
import org.dhatim.fastexcel.Worksheet
import org.dhatim.fastexcel.StyleSetter
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import raegae.shark.attnow.data.AppDatabase
import android.content.Context
import android.net.Uri
import androidx.room.Database
import org.dhatim.fastexcel.reader.ReadableWorkbook
import raegae.shark.attnow.data.Attendance
import java.io.InputStream
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import java.io.FileNotFoundException
import java.util.Calendar

data class ExportStudent(
    val id: Int,
    val name: String,
    val batch: String,
    val subject: String,
    val year: Int,
    val attendance: Map<LocalDate, Boolean> // true = present, false = absent
)


class AttendanceExcelExporter(
    private val context: Context,
    private val database: AppDatabase
) {

    suspend fun export(uri: Uri) = withContext(Dispatchers.IO) {

        // 🔹 FORCE data evaluation (VERY IMPORTANT)
        val students = database.studentDao().getAllStudentsOnce()
        val attendance = database.attendanceDao().getAllAttendanceOnce()

        if (students.isEmpty()) return@withContext

        val outputStream =
            context.contentResolver.openOutputStream(uri)
                ?: throw IllegalStateException("Cannot open output stream")

        Workbook(outputStream, "AttNow", "1.0").use { workbook ->

            val bySubject = students.groupBy { it.subject }
            val attendanceByStudent = attendance.groupBy { it.studentId }


            


            
            bySubject.forEach { (subject, subjectStudents) ->

                val sheetName = "${subject}_${LocalDate.now().year}"
                val sheet = workbook.newWorksheet(sheetName)

                writeHeader(sheet)

                var rowIndex = 1

                subjectStudents.forEach { student ->

                    val startRow = rowIndex

                    val studentAttendance = attendanceByStudent[student.id].orEmpty()

                    // group attendance by month (0..11)
                    val byMonth = studentAttendance.groupBy {
                        Calendar.getInstance().apply {
                            timeInMillis = it.date
                        }.get(Calendar.MONTH)
                    }

                    for (month in 0..11) {

                        // --- fixed columns ---
                        sheet.value(rowIndex, 0, student.id)
                        sheet.value(rowIndex, 1, student.name)
                        sheet.value(rowIndex, 2, formatBatchTimes(student.batchTimes))
                        sheet.value(
                            rowIndex,
                            3,
                            java.time.Month.of(month + 1)
                                .name.lowercase()
                                .replaceFirstChar { it.uppercase() }
                        )

                        val monthAttendance = byMonth[month].orEmpty()

                        val byDay = monthAttendance.associateBy {
                            Calendar.getInstance().apply {
                                timeInMillis = it.date
                            }.get(Calendar.DAY_OF_MONTH)
                        }

                        for (day in 1..31) {
                            val entry = byDay[day] ?: continue

                            val cal = Calendar.getInstance().apply {
                                timeInMillis = entry.date
                            }

                            val text = "%02d/%02d".format(
                                cal.get(Calendar.DAY_OF_MONTH),
                                cal.get(Calendar.MONTH) + 1
                            )

                            val col = 3 + day
                            sheet.value(rowIndex, col, text)

                            // ✅ FastExcel coloring (THIS is the right way)
                            if (entry.isPresent) {
                                sheet.style(rowIndex, col).fillColor("92D050").set() // green
                            } else {
                                sheet.style(rowIndex, col).fillColor("FF6B6B").set() // red
                            }
                        }

                        rowIndex++
                    }

                        val endRow = rowIndex - 1

                        // Merge ID, Name, Batch columns vertically
                        sheet.range(startRow, 0, endRow, 0).merge() // ID
                        sheet.range(startRow, 1, endRow, 1).merge()// Name
                        sheet.range(startRow, 2, endRow, 2).merge() // Batch

                        sheet.style(startRow, 0).verticalAlignment("center").set()
                        sheet.style(startRow, 1).verticalAlignment("center").set()
                        sheet.style(startRow, 2).verticalAlignment("center").set()
                }
            }
        }

        outputStream.flush()
        outputStream.close()
    }

    // ---------- helpers ----------

    private fun writeHeader(sheet: Worksheet) {
        sheet.value(0, 0, "ID")
        sheet.value(0, 1, "Name")
        sheet.value(0, 2, "Batch")
        sheet.value(0, 3, "Month")

        var col = 4
        for (day in 1..31) {
            sheet.value(0, col++, day)
        }
    }

    private fun writeStudentRow(
        sheet: Worksheet,
        rowIndex: Int,
        student: raegae.shark.attnow.data.Student,
        attendance: List<raegae.shark.attnow.data.Attendance>
    ) {
        sheet.value(rowIndex, 0, student.id)
        sheet.value(rowIndex, 1, student.name)
        sheet.value(rowIndex, 2, formatBatchTimes(student.batchTimes))
        sheet.value(rowIndex, 3, "ALL")

        val formatter = DateTimeFormatter.ofPattern("dd/MM")

        attendance.forEach { record ->
            val date = LocalDate.ofEpochDay(record.date / 86_400_000)
            val col = 3 + date.dayOfMonth

            sheet.value(rowIndex, col, date.format(formatter))

            val color = if (record.isPresent) "#92D050" else "#FF6B6B"
            sheet.style(rowIndex, col).fillColor(color).set()
        }
    }

    private fun formatBatchTimes(batchTimes: Map<String, String>): String {
        if (batchTimes.isEmpty()) return ""
        return batchTimes.entries.joinToString(" | ") { (day, time) ->
            "$day: $time"
        }
    }}