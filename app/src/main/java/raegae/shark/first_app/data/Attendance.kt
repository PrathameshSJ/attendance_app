package raegae.shark.first_app.data

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

