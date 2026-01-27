package raegae.shark.attnow.data.export

import android.content.Context
import android.net.Uri
import raegae.shark.attnow.data.AppDatabase
import raegae.shark.attnow.data.Attendance
import org.dhatim.fastexcel.reader.ReadableWorkbook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar


data class ImportedStudentAttendance(
    val name: String,
    val subject: String,
    val batch: String,
    val dateMillis: Long,
    val isPresent: Boolean?
)

class AttendanceExcelImporter(
    private val context: Context
) {

    fun read(uri: Uri): List<Attendance> {
        val input = context.contentResolver.openInputStream(uri)
            ?: return emptyList()

        val workbook = ReadableWorkbook(input)
        val result = mutableListOf<Attendance>()

        /*workbook.sheets.forEach { sheet ->
            val year = extractYear(sheet.name)

            var isHeader = true

            sheet.openStream()
                .forEach { row ->

                    if (isHeader) {
                        isHeader = false
                        return@forEach
                    }

                    val studentId =
                        row.getCell(0)?.asNumber()?.toInt() ?: return@forEach

                    for (col in 4 until row.cellCount) {
                        val cell = row.getCell(col) ?: continue
                        val text = cell.asString() ?: continue
                        if (text.length < 7) continue

                        // Expect: P(03/03) or A(03/03)
                        val isPresent = if (text.startsWith("P")) true else if (text.startsWith("A")) false else null
                        val datePart = text.substringAfter("(").substringBefore(")")

                        val date = parseDate(datePart, year) ?: continue

                        result += ImportedStudentAttendance(
                            name = name,
                            subject = subject,
                            batch = batch,
                            dateMillis = date.toEpochDay() * 86_400_000,
                            isPresent = isPresent
                        )
                    }
                }
        }*/

        input.close()
        return result
    }

    private fun extractYear(sheetName: String): Int =
        sheetName.takeLast(4).toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)

    private fun parseDate(text: String, year: Int): Long? {
        // text = dd/MM
        val parts = text.split("/")
        if (parts.size != 2) return null

        val day = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null

        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return cal.timeInMillis
    }
}
