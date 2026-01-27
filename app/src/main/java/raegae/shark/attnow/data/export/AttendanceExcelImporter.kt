package raegae.shark.attnow.data.export

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.dhatim.fastexcel.reader.ReadableWorkbook
import org.dhatim.fastexcel.reader.Row
import raegae.shark.attnow.data.AppDatabase
import raegae.shark.attnow.data.Attendance
import raegae.shark.attnow.data.Student
import java.util.Calendar

import androidx.room.withTransaction

class AttendanceExcelImporter(
    private val context: Context,
    private val database: AppDatabase
) {

    suspend fun import(uri: Uri) = withContext(Dispatchers.IO) {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Cannot open input stream")

        // Wrap in transaction to prevent UI flicker/crash from intermediate states
        database.withTransaction {
            ReadableWorkbook(input).use { workbook ->
                // 1. Parse Attributes & Ensure Entities Exist
                val idMap = parseAttributesAndSyncEntities(workbook)

                // 2. Parse Attendance & Merge
                val newAttendance = parseAttendance(workbook, idMap)

                // 3. Commit Attendance (Merge: Present Wins)
                commitAttendance(newAttendance)
            }
        }
    }

    private suspend fun parseAttributesAndSyncEntities(workbook: ReadableWorkbook): Map<Int, Int> {
        val attributesSheet = workbook.findSheet("Entities").orElse(null) ?: return emptyMap()
        val idMap = mutableMapOf<Int, Int>() // OldID -> RealID

        val rows = attributesSheet.read()
        if (rows.isEmpty()) return emptyMap()

        // Skip header
        for (i in 1 until rows.size) {
            val row = rows[i]
            val oldId = row.getCell(0)?.asNumber()?.toInt() ?: continue
            val name = row.getCell(1)?.asString() ?: ""
            val subject = row.getCell(2)?.asString() ?: ""
            val start = parseDateToMillis(row.getCell(3)?.asString() ?: "")
            val end = parseDateToMillis(row.getCell(4)?.asString() ?: "")
            val daysStr = row.getCell(5)?.asString() ?: ""
            val timesStr = row.getCell(6)?.asString() ?: ""

            // Check if exists
            val existing = database.studentDao().findByNameAndSubject(name, subject)
                .find {
                    // Fuzzy match on dates? Or exact? 
                    // Let's assume start/end match is strong enough signal for "same entity"
                    // along with name/subject.
                    it.subscriptionStartDate == start && it.subscriptionEndDate == end
                }

            if (existing != null) {
                idMap[oldId] = existing.id
            } else {
                // Create new
                val newEntity = Student(
                    name = name,
                    subject = subject,
                    subscriptionStartDate = start,
                    subscriptionEndDate = end,
                    daysOfWeek = if (daysStr.isBlank()) emptyList() else daysStr.split(","),
                    batchTimes = parseBatchTimes(timesStr)
                )
                val newId = database.studentDao().insert(newEntity).toInt()
                idMap[oldId] = newId
            }
        }
        return idMap
    }

    private fun parseBatchTimes(str: String): Map<String, String> {
        if (str.isBlank()) return emptyMap()
        // Format: Mon: 10-11 | Tue: 12-01
        return str.split(" | ").associate {
            val parts = it.split(": ")
            if (parts.size == 2) parts[0] to parts[1] else "" to ""
        }.filterKeys { it.isNotEmpty() }
    }

    private fun parseAttendance(workbook: ReadableWorkbook, idMap: Map<Int, Int>): List<Attendance> {
        val result = mutableListOf<Attendance>()

        workbook.sheets.forEach { sheet ->
            if (sheet.name == "Entities") return@forEach

            // subject_YEAR
            val strYear = sheet.name.takeLast(4)
            val year = strYear.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)

            val rows = sheet.read()
            if (rows.isEmpty()) return@forEach

            // Header mapping: colIndex -> DayOfMonth
            val header = rows[0]
            val colToDay = mutableMapOf<Int, Int>()
            for (c in 4 until header.cellCount) {
                val day = header.getCell(c)?.asNumber()?.toInt() ?: continue
                colToDay[c] = day
            }
            
            // Month column index is 3
            // But wait, the previous exporter loops by MONTH (0..11).
            // The existing format is:
            // Row: ID | Name | Batch | Month | 1 | 2 | ...
            // So each row represents ONE STUDENT for ONE MONTH.
            
            for (r in 1 until rows.size) {
                val row = rows[r]
                val oldId = row.getCell(0)?.asNumber()?.toInt() ?: continue
                val realId = idMap[oldId] ?: continue // Skip if we couldn't map entity

                val monthStr = row.getCell(3)?.asString() ?: continue
                val monthIndex = parseMonth(monthStr)

                // Loop days
                for ((col, day) in colToDay) {
                    val cellText = row.getCell(col)?.asString() ?: continue
                    if (cellText.isBlank()) continue

                    // P(...) or A(...)
                    val isPresent = cellText.startsWith("P")

                    // Reconstruct date
                    val dateMillis = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, monthIndex)
                        set(Calendar.DAY_OF_MONTH, day)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    result.add(
                        Attendance(
                            studentId = realId,
                            date = dateMillis,
                            isPresent = isPresent
                        )
                    )
                }
            }
        }
        return result
    }

    private suspend fun commitAttendance(newRecords: List<Attendance>) {
        if (newRecords.isEmpty()) return

        // Fetch existing for affected students
        val studentIds = newRecords.map { it.studentId }.distinct()
        val existing = database.attendanceDao().getAttendanceForStudentsOnce(studentIds)
        
        // Map: Key(studentId, date) -> Attendance
        val existingMap = existing.associateBy { "${it.studentId}_${it.date}" }

        val finalRecords = mutableListOf<Attendance>()

        for (newRec in newRecords) {
            val key = "${newRec.studentId}_${newRec.date}"
            val oldRec = existingMap[key]

            if (oldRec != null) {
                // Conflict: Present wins
                val finalStatus = oldRec.isPresent || newRec.isPresent
                
                if (oldRec.isPresent != finalStatus) {
                     finalRecords.add(oldRec.copy(isPresent = finalStatus))
                }
            } else {
                finalRecords.add(newRec)
            }
        }
        
        // Batch Upsert
        database.attendanceDao().upsertAll(finalRecords)
    }

    private fun parseMonth(monthName: String): Int {
        // "January", "February"...
        return try {
            java.time.Month.valueOf(monthName.uppercase()).value - 1
        } catch (e: Exception) {
            0
        }
    }

    private fun parseDateToMillis(dateStr: String): Long {
        if (dateStr.isBlank()) return 0L
        return try {
            val ld = java.time.LocalDate.parse(dateStr, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
            ld.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (e: Exception) {
            0L
        }
    }
}
