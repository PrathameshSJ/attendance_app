package raegae.shark.first_app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE) }
    var animationSpeed by remember { mutableFloatStateOf(prefs.getFloat("animation_speed", 1.0f)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Settings", style = MaterialTheme.typography.headlineMedium)

        Text(text = "Animation Speed: ${animationSpeed}x")

        Slider(
            value = animationSpeed,
            onValueChange = { animationSpeed = it },
            valueRange = 0f..2f,
            steps = 5
        )

        Button(onClick = {
            prefs.edit().putFloat("animation_speed", animationSpeed).apply()
        }) {
            Text("Save")
        }
    }
}