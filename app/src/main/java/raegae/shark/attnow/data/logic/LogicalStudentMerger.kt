package raegae.shark.attnow.data.logic

import raegae.shark.attnow.data.Student
import raegae.shark.attnow.data.model.LogicalStudent
import raegae.shark.attnow.data.util.StudentKey

/**
 * Responsible for converting physical Student entities
 * into merged LogicalStudent objects.
 *
 * Rules:
 * - Logical identity = (name + subject)
 * - Multiple entities with same key are merged
 * - activeEntityId = entity with the latest subscriptionEndDate
 * - daysOfWeek & batchTimes are merged (union, last-write-wins)
 * - subscriptionStart = earliest start
 * - subscriptionEnd = latest end
 */
object LogicalStudentMerger {

    fun merge(students: List<Student>): List<LogicalStudent> {
        if (students.isEmpty()) return emptyList()

        return students
            .groupBy { StudentKey(it.name, it.subject) }
            .map { (key, entities) ->

                // Pick active entity = latest subscription end
                val active = entities.maxByOrNull { it.subscriptionEndDate }
                    ?: error("LogicalStudent with no entities: $key")

                val start = entities.minOf { it.subscriptionStartDate }
                val end = entities.maxOf { it.subscriptionEndDate }

                val mergedDays = entities
                    .flatMap { it.daysOfWeek }
                    .distinct()

                val mergedBatchTimes =
                    entities.fold(emptyMap<String, String>()) { acc, student ->
                        acc + student.batchTimes
                    }

                val subscriptionRanges = entities.map {
                    it.subscriptionStartDate to it.subscriptionEndDate
                }

                LogicalStudent(
                    key = key,
                    activeEntityId = active.id,
                    entityIds = entities.map { it.id },
                    name = key.name,
                    subject = key.subject,
                    subscriptionStartDate = start,
                    subscriptionEndDate = end,
                    daysOfWeek = mergedDays,
                    batchTimes = mergedBatchTimes,
                    subscriptionRanges = subscriptionRanges
                )
            }
    }
}
