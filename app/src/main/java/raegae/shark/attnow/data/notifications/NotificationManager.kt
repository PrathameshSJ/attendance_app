package raegae.shark.attnow.data.notifications

import android.content.Context
import android.util.Log
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import raegae.shark.attnow.data.AppDatabase
import raegae.shark.attnow.data.logic.LogicalStudentMerger

class NotificationManager(private val context: Context) {
        private val db = AppDatabase.getDatabase(context)
        private val studentDao = db.studentDao()
        private val attendanceDao = db.attendanceDao()

        suspend fun getNotifications(): List<AppNotification> =
                withContext(Dispatchers.IO) {
                        val notifications = mutableListOf<AppNotification>()

                        // 1. Get all students and populate logical students
                        val allStudents = studentDao.getAllStudentsOnce()
                        val allAttendance = attendanceDao.getAllAttendanceOnce()
                        val logicalStudents = LogicalStudentMerger.merge(allStudents)

                        val today =
                                Calendar.getInstance()
                                        .apply {
                                                set(Calendar.HOUR_OF_DAY, 0)
                                                set(Calendar.MINUTE, 0)
                                                set(Calendar.SECOND, 0)
                                                set(Calendar.MILLISECOND, 0)
                                        }
                                        .timeInMillis

                        // 2. Process each logical student
                        for (logicalStudent in logicalStudents) {
                                val entities =
                                        logicalStudent.entities.sortedBy {
                                                it.subscriptionStartDate
                                        }
                                if (entities.isEmpty()) continue

                                // Find current entity: either today is in range, or it's the last
                                // one
                                val currentEntity =
                                        entities.find {
                                                today in
                                                        it.subscriptionStartDate..it.subscriptionEndDate
                                        }
                                                ?: entities.last()

                                // Check Suppression: Is there a future entity?
                                // A future entity starts AFTER the current entity's end date
                                val futureEntityExists =
                                        entities.any {
                                                it.subscriptionStartDate >
                                                        currentEntity.subscriptionEndDate
                                        }

                                if (futureEntityExists) {
                                        Log.d(
                                                "NotificationManager",
                                                "Suppressing notification for ${logicalStudent.name} (${logicalStudent.subject}) - Future entity exists"
                                        )
                                        continue
                                }

                                // --- Check 1: Date Expiry ---
                                val msPerDay = 24 * 60 * 60 * 1000L
                                val daysRemaining =
                                        ((currentEntity.subscriptionEndDate - today) / msPerDay)
                                                .toInt()

                                // Only notify if we are close to end (e.g. within 10 days) and not
                                // already long
                                // expired
                                // (e.g. if expired 30 days ago, maybe don't show? User said "check
                                // remaining")
                                // For now, let's show even if negative (expired) to be safe, or
                                // clamp?
                                // "if user checks app... it should warning"

                                var dateWarningType: NotificationType? = null
                                if (daysRemaining <= 5)
                                        dateWarningType = NotificationType.CRITICAL_RED
                                else if (daysRemaining <= 10)
                                        dateWarningType = NotificationType.WARNING_YELLOW

                                if (dateWarningType != null) {
                                        notifications.add(
                                                AppNotification(
                                                        id = "date_${currentEntity.id}",
                                                        title =
                                                                "Subscription Expiring: ${logicalStudent.name}",
                                                        description =
                                                                "${logicalStudent.name}'s ${logicalStudent.subject} subscription ends in $daysRemaining days.",
                                                        studentName = logicalStudent.name,
                                                        studentSubject = logicalStudent.subject,
                                                        type = dateWarningType,
                                                        timestamp = System.currentTimeMillis(),
                                                        studentPhoneNumber =
                                                                logicalStudent.phoneNumber
                                                )
                                        )
                                }

                                // --- Check 2: Class Count Limit ---
                                if (currentEntity.max_days > 0) {
                                        // Count PRESENT attendance for THIS entity's range
                                        val presentCount =
                                                allAttendance.count {
                                                        it.studentId == currentEntity.id &&
                                                                it.isPresent &&
                                                                it.date >=
                                                                        currentEntity
                                                                                .subscriptionStartDate &&
                                                                it.date <=
                                                                        currentEntity
                                                                                .subscriptionEndDate
                                                }

                                        val classesRemaining = currentEntity.max_days - presentCount

                                        var countWarningType: NotificationType? = null
                                        if (classesRemaining <= 1)
                                                countWarningType = NotificationType.CRITICAL_RED
                                        else if (classesRemaining <= 2)
                                                countWarningType = NotificationType.WARNING_YELLOW

                                        // Avoid duplicate warning if both date and count trigger?
                                        // User said "OR condition". We can show both or merge.
                                        // Let's add separate notification for count clarity.

                                        if (countWarningType != null) {
                                                notifications.add(
                                                        AppNotification(
                                                                id = "count_${currentEntity.id}",
                                                                title =
                                                                        "Classes Running Out: ${logicalStudent.name}",
                                                                description =
                                                                        "${logicalStudent.name} has only $classesRemaining classes left for ${logicalStudent.subject}.",
                                                                studentName = logicalStudent.name,
                                                                studentSubject =
                                                                        logicalStudent.subject,
                                                                type = countWarningType,
                                                                timestamp =
                                                                        System.currentTimeMillis(),
                                                                studentPhoneNumber =
                                                                        logicalStudent.phoneNumber
                                                        )
                                                )
                                        }
                                }
                        }

                        return@withContext notifications
                }
}
