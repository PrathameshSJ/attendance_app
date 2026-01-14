package raegae.shark.attnow.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(attendance: Attendance)

    @Query("SELECT * FROM attendance WHERE studentId = :studentId")
    fun getAttendanceForStudent(studentId: Int): Flow<List<Attendance>>

    @Query("SELECT * FROM attendance")
    fun getAllAttendance(): Flow<List<Attendance>>

    @Query("DELETE FROM attendance WHERE studentId = :studentId AND date = :date")
    suspend fun deleteAttendance(studentId: Int, date: Long)
}
