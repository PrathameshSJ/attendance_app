package raegae.shark.first_app.ui.animation

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.runtime.Composable
import raegae.shark.first_app.ui.theme.LocalAnimationSpeed
import androidx.compose.ui.unit.IntOffset


@Composable
fun scaledFloatTween(
    baseMillis: Int,
    easing: Easing = FastOutSlowInEasing
): FiniteAnimationSpec<Float> {
    val speed = LocalAnimationSpeed.current
    return tween(
        durationMillis = (baseMillis / speed).toInt().coerceAtLeast(1),
        easing = easing
    )
}

fun scaledOffsetTween(
    baseMillis: Int,
    speed: Float
): FiniteAnimationSpec<IntOffset> {
    return tween(
        durationMillis = (baseMillis / speed).toInt().coerceAtLeast(1)
    )
}
