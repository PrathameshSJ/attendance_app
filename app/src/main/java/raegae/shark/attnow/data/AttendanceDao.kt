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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(list: List<Attendance>)

    @Query("SELECT * FROM attendance WHERE studentId IN (:studentIds) ORDER BY date ASC")
    fun getAttendanceForStudents(studentIds: List<Int>): Flow<List<Attendance>>

    @Query("SELECT * FROM attendance WHERE studentId IN (:studentIds) ORDER BY date ASC")
    suspend fun getAttendanceForStudentsOnce(studentIds: List<Int>): List<Attendance>

    @Query("SELECT * FROM attendance WHERE studentId = :studentId ORDER BY date ASC")
    fun getAttendanceForStudent(studentId: Int): Flow<List<Attendance>>

    @Query("SELECT * FROM attendance")
    fun getAllAttendance(): Flow<List<Attendance>>

    @Query("SELECT * FROM attendance")
    suspend fun getAllAttendanceOnce(): List<Attendance>


    @Query("DELETE FROM attendance WHERE studentId = :studentId AND date = :date")
    suspend fun deleteAttendance(studentId: Int, date: Long)

    @Query(
        """
        DELETE FROM attendance
        WHERE studentId = :studentId
        """
    )
    suspend fun deleteAllForStudent(studentId: Int)

    @Query(
        """
        DELETE FROM attendance
        WHERE studentId IN (
            SELECT id FROM student
            WHERE name = :name AND subject = :subject
        )
        """
    )
    suspend fun deleteForLogicalStudent(
        name: String,
        subject: String
    )
}
