package raegae.shark.attnow.data.export

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.lang.StringBuilder
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.dhatim.fastexcel.reader.ReadableWorkbook
import raegae.shark.attnow.data.AppDatabase
import raegae.shark.attnow.data.Attendance
import raegae.shark.attnow.data.Student

data class ImportResult(val success: Boolean, val message: String, val count: Int)

class AttendanceExcelImporter(private val context: Context, private val database: AppDatabase) {

    suspend fun import(file: File): ImportResult =
            withContext(Dispatchers.IO) {
                val sb = StringBuilder()
                var total = 0
                try {
                    if (!file.exists()) return@withContext ImportResult(false, "File not found", 0)

                    sb.appendLine("Reading file: ${file.name}")
                    val input = FileInputStream(file)

                    ReadableWorkbook(input).use { workbook ->
                        // Check available sheets
                        val allSheets =
                                workbook.sheets.collect(java.util.stream.Collectors.toList())
                        sb.appendLine("Sheets in file: [${allSheets.joinToString { it.name }}]")

                        // 1. Parse Attributes & Ensure Entities Exist
                        val idMap = parseAttributesAndSyncEntities(workbook, sb)
                        sb.appendLine("Synced ${idMap.size} entities")

                        if (idMap.isEmpty()) {
                            if (allSheets.none { it.name == "Entities" }) {
                                sb.appendLine("ERROR: 'Entities' sheet NOT found! Cannot map IDs.")
                                return@withContext ImportResult(false, sb.toString(), 0)
                            }
                        }

                        // 2. Stream & Batch Commit (Memory Optimization)
                        total = streamAttendanceAndCommit(workbook, idMap, sb)
                        sb.appendLine("Total Imported: $total")
                    }
                    ImportResult(true, sb.toString(), total)
                } catch (t: Throwable) {
                    ImportResult(false, sb.toString() + "\nCRITICAL ERROR: ${t.message}", total)
                }
            }

    private suspend fun streamAttendanceAndCommit(
            workbook: ReadableWorkbook,
            idMap: Map<Int, Int>,
            sb: StringBuilder
    ): Int {
        var totalRecords = 0
        val buffer = mutableListOf<Attendance>()
        val BATCH_SIZE = 500

        val sheetList = workbook.sheets.collect(java.util.stream.Collectors.toList())

        for (sheet in sheetList) {
            if (sheet.name == "Entities") continue

            sb.appendLine("Processing '${sheet.name}'...")

            val strYear = sheet.name.takeLast(4)
            val year = strYear.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)

            val rows = sheet.read()
            if (rows.isEmpty()) {
                sb.appendLine("  -> Empty sheet (0 rows)")
                continue
            }

            // Header mapping
            val header = rows[0]
            val colToDay = mutableMapOf<Int, Int>()
            for (c in 0 until header.cellCount) {
                val cell = header.getCell(c) ?: continue
                // Safely check if it's a number (Day 1..31)
                // New format: Cols 4(MaxClasses), 5(Phone) are Strings.
                // Old format: Cols 4, 5 are Days.
                try {
                    if (cell.type == org.dhatim.fastexcel.reader.CellType.NUMBER) {
                        val day = cell.asNumber().toInt()
                        if (day in 1..31) {
                            colToDay[c] = day
                        }
                    }
                } catch (e: Exception) {
                    // Ignore non-number headers
                }
            }
            // sb.appendLine("  -> Found ${colToDay.size} day columns")

            var sheetRecs = 0
            for (r in 1 until rows.size) {
                val row = rows[r]
                val oldId = row.getCell(0)?.asNumber()?.toInt() ?: continue
                val realId = idMap[oldId]

                if (realId == null) {
                    // sb.appendLine("  -> Row $r: Skipped (Old ID $oldId not found in map)")
                    continue
                }

                val monthStr = row.getCell(5)?.asString() ?: continue
                val monthIndex = parseMonth(monthStr)

                for ((col, day) in colToDay) {
                    if (col >= row.cellCount) continue
                    val cellText = row.getCell(col)?.asString() ?: continue
                    if (cellText.isBlank()) continue

                    val isPresent = cellText.startsWith("P") // Wait, startsWith lower case?
                    // Previous code kept consistent with StartsWith "P"
                    // But I should check method case. `startsWith`.

                    val dateMillis =
                            Calendar.getInstance()
                                    .apply {
                                        set(Calendar.YEAR, year)
                                        set(Calendar.MONTH, monthIndex)
                                        set(Calendar.DAY_OF_MONTH, day)
                                        set(Calendar.HOUR_OF_DAY, 0)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    .timeInMillis

                    buffer.add(
                            Attendance(
                                    studentId = realId,
                                    date = dateMillis,
                                    isPresent = cellText.startsWith("P")
                            )
                    )

                    if (buffer.size >= BATCH_SIZE) {
                        commitBatch(buffer)
                        totalRecords += buffer.size
                        buffer.clear()
                    }
                }
            }
        }

        if (buffer.isNotEmpty()) {
            commitBatch(buffer)
            totalRecords += buffer.size
        }

        return totalRecords
    }

    private suspend fun commitBatch(newRecords: List<Attendance>) {
        if (newRecords.isEmpty()) return

        // Fetch existing ONLY for this batch
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

    private suspend fun parseAttributesAndSyncEntities(
            workbook: ReadableWorkbook,
            sb: StringBuilder
    ): Map<Int, Int> {
        val attributesSheet = workbook.findSheet("Entities").orElse(null)
        if (attributesSheet == null) return emptyMap()

        val idMap = mutableMapOf<Int, Int>()

        val rows = attributesSheet.read()
        if (rows.isEmpty()) {
            sb.appendLine("Entities sheet read, but it has 0 rows.")
            return emptyMap()
        }

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
            val maxClassesStr = row.getCell(7)?.asString() ?: ""
            val phoneNumber = row.getCell(8)?.asString() ?: ""

            // Check if exists
            val existing =
                    database.studentDao().findByNameAndSubject(name, subject).find {
                        // Fuzzy match on dates? Or exact?
                        // Let's assume start/end match is strong enough signal for "same entity"
                        // along with name/subject.
                        it.subscriptionStartDate == start && it.subscriptionEndDate == end
                    }

            if (existing != null) {
                // Should we update existing phoneNumber/maxClasses if different?
                // User said "clean code", but didn't specify strict overwrite.
                // Importing implies "state restoration".
                // If I restore, maybe I should ensure attributes match?
                // For now, let's just map ID.
                idMap[oldId] = existing.id
            } else {
                // Create new
                val newEntity =
                        Student(
                                name = name,
                                subject = subject,
                                subscriptionStartDate = start,
                                subscriptionEndDate = end,
                                daysOfWeek =
                                        if (daysStr.isBlank()) emptyList() else daysStr.split(","),
                                batchTimes = parseBatchTimes(timesStr),
                                max_classes = maxClassesStr.toIntOrNull() ?: 0,
                                phoneNumber = phoneNumber
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
        return str.split(" | ")
                .associate {
                    val parts = it.split(": ")
                    if (parts.size == 2) parts[0] to parts[1] else "" to ""
                }
                .filterKeys { it.isNotEmpty() }
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
            val ld =
                    java.time.LocalDate.parse(
                            dateStr,
                            java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
                    )
            ld.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (e: Exception) {
            0L
        }
    }
}
