package raegae.shark.attnow.ui.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import raegae.shark.attnow.data.notifications.AppNotification
import raegae.shark.attnow.data.notifications.NotificationType
import raegae.shark.attnow.viewmodels.NotificationsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
        navController: NavController,
        viewModel: NotificationsViewModel = viewModel()
) {
    val notifications by viewModel.notifications.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) { viewModel.refreshNotifications() }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("Notifications") },
                        actions = {
                            TextButton(onClick = { viewModel.markAllAsRead() }) {
                                Text("Mark All Read")
                            }
                        }
                )
            }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            if (notifications.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("No new notifications", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            } else {
                LazyColumn(contentPadding = padding, modifier = Modifier.fillMaxSize()) {
                    items(notifications) { notification ->
                        NotificationItem(
                                notification = notification,
                                onClick = {
                                    navController.navigate("notification_detail/${notification.id}")
                                },
                                onMarkRead = { viewModel.markAsRead(notification.id) }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(notification: AppNotification, onClick: () -> Unit, onMarkRead: () -> Unit) {
    val color =
            when (notification.type) {
                NotificationType.WARNING_YELLOW -> Color(0xFFFFC107) // Amber
                NotificationType.CRITICAL_RED -> Color(0xFFF44336) // Red
            }

    // "Marking it a little dark" if read
    val alpha = if (notification.isRead) 0.5f else 1.0f

    ListItem(
            modifier =
                    Modifier.clickable(onClick = onClick)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = alpha)),
            headlineContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // "small circle before title in the preview to show it priority colour"
                    if (!notification.isRead) {
                        Box(
                                Modifier.size(8.dp)
                                        .background(
                                                color,
                                                shape =
                                                        androidx.compose.foundation.shape
                                                                .CircleShape
                                        )
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                            notification.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style =
                                    if (notification.isRead) MaterialTheme.typography.bodyMedium
                                    else MaterialTheme.typography.titleMedium
                    )
                }
            },
            supportingContent = {
                Text(notification.description, maxLines = 2, overflow = TextOverflow.Ellipsis)
            },
            trailingContent = {
                if (!notification.isRead) {
                    IconButton(onClick = onMarkRead) { Icon(Icons.Default.Check, "Mark as Read") }
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
