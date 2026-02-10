package raegae.shark.attnow.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Student::class, Attendance::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studentDao(): StudentDao
    abstract fun attendanceDao(): AttendanceDao

    companion object {
        val MIGRATION_1_2 =
                object : androidx.room.migration.Migration(1, 2) {
                    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                        database.execSQL(
                                "ALTER TABLE student ADD COLUMN max_days INTEGER NOT NULL DEFAULT 0"
                        )
                    }
                }

        val MIGRATION_2_3 =
                object : androidx.room.migration.Migration(2, 3) {
                    override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                        database.execSQL(
                                "ALTER TABLE student ADD COLUMN phoneNumber TEXT NOT NULL DEFAULT ''"
                        )
                    }
                }

        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE
                    ?: synchronized(this) {
                        val instance =
                                Room.databaseBuilder(
                                                context.applicationContext,
                                                AppDatabase::class.java,
                                                "attnow.db"
                                        )
                                        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                                        .build()

                        INSTANCE = instance
                        instance
                    }
        }
    }
}
