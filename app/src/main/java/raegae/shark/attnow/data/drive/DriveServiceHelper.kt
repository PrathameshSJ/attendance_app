package raegae.shark.attnow.data.drive

import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File as DriveFile
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DriveServiceHelper(private val driveService: Drive) {

    suspend fun createFolderIfNotExist(folderName: String, parentId: String? = null): String? =
            withContext(Dispatchers.IO) {
                // Check if exists
                val query =
                        "mimeType = 'application/vnd.google-apps.folder' and name = '$folderName' and trashed = false" +
                                if (parentId != null) " and '$parentId' in parents" else ""

                val result =
                        driveService
                                .files()
                                .list()
                                .setQ(query)
                                .setSpaces("drive")
                                .setFields("files(id, name)")
                                .execute()

                if (result.files.isNotEmpty()) {
                    return@withContext result.files[0].id
                }

                // Create
                val metadata =
                        DriveFile()
                                .setName(folderName)
                                .setMimeType("application/vnd.google-apps.folder")

                if (parentId != null) {
                    metadata.parents = listOf(parentId)
                }

                val file = driveService.files().create(metadata).setFields("id").execute()

                file.id
            }

    suspend fun uploadFile(
            localFile: File,
            mimeType: String,
            folderId: String?,
            filename: String
    ): String =
            withContext(Dispatchers.IO) {
                val metadata = DriveFile().setName(filename)

                if (folderId != null) {
                    metadata.parents = listOf(folderId)
                }

                val content = FileContent(mimeType, localFile)

                val file = driveService.files().create(metadata, content).setFields("id").execute()

                file.id
            }

    suspend fun listFiles(folderId: String): List<DriveFile> =
            withContext(Dispatchers.IO) {
                val query = "'$folderId' in parents and trashed = false"
                // Order by createdTime desc to handle rotation logic appropriately if we relied on
                // time,
                // but we rely on name.

                val result =
                        driveService
                                .files()
                                .list()
                                .setQ(query)
                                .setOrderBy("name")
                                .setFields("files(id, name, createdTime, size)")
                                .execute()

                result.files
            }

    suspend fun downloadFile(fileId: String, targetFile: File) =
            withContext(Dispatchers.IO) {
                val outputStream = FileOutputStream(targetFile)
                driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream)
                outputStream.close()
            }

    suspend fun deleteFile(fileId: String) =
            withContext(Dispatchers.IO) { driveService.files().delete(fileId).execute() }

    suspend fun renameFile(fileId: String, newName: String) =
            withContext(Dispatchers.IO) {
                val metadata = DriveFile().setName(newName)
                driveService.files().update(fileId, metadata).execute()
            }
}
