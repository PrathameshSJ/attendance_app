package raegae.shark.attnow.data.model

import raegae.shark.attnow.data.util.StudentKey

/**
 * A LogicalStudent represents a merged view of one or more physical Student entities.
 *
 * Identity is defined by StudentKey (name + subject).
 */
data class LogicalStudent(

        /* ---------- identity ---------- */

        val key: StudentKey,

        /**
         * Physical student ID that should be used for attendance marking and updates.
         *
         * Always the entity with the latest subscriptionEndDate.
         */
        val activeEntityId: Int,

        /** All physical student IDs that belong to this logical student. */
        val entityIds: List<Int>,
        val entities: List<raegae.shark.attnow.data.Student>,

        /* ---------- display ---------- */

        val name: String,
        val subject: String,

        /* ---------- subscription ---------- */

        val subscriptionStartDate: Long,
        val subscriptionEndDate: Long,

        /* ---------- schedule ---------- */

        val daysOfWeek: List<String>,
        val batchTimes: Map<String, String>,

        /**
         * Ranges of active subscription periods (start to end) derived from all merged entities.
         * Used for grey/gap logic in calendar.
         */
        val subscriptionRanges: List<Pair<Long, Long>>,

        /** Phone number of the active student entity. */
        val phoneNumber: String = ""
)
