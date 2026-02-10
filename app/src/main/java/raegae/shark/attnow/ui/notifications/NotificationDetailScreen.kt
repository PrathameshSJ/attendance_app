package raegae.shark.attnow.ui.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import raegae.shark.attnow.viewmodels.NotificationsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationDetailScreen(
        navController: NavController,
        notificationId: String,
        viewModel: NotificationsViewModel
) {
        val notifications by viewModel.notifications.collectAsState()
        val isLoading by viewModel.isLoading.collectAsState()
        val notification = notifications.find { it.id == notificationId }
        val context = androidx.compose.ui.platform.LocalContext.current

        Scaffold(
                topBar = {
                        TopAppBar(
                                title = { Text("Details") },
                                navigationIcon = {
                                        IconButton(
                                                onClick = { navController.popBackStack() }
                                        ) { // Back button beside title logic
                                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                                        }
                                }
                        )
                }
        ) { padding ->
                if (isLoading) {
                        Box(
                                Modifier.fillMaxSize().padding(padding),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                        ) { CircularProgressIndicator() }
                } else if (notification == null) {
                        Box(
                                Modifier.fillMaxSize().padding(padding),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                        ) { Text("Notification not found") }
                } else {
                        val dateStr =
                                remember(notification.timestamp) {
                                        java.text.SimpleDateFormat(
                                                        "MMM dd, yyyy h:mm a",
                                                        java.util.Locale.getDefault()
                                                )
                                                .format(java.util.Date(notification.timestamp))
                                }

                        Column(
                                Modifier.padding(padding).fillMaxSize().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                                // "title is on a bigger font"
                                Text(
                                        notification.title,
                                        style = MaterialTheme.typography.displaySmall
                                )

                                Text(
                                        dateStr,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                HorizontalDivider()

                                // "below it a description"
                                Text(
                                        notification.description,
                                        style = MaterialTheme.typography.bodyLarge
                                )

                                // WhatsApp Button if phone number exists
                                val phoneNumber = notification.studentPhoneNumber
                                if (phoneNumber.isNotBlank() && phoneNumber.trim() != "null") {
                                        Button(
                                                onClick = {
                                                        try {
                                                                // "types a prewritten message in
                                                                // the
                                                                // chatbox"
                                                                val message =
                                                                        "Hi, regarding your subscription: ${notification.description}"
                                                                val encodedMessage =
                                                                        java.net.URLEncoder.encode(
                                                                                message,
                                                                                "UTF-8"
                                                                        )

                                                                // "strict int of 10 digit...
                                                                // default to
                                                                // prepending +91"
                                                                val cleanNumber = phoneNumber.trim()
                                                                val fullNumber =
                                                                        if (!cleanNumber.startsWith(
                                                                                        "+"
                                                                                )
                                                                        )
                                                                                "+91$cleanNumber"
                                                                        else cleanNumber

                                                                val intent =
                                                                        android.content.Intent(
                                                                                android.content
                                                                                        .Intent
                                                                                        .ACTION_VIEW,
                                                                                android.net.Uri
                                                                                        .parse(
                                                                                                "https://api.whatsapp.com/send?phone=$fullNumber&text=$encodedMessage"
                                                                                        )
                                                                        )
                                                                context.startActivity(intent)
                                                        } catch (e: Exception) {
                                                                android.widget.Toast.makeText(
                                                                                context,
                                                                                "WhatsApp not installed or invalid number",
                                                                                android.widget.Toast
                                                                                        .LENGTH_SHORT
                                                                        )
                                                                        .show()
                                                        }
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors =
                                                        ButtonDefaults.buttonColors(
                                                                containerColor =
                                                                        androidx.compose.ui.graphics
                                                                                .Color(0xFF25D366)
                                                        )
                                        ) { Text("Contact on WhatsApp") }
                                }

                                Spacer(Modifier.weight(1f))

                                // "action buttons below it"
                                // "add a mark as read button for now"
                                if (!notification.isRead) {
                                        Button(
                                                onClick = {
                                                        viewModel.markAsRead(notificationId)
                                                        navController.popBackStack()
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                        ) { Text("Mark as Read") }
                                } else {
                                        OutlinedButton(
                                                onClick = { navController.popBackStack() },
                                                modifier = Modifier.fillMaxWidth()
                                        ) { Text("Close") }
                                }
                        }
                }
        }
}
