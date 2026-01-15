package raegae.shark.attnow.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.*

@Entity(
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
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: Int,
    val date: Long,
    val isPresent: Boolean
)


/* 
@Entity(
    tableName = "attendance",
    primaryKeys = ["studentId", "date"]
)
data class Attendance(
    val studentId: Int,
    val date: Long,
    val isPresent: Boolean
)
*/
