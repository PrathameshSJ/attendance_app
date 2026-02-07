package raegae.shark.attnow.data.drive

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackupManager(private val driveService: DriveServiceHelper) {

    companion object {
        private const val FOLDER_NAME = "Attendance"
        private const val BASE_FILENAME = "Attendance"
        private const val EXTENSION = ".xlsx"
        private const val MAX_BACKUPS = 5
    }

    suspend fun backup(localFile: File) =
            withContext(Dispatchers.IO) {
                // 1. Get/Create Folder
                val folderId =
                        driveService.createFolderIfNotExist(FOLDER_NAME)
                                ?: throw Exception("Failed to create/find '$FOLDER_NAME' folder.")

                // 2. List Existing Files
                val files = driveService.listFiles(folderId)

                // Map Name -> FileId
                // We want to identify: Attendance.xlsx, Attendance_1.xlsx, ...
                // Attendance_4.xlsx
                // Logic:
                // Delete Attendance_4.xlsx
                // Rename Attendance_3 -> Attendance_4
                // Rename Attendance_2 -> Attendance_3
                // Rename Attendance_1 -> Attendance_2
                // Rename Attendance -> Attendance_1
                // Upload New -> Attendance

                // Helper to get name for index. 0 = Base
                fun getName(index: Int): String {
                    return if (index == 0) "$BASE_FILENAME$EXTENSION"
                    else "${BASE_FILENAME}_$index$EXTENSION"
                }

                // A. Delete Max
                val maxName = getName(MAX_BACKUPS - 1)
                files.find { it.name == maxName }?.let { driveService.deleteFile(it.id) }

                // B. Shift Down (Iterate backwards from Max-1 to 0)
                for (i in (MAX_BACKUPS - 2) downTo 0) {
                    val currentName = getName(i)
                    val targetName = getName(i + 1)

                    val fileToRename = files.find { it.name == currentName }
                    // Warning: files list is stale if we deleted one? No, we deleted 'maxName'
                    // which is irrelevant here.
                    // But if we renamed '3' to '4', does 'files' list update? No.
                    // WE rely on IDs captured in start 'files'.

                    if (fileToRename != null) {
                        driveService.renameFile(fileToRename.id, targetName)
                    }
                }

                // C. Upload New File
                driveService.uploadFile(
                        localFile,
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        folderId,
                        getName(0)
                )
            }

    suspend fun getBackups(): List<com.google.api.services.drive.model.File> =
            withContext(Dispatchers.IO) {
                try {
                    val folderId =
                            driveService.createFolderIfNotExist(FOLDER_NAME)
                                    ?: return@withContext emptyList()
                    // Return all files in folder, let UI sort or filter
                    driveService.listFiles(folderId)
                } catch (e: Exception) {
                    emptyList()
                }
            }

    suspend fun restore(fileId: String, targetFile: File): Boolean =
            withContext(Dispatchers.IO) {
                try {
                    driveService.downloadFile(fileId, targetFile)
                    true
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
            }
}
