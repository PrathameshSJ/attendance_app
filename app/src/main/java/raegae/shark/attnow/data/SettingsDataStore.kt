package raegae.shark.attnow.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import java.util.Calendar

private val Context.dataStore by preferencesDataStore("settings")

class SettingsDataStore(private val context: Context) {

    private val ANIMATION_SPEED = floatPreferencesKey("animation_speed")

    val animationSpeed: Flow<Float> = context.dataStore.data.map {
        it[ANIMATION_SPEED] ?: 1f
    }

    suspend fun setAnimationSpeed(speed: Float) {
        context.dataStore.edit {
            it[ANIMATION_SPEED] = speed
        }
    }

    companion object {
        private val PINNED_STUDENTS = stringSetPreferencesKey("pinned_students")
        private val LAST_CLEAR_DAY = longPreferencesKey("last_clear_day")
    }

    private fun todayKey(): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    fun pinnedStudentsForToday(): Flow<Set<Int>> =
        context.dataStore.data.map { prefs ->

            val today = todayKey()
            val last = prefs[LAST_CLEAR_DAY] ?: 0L

            // If day changed → treat as empty
            if (last != today) {
                emptySet()
            } else {
                prefs[PINNED_STUDENTS]
                    ?.map { it.toInt() }
                    ?.toSet()
                    ?: emptySet()
            }
        }
    


 
    val pinnedStudents: Flow<Set<Int>> = context.dataStore.data.map { prefs ->
        val today = todayKey()
        val last = prefs[LAST_CLEAR_DAY] ?: 0L

        if (last != today) {
            emptySet()   // we clear lazily in edit(), not inside map()
        } else {
            prefs[PINNED_STUDENTS]?.map { it.toInt() }?.toSet() ?: emptySet()
        }
    }.onEach {
        val today = todayKey()
        context.dataStore.edit { prefs ->
            val last = prefs[LAST_CLEAR_DAY] ?: 0L
            if (last != today) {
                prefs[PINNED_STUDENTS] = emptySet()
                prefs[LAST_CLEAR_DAY] = today
            }
        }
    }

/*
    val pinnedStudents: Flow<Set<Int>> = context.dataStore.data.map { prefs ->
        val today = todayKey()
        val last = prefs[LAST_CLEAR_DAY] ?: 0L

        if (last != today) {
            context.dataStore.edit {
                it[PINNED_STUDENTS] = emptySet()
                it[LAST_CLEAR_DAY] = today
            }
            emptySet()
        } else {
            prefs[PINNED_STUDENTS]?.map { it.toInt() }?.toSet() ?: emptySet()
        }
    }
   

*/

    suspend fun ensureToday() {
        val today = todayKey()

        context.dataStore.edit { prefs ->
            val last = prefs[LAST_CLEAR_DAY] ?: 0L
            if (last != today) {
                prefs[PINNED_STUDENTS] = emptySet()
                prefs[LAST_CLEAR_DAY] = today
            }
        }
    }


    suspend fun addPinned(id: Int) {
        context.dataStore.edit { prefs ->
            val set = prefs[PINNED_STUDENTS]?.toMutableSet() ?: mutableSetOf()
            set.add(id.toString())
            prefs[PINNED_STUDENTS] = set
        }
    }

    suspend fun removePinned(id: Int) {
        context.dataStore.edit { prefs ->
            val set = prefs[PINNED_STUDENTS]?.toMutableSet() ?: mutableSetOf()
            set.remove(id.toString())
            prefs[PINNED_STUDENTS] = set
        }
    }
        
}
