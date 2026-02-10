package raegae.shark.attnow.data

import androidx.room.*
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.io.Serializable

@Entity(tableName = "student")
@TypeConverters(Converters::class)
data class Student(
        @PrimaryKey(autoGenerate = true) val id: Int = 0,
        val name: String,
        val subject: String,
        val subscriptionStartDate: Long,
        val subscriptionEndDate: Long,
        val batchTimes: Map<String, String>,
        val daysOfWeek: List<String>,
        val max_classes: Int = 0,
        val phoneNumber: String = ""
) : Serializable
