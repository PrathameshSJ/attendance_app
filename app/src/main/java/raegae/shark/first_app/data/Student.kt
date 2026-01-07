package raegae.shark.first_app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "student")
@TypeConverters(Converters::class)
data class Student(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val subject: String,
    val subscriptionEndDate: Long,
    val batchTime: String,
    val daysOfWeek: List<String>
)
