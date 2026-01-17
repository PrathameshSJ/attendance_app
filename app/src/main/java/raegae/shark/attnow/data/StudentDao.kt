package raegae.shark.attnow.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Insert
    suspend fun insert(student: Student)

    @androidx.room.Update
    suspend fun update(student: Student)

    @Query("SELECT * FROM student")
    fun getAllStudents(): Flow<List<Student>>

    @Query("SELECT * FROM student WHERE id = :studentId")
    fun getStudentById(studentId: Int): Flow<Student?>

    @Query("DELETE FROM student WHERE id = :id")
    suspend fun deleteStudentById(id: Int)

    @Query("DELETE FROM student WHERE id = :studentId")
    suspend fun deleteById(studentId: Int)

    
}
