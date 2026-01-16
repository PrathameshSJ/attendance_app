package raegae.shark.attnow.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.*

@Entity(
    tableName = "attendance",
    primaryKeys = ["studentId", "date","isPresent"],
    foreignKeys = [
        ForeignKey(
            entity = Student::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("studentId")]
)
data class Attendance(
    val studentId: Int,
    val date: Long,
    val isPresent: Boolean
)



