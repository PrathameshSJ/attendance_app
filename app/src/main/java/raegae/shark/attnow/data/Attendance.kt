package raegae.shark.attnow.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.*

@Entity(
    tableName = "attendance",
    primaryKeys = ["studentId", "date"],
    indices = [
        Index("studentId"),
        Index("date")
    ]
)
data class Attendance(
    val studentId: Int,
    val date: Long,
    val isPresent: Boolean
)



