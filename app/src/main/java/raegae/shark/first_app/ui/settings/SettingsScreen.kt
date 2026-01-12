package raegae.shark.first_app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import raegae.shark.first_app.data.SettingsDataStore

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val settings = remember { SettingsDataStore(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val speed by settings.animationSpeed.collectAsState(initial = 1f)

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        Text("Animation speed: %.2fx".format(speed))

        Slider(
            value = speed,
            onValueChange = { newSpeed ->
                scope.launch {
                    settings.setAnimationSpeed(newSpeed)
                }
            },
            valueRange = 0.25f..2f,
            steps = 5
        )
    }
}
/* 
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val dataStore = remember { SettingsDataStore(context) }
    val scope = rememberCoroutineScope()

    val animationSpeed by dataStore.animationSpeed.collectAsState(initial = 1f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        Text("Animation Speed: ${"%.2f".format(animationSpeed)}x")

        Slider(
            value = animationSpeed,
            onValueChange = { newSpeed ->
                scope.launch {
                    dataStore.setAnimationSpeed(newSpeed)
                }
            },
            valueRange = 0.5f..3f,
            steps = 9
        )
    }
}
*/