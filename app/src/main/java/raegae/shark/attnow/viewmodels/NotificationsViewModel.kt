package raegae.shark.attnow.viewmodels

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import raegae.shark.attnow.data.notifications.AppNotification
import raegae.shark.attnow.data.notifications.NotificationManager

class NotificationsViewModel(application: Application) : AndroidViewModel(application) {
    private val notificationManager = NotificationManager(application)
    private val prefs =
            application.getSharedPreferences("attnow_notifications", Context.MODE_PRIVATE)

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications = _notifications.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        refreshNotifications()
    }

    fun refreshNotifications() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val rawNotifications = notificationManager.getNotifications()

                // Apply "Read" status from Prefs
                val readIds = prefs.getStringSet("read_ids", emptySet()) ?: emptySet()

                val processed =
                        rawNotifications
                                .map { notif -> notif.copy(isRead = readIds.contains(notif.id)) }
                                .sortedBy { it.isRead } // Unread first

                _notifications.value = processed
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun markAsRead(notificationId: String) {
        val currentRead =
                prefs.getStringSet("read_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
        currentRead.add(notificationId)
        prefs.edit().putStringSet("read_ids", currentRead).apply()
        refreshNotifications()
    }

    fun markAllAsRead() {
        val allIds = _notifications.value.map { it.id }.toSet()
        val currentRead =
                prefs.getStringSet("read_ids", emptySet())?.toMutableSet() ?: mutableSetOf()
        currentRead.addAll(allIds)
        prefs.edit().putStringSet("read_ids", currentRead).apply()
        refreshNotifications()
    }

    fun getNotification(id: String): AppNotification? {
        return _notifications.value.find { it.id == id }
    }
}
