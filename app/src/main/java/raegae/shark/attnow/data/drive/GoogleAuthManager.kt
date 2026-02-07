package raegae.shark.attnow.data.drive

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

class GoogleAuthManager(private val context: Context) {
    private val _signedInAccount = MutableStateFlow<GoogleSignInAccount?>(null)
    val signedInAccount: StateFlow<GoogleSignInAccount?> = _signedInAccount

    private val googleSignInClient: GoogleSignInClient

    init {
        val gso =
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .requestScopes(Scope(DriveScopes.DRIVE_FILE))
                        // .requestScopes(Scope(DriveScopes.DRIVE_APPDATA)) // If we wanted app
                        // data, but FILE is better for user visibility
                        .build()
        googleSignInClient = GoogleSignIn.getClient(context, gso)

        // Check existing login
        _signedInAccount.value = GoogleSignIn.getLastSignedInAccount(context)
    }

    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    suspend fun handleSignInResult(intent: Intent?) {
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
            _signedInAccount.value = task.await()
        } catch (e: Exception) {
            e.printStackTrace()
            _signedInAccount.value = null
            throw e
        }
    }

    suspend fun signOut() {
        try {
            googleSignInClient.signOut().await()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _signedInAccount.value = null
        }
    }
}
