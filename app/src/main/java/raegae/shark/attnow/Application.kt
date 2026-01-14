package raegae.shark.attnow

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun getApplication(): Application {
    return LocalContext.current.applicationContext as Application
}
