package org.jetbrains.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.core.content.edit

private const val SECURE_PREFS_NAME = "realworld_secure_auth"
private const val TOKEN_KEY = "auth_token"

@Composable
actual fun rememberSecureTokenStorage(): SecureTokenStorage {
    val context = LocalContext.current.applicationContext
    return remember(context) { AndroidSecureTokenStorage(context) }
}

private class AndroidSecureTokenStorage(context: Context) : SecureTokenStorage {
    private val preferences: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            SECURE_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override suspend fun readToken(): String? = preferences.getString(TOKEN_KEY, null)

    override suspend fun saveToken(token: String) {
        preferences.edit { putString(TOKEN_KEY, token) }
    }

    override suspend fun clearToken() {
        preferences.edit { remove(TOKEN_KEY) }
    }
}
