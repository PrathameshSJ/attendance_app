package raegae.shark.attnow.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import androidx.room.*

@Entity(tableName = "student")
@TypeConverters(Converters::class)
data class Student(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val subject: String,
    val subscriptionStartDate: Long,
    val subscriptionEndDate: Long,
    val batchTimes: Map<String, String>,
    val daysOfWeek: List<String>
){
    @Ignore
    var isPinned: Boolean = false
}
