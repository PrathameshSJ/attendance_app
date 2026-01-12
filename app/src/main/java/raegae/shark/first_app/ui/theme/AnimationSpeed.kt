package raegae.shark.first_app.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import raegae.shark.first_app.data.SettingsDataStore

val LocalAnimationSpeed = compositionLocalOf { 1f }

@Composable
fun ProvideAnimationSpeed(
    dataStore: SettingsDataStore,
    content: @Composable () -> Unit
) {
    val speed by dataStore.animationSpeed.collectAsState(initial = 1f)

    androidx.compose.runtime.CompositionLocalProvider(
        LocalAnimationSpeed provides speed,
        content = content
    )
}

@Composable
fun animationSpeed(): Float = LocalAnimationSpeed.current
