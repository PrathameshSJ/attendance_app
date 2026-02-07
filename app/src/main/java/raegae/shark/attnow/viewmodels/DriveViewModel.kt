package raegae.shark.attnow.viewmodels

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File as DriveFile
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import raegae.shark.attnow.data.drive.BackupManager
import raegae.shark.attnow.data.drive.DriveServiceHelper
import raegae.shark.attnow.data.drive.GoogleAuthManager

class DriveViewModel(application: Application) : AndroidViewModel(application) {

    val authManager = GoogleAuthManager(application)

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage

    private val _backups = MutableStateFlow<List<DriveFile>>(emptyList())
    val backups: StateFlow<List<DriveFile>> = _backups

    // Drive Logic
    private var driveHelper: DriveServiceHelper? = null
    private var backupManager: BackupManager? = null

    init {
        viewModelScope.launch {
            authManager.signedInAccount.collect { account ->
                try {
                    if (account != null) {
                        initializeDrive(account)
                    } else {
                        driveHelper = null
                        backupManager = null
                        _backups.value = emptyList()
                    }
                } catch (e: Throwable) {
                    android.util.Log.e("DriveViewModel", "Init failed", e)
                    _statusMessage.value = "Drive Init Failed: ${e.localizedMessage}"
                }
            }
        }
    }

    private fun initializeDrive(account: GoogleSignInAccount) {
        val credential =
                GoogleAccountCredential.usingOAuth2(
                        getApplication(),
                        listOf(DriveScopes.DRIVE_FILE)
                )
        credential.selectedAccount = account.account

        val drive =
                Drive.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance(), credential)
                        .setApplicationName("AttNow")
                        .build()

        driveHelper = DriveServiceHelper(drive)
        backupManager = BackupManager(driveHelper!!)

        refreshBackups()
    }

    fun handleSignInResult(intent: Intent?) {
        viewModelScope.launch { authManager.handleSignInResult(intent) }
    }

    fun refreshBackups() {
        viewModelScope.launch {
            backupManager?.let { bm ->
                _isBusy.value = true
                _backups.value = bm.getBackups().sortedByDescending { it.createdTime?.value ?: 0L }
                _isBusy.value = false
            }
        }
    }

    fun performBackup(localFile: File) {
        viewModelScope.launch {
            if (backupManager == null) return@launch
            _isBusy.value = true
            _statusMessage.value = "Uploading Backup..."

            try {
                backupManager!!.backup(localFile)
                _statusMessage.value = "Backup Successful"
                refreshBackups()
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: e.message ?: "Unknown Error"
                android.util.Log.e("DriveViewModel", "Backup failed", e)
                _statusMessage.value = "Backup Failed: $msg"
            }
            _isBusy.value = false
        }
    }

    fun performRestore(fileId: String, targetFile: File, onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (backupManager == null) return@launch
            _isBusy.value = true
            _statusMessage.value = "Downloading Backup..."

            val success = backupManager!!.restore(fileId, targetFile)

            if (success) {
                _statusMessage.value = "Restored. Initializing..."
                onSuccess()
            } else {
                _statusMessage.value = "Restore Failed"
                _isBusy.value = false // Call onSuccess handles busy state if success
            }
            // onSuccess handles busy state transition separately (restarting app potentially)
        }
    }

    fun clearStatus() {
        _statusMessage.value = null
    }
}
