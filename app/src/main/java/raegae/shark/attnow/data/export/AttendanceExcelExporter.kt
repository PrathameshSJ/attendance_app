package raegae.shark.attnow.data.export

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import kotlinx.coroutines.*
import org.dhatim.fastexcel.Workbook
import org.dhatim.fastexcel.Worksheet
import raegae.shark.attnow.data.AppDatabase
import raegae.shark.attnow.data.Attendance

data class ExportStudent(
        val id: Int,
        val name: String,
        val batch: String,
        val subject: String,
        val year: Int,
        val attendance: Map<LocalDate, Boolean> // true = present, false = absent
)

class AttendanceExcelExporter(private val context: Context, private val database: AppDatabase) {

    suspend fun export(uri: Uri) =
            withContext(Dispatchers.IO) {

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

                    // 🔹 ENTITIES SHEET (First) for Restore
                    val entSheet = workbook.newWorksheet("Entities")
                    writeAttributesHeader(entSheet)
                    var entRow = 1
                    students.forEach { s -> writeAttributesRow(entSheet, entRow++, s) }
                    entSheet.finish()

                    bySubject.forEach { (subject, subjectStudents) ->
                        val sheetName = "${subject}_${LocalDate.now().year}"
                        val sheet = workbook.newWorksheet(sheetName)

                        writeHeader(sheet)

                        var rowIndex = 1

                        subjectStudents.forEach { student ->
                            val startRow = rowIndex

                            val studentAttendance = attendanceByStudent[student.id].orEmpty()

                            // group attendance by month (0..11)
                            val byMonth =
                                    studentAttendance.groupBy {
                                        Calendar.getInstance()
                                                .apply { timeInMillis = it.date }
                                                .get(Calendar.MONTH)
                                    }

                            for (month in 0..11) {

                                // --- fixed columns ---
                                sheet.value(rowIndex, 0, student.id)
                                sheet.value(rowIndex, 1, student.name)
                                sheet.value(rowIndex, 2, formatBatchTimes(student.batchTimes))

                                // NEW ORDER: 3=MaxClasses, 4=Phone, 5=Month
                                sheet.value(
                                        rowIndex,
                                        3,
                                        if (student.max_classes > 0) student.max_classes.toString()
                                        else "Unlimited"
                                )
                                sheet.value(rowIndex, 4, student.phoneNumber)
                                sheet.value(
                                        rowIndex,
                                        5,
                                        java.time.Month.of(month + 1)
                                                .name
                                                .lowercase()
                                                .replaceFirstChar { it.uppercase() }
                                )

                                val monthAttendance = byMonth[month].orEmpty()

                                val byDay =
                                        monthAttendance.associateBy {
                                            Calendar.getInstance()
                                                    .apply { timeInMillis = it.date }
                                                    .get(Calendar.DAY_OF_MONTH)
                                        }

                                for (day in 1..31) {
                                    val entry = byDay[day] ?: continue

                                    val cal =
                                            Calendar.getInstance().apply {
                                                timeInMillis = entry.date
                                            }

                                    val text =
                                            "%02d/%02d".format(
                                                    cal.get(Calendar.DAY_OF_MONTH),
                                                    cal.get(Calendar.MONTH) + 1
                                            )

                                    val prefix = if (entry.isPresent) "P" else "A"
                                    val finalText = "$prefix($text)"

                                    val col = 5 + day // 5(Month) + 1 = 6. Correct.
                                    sheet.value(rowIndex, col, finalText)

                                    // ✅ FastExcel coloring (THIS is the right way)
                                    if (entry.isPresent) {
                                        sheet.style(rowIndex, col)
                                                .fillColor("92D050")
                                                .set() // green
                                    } else {
                                        sheet.style(rowIndex, col).fillColor("FF6B6B").set() // red
                                    }
                                }

                                rowIndex++
                            }

                            val endRow = rowIndex - 1

                            // Merge ID, Name, Batch, MaxClasses, Phone columns vertically
                            sheet.range(startRow, 0, endRow, 0).merge() // ID
                            sheet.range(startRow, 1, endRow, 1).merge() // Name
                            sheet.range(startRow, 2, endRow, 2).merge() // Batch
                            sheet.range(startRow, 3, endRow, 3).merge() // MaxClasses (Was 4)
                            sheet.range(startRow, 4, endRow, 4).merge() // Phone (Was 5)

                            sheet.style(startRow, 0).verticalAlignment("center").set()
                            sheet.style(startRow, 1).verticalAlignment("center").set()
                            sheet.style(startRow, 2).verticalAlignment("center").set()
                            sheet.style(startRow, 3).verticalAlignment("center").set()
                            sheet.style(startRow, 4).verticalAlignment("center").set()
                        } // End student loop
                    } // End subject loop
                } // End workbook use

                outputStream.flush()
                outputStream.close()
            }

    // ---------- helpers ----------

    private fun writeHeader(sheet: Worksheet) {
        sheet.value(0, 0, "ID")
        sheet.value(0, 1, "Name")
        sheet.value(0, 2, "Batch")
        sheet.value(0, 3, "MaxClasses")
        sheet.value(0, 4, "Phone")
        sheet.value(0, 5, "Month")

        var col = 6 // Shifted
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
        sheet.value(
                rowIndex,
                4,
                if (student.max_classes > 0) student.max_classes.toString() else "Unlimited"
        )
        sheet.value(rowIndex, 5, student.phoneNumber)

        val formatter = DateTimeFormatter.ofPattern("dd/MM")

        attendance.forEach { record ->
            val date = LocalDate.ofEpochDay(record.date / 86_400_000)
            val col = 5 + date.dayOfMonth // Shifted

            sheet.value(rowIndex, col, date.format(formatter))

            val color = if (record.isPresent) "#92D050" else "#FF6B6B"
            sheet.style(rowIndex, col).fillColor(color).set()
        }
    }

    private fun formatBatchTimes(batchTimes: Map<String, String>): String {
        if (batchTimes.isEmpty()) return ""
        return batchTimes.entries.joinToString(" | ") { (day, time) -> "$day: $time" }
    }

    private fun writeAttributesHeader(sheet: Worksheet) {
        sheet.value(0, 0, "ID")
        sheet.value(0, 1, "Name")
        sheet.value(0, 2, "Subject")
        sheet.value(0, 3, "StartDate")
        sheet.value(0, 4, "EndDate")
        sheet.value(0, 5, "Days")
        sheet.value(0, 6, "Times")
        sheet.value(0, 7, "MaxClasses")
        sheet.value(0, 8, "PhoneNumber")
    }

    private fun writeAttributesRow(sheet: Worksheet, r: Int, s: raegae.shark.attnow.data.Student) {
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
        val start =
                java.time.Instant.ofEpochMilli(s.subscriptionStartDate)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate()
                        .format(fmt)
        val end =
                java.time.Instant.ofEpochMilli(s.subscriptionEndDate)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate()
                        .format(fmt)

        sheet.value(r, 0, s.id)
        sheet.value(r, 1, s.name)
        sheet.value(r, 2, s.subject)
        sheet.value(r, 3, start)
        sheet.value(r, 4, end)
        sheet.value(r, 5, s.daysOfWeek.joinToString(","))
        sheet.value(r, 6, formatBatchTimes(s.batchTimes))
        sheet.value(r, 7, if (s.max_classes > 0) s.max_classes.toString() else "Unlimited")
        sheet.value(r, 8, s.phoneNumber)
    }
}
