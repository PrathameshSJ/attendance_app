package raegae.shark.attnow.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import raegae.shark.attnow.data.util.StudentKey
import java.util.Calendar

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    /* ---------- Animation ---------- */

    private val ANIMATION_SPEED = floatPreferencesKey("animation_speed")

    val animationSpeed: Flow<Float> =
        context.dataStore.data.map { prefs ->
            prefs[ANIMATION_SPEED] ?: 1f
        }

    suspend fun setAnimationSpeed(speed: Float) {
        context.dataStore.edit { prefs ->
            prefs[ANIMATION_SPEED] = speed
        }
    }

    /* ---------- Pinning (LOGICAL students) ---------- */

    companion object {
        private val PINNED_KEYS = stringSetPreferencesKey("pinned_students")
        private val LAST_CLEAR_DAY = longPreferencesKey("last_clear_day")
    }

    private fun todayKey(): Long =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    /**
     * Emits pinned logical students (StudentKey) for TODAY only.
     * Auto-clears when day changes.
     */
    val pinnedStudents: Flow<Set<StudentKey>> =
        context.dataStore.data
            .map { prefs ->
                val today = todayKey()
                val last = prefs[LAST_CLEAR_DAY] ?: 0L

                if (last != today) {
                    emptySet()
                } else {
                    prefs[PINNED_KEYS]
                        ?.mapNotNull { StudentKey.fromString(it) }
                        ?.toSet()
                        ?: emptySet()
                }
            }
            .onEach {
                // lazy daily reset (SAFE)
                context.dataStore.edit { prefs ->
                    val today = todayKey()
                    val last = prefs[LAST_CLEAR_DAY] ?: 0L

                    if (last != today) {
                        prefs[PINNED_KEYS] = emptySet()
                        prefs[LAST_CLEAR_DAY] = today
                    }
                }
            }

    suspend fun addPinned(key: StudentKey) {
        context.dataStore.edit { prefs ->
            val set = prefs[PINNED_KEYS]?.toMutableSet() ?: mutableSetOf()
            set.add(key.serialize())
            prefs[PINNED_KEYS] = set
            prefs[LAST_CLEAR_DAY] = todayKey()
        }
    }

    suspend fun removePinned(key: StudentKey) {
        context.dataStore.edit { prefs ->
            val set = prefs[PINNED_KEYS]?.toMutableSet() ?: mutableSetOf()
            set.remove(key.serialize())
            prefs[PINNED_KEYS] = set
        }
    }
}
