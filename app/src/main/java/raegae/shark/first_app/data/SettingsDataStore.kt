package raegae.shark.first_app.data

import android.content.Context
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.datastore.core.DataStore

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
    private val PINNED_STUDENTS = stringSetPreferencesKey("pinned_students")

    val pinnedStudents: Flow<Set<Int>> =
        context.dataStore.data.map { prefs ->
            prefs[PINNED_STUDENTS]?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
        }

    suspend fun setPinnedStudents(ids: Set<Int>) {
        context.dataStore.edit { prefs ->
            prefs[PINNED_STUDENTS] = ids.map { it.toString() }.toSet()
        }
    }
    
    suspend fun addPinned(studentId: Int) {
        context.dataStore.edit { prefs ->
            val current = prefs[PINNED_STUDENTS]?.map { it.toInt() }?.toMutableSet() ?: mutableSetOf()
            current.add(studentId)
            prefs[PINNED_STUDENTS] = current.map { it.toString() }.toSet()
        }
    }

    suspend fun removePinned(studentId: Int) {
        context.dataStore.edit { prefs ->
            val current = prefs[PINNED_STUDENTS]?.map { it.toInt() }?.toMutableSet() ?: mutableSetOf()
            current.remove(studentId)
            prefs[PINNED_STUDENTS] = current.map { it.toString() }.toSet()
        }
    }


}
