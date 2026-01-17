package raegae.shark.attnow.data.export

import org.dhatim.fastexcel.Workbook
import org.dhatim.fastexcel.Worksheet
import org.dhatim.fastexcel.StyleSetter
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import raegae.shark.attnow.data.AppDatabase
import android.content.Context

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

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM")

    fun export(
        students: List<ExportStudent>,
        outputStream: OutputStream
    ) {
        Workbook(outputStream, "AttNow", "1.0").use { workbook ->

            

            // Group by subject + year → sheets
            val sheets = students.groupBy { "${it.subject}_${it.year}" }

            sheets.forEach { (sheetName, sheetStudents) ->
                val sheet = workbook.newWorksheet(sheetName)

                writeHeader(sheet)

                var rowIndex = 1

                sheetStudents.forEach { student ->
                    for (month in 1..12) {
                        writeStudentMonthRow(
                            sheet = sheet,
                            row = rowIndex++,
                            student = student,
                            month = month
                        )
                    }
                }
            }
        }
    }

    private fun writeHeader(sheet: org.dhatim.fastexcel.Worksheet) {
        sheet.value(0, 0, "Student ID")
        sheet.value(0, 1, "Name")
        sheet.value(0, 2, "Batch")
        sheet.value(0, 3, "Month")

        for (day in 1..31) {
            sheet.value(0, 3 + day, day)
        }
    }

    private fun writeStudentMonthRow(
        sheet: org.dhatim.fastexcel.Worksheet,
        row: Int,
        student: ExportStudent,
        month: Int
    ) {
        sheet.value(row, 0, student.id)
        sheet.value(row, 1, student.name)
        sheet.value(row, 2, student.batch)
        sheet.value(row, 3, monthName(month))

        for (day in 1..31) {
            val date = runCatching {
                LocalDate.of(student.year, month, day)
            }.getOrNull() ?: continue

            val status = student.attendance[date] ?: continue

            val col = 3 + day
            sheet.value(row, col, date.format(dateFormatter))

            val styleSetter = sheet.style(row, col)
            if (status) {
                styleSetter.fillColor("92D050") // Present
            } else {
                styleSetter.fillColor("FF4C4C") // Absent
            }
        }
    }

    private fun monthName(month: Int): String =
        java.time.Month.of(month).name.lowercase().replaceFirstChar { it.uppercase() }
}
