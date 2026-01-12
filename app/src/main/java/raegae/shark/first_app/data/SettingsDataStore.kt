package raegae.shark.first_app.data

import android.content.Context
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
}
