package raegae.shark.attnow.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(student: Student): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(students: List<Student>)

    @Update
    suspend fun update(student: Student)

    @Query("SELECT * FROM student")
    suspend fun getAll(): List<Student>

    @Query("SELECT * FROM student ORDER BY name ASC")
    fun getAllStudents(): Flow<List<Student>>

    @Query("SELECT * FROM student WHERE id = :studentId")
    fun getStudentById(studentId: Int): Flow<Student?>

    @Query("SELECT * FROM student WHERE id = :id LIMIT 1")
    suspend fun getStudentByIdOnce(id: Int): Student?

    @Query("DELETE FROM student WHERE id = :id")
    suspend fun deleteStudentById(id: Int)

    @Query("DELETE FROM student WHERE id = :studentId")
    suspend fun deleteById(studentId: Int)
    
    @Delete
    suspend fun delete(student: Student)

    @Query("SELECT * FROM student")
    suspend fun getAllStudentsOnce(): List<Student>

    @Query("""
        SELECT * FROM student 
        WHERE name = :name AND subject = :subject
    """)
    suspend fun findByNameAndSubject(
        name: String,
        subject: String
    ): List<Student>


    @Query("""
        DELETE FROM student 
        WHERE name = :name AND subject = :subject
    """)
    suspend fun deleteLogicalStudent(
        name: String,
        subject: String
    )
    
}
