package raegae.shark.attnow.data.export

import android.content.Context
import android.net.Uri
import org.dhatim.fastexcel.reader.ReadableWorkbook
import org.dhatim.fastexcel.StyleSetter
import raegae.shark.attnow.data.AppDatabase
import raegae.shark.attnow.data.Attendance
import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import java.io.FileNotFoundException

class AttendanceExcelImporter(
    private val context: Context
) {

    fun read(uri: Uri): List<Attendance> {
        val input = context.contentResolver.openInputStream(uri) ?: return emptyList()
        val workbook = ReadableWorkbook(input)

        

        val result = mutableListOf<Attendance>()

        workbook.sheets.forEach { sheet ->
            val rows = sheet.openStream().toList().drop(1)
            val year = extractYear(sheet.name)

            rows.forEach { row ->
                val studentId = row.getCell(0)?.asNumber()?.toInt() ?: return@forEach

                for (col in 4 until row.cellCount) {
                    val cell = row.getCell(col) ?: continue
                    val value = cell.asString() ?: continue

                    // Expect: P(03/03) or A(03/03)
                    if (value.length < 6) continue

                    val present = value.startsWith("P")
                    val datePart = value.substringAfter("(").substringBefore(")")

                    val date = LocalDate.parse("$datePart/$year", DateTimeFormatter.ofPattern("dd/MM/yyyy"))

                    result += Attendance(
                        studentId = studentId,
                        date = date.toEpochDay(),
                        isPresent = present
                    )
                }
            }
        }

        input.close()
        return result
    }



/* 
class AttendanceExcelImporter(
    private val context: Context,
    private val database: AppDatabase
) {
    
    private val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    
    suspend fun import(uri: Uri) {
        val input: InputStream =
            context.contentResolver.openInputStream(uri) ?: throw FileNotFoundException()//return

        val workbook = ReadableWorkbook(input)
        rememberCoroutineScope().launch{
        workbook.sheets.forEach { sheet ->
            importSheet(sheet)
        }}
        
        input.close()
    }

    
    private suspend fun importSheet(sheet: org.dhatim.fastexcel.reader.Sheet) {
        
        val rows = sheet.openStream().toList()
        if (rows.size <= 1) return

        val year = extractYear(sheet.name)

        rows.drop(1).forEach { row ->
            val studentId = row.getCell(0)?.asNumber()?.toInt() ?: return@forEach

            for (col in 4 until row.cellCount) {
                val cell = row.getCell(col) ?: continue
                val value = cell.asString() ?: continue

                // Expect: P(03/03) or A(03/03)
                if (value.length < 6) continue

                val present = value.startsWith("P")
                val datePart = value.substringAfter("(").substringBefore(")")

                val date = LocalDate.parse(
                    "$datePart/$year",
                    formatter
                )
                
                database.attendanceDao().upsert(
                    Attendance(
                        studentId = studentId,
                        date = date.toEpochDay(),
                        isPresent = present
                    )
                )
            }
        }
    }


    private fun importSheet(sheet: org.dhatim.fastexcel.reader.Sheet) {
        val rows = sheet.openStream().toList()
        if (rows.size < 1) return

        val year = extractYear(sheet.name)

        rows.drop(1).forEach { row ->
            val studentId = row.getCell(0)?.asNumber()?.toInt() ?: return@forEach
            val monthName = row.getCell(3)?.asString() ?: return@forEach
            val month = monthFromName(monthName)

            for (col in 4 until row.cellCount) {
                val cell = row.getCell(col) ?: continue
                val text = cell.asString() ?: continue

                val date = LocalDate.parse("$text/$year", DateTimeFormatter.ofPattern("dd/MM/yyyy"))

                val present = cell.style?.fillColor?.let {
                    it.equals("92D050", true)
                } ?: false

                database.attendanceDao().upsert(
                    Attendance(
                        studentId = studentId,
                        date = date.toEpochDay(),
                        isPresent = present
                    )
                )
            }
        }
    }
*/
    private fun extractYear(sheetName: String): Int {
        return sheetName.takeLast(4).toIntOrNull()
            ?: LocalDate.now().year
    }

    private fun monthFromName(name: String): Int =
        java.time.Month.valueOf(name.uppercase()).value
}
