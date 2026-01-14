package raegae.shark.attnow.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "attendance",
    primaryKeys = ["studentId", "date"]
)
data class Attendance(
    val studentId: Int,
    val date: Long,
    val isPresent: Boolean
)

