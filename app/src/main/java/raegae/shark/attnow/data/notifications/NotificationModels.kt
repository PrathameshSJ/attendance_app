package raegae.shark.attnow.data.notifications

data class AppNotification(
        val id: String, // Unique ID (e.g., "name_subject_date")
        val title: String,
        val description: String,
        val studentName: String,
        val studentSubject: String,
        val type: NotificationType,
        val timestamp: Long,
        val isRead: Boolean = false,
        val studentPhoneNumber: String = ""
)

enum class NotificationType {
    WARNING_YELLOW, // 10 days or 2 classes left
    CRITICAL_RED // 5 days or 1 class left
}
