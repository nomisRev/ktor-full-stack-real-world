package org.jetbrains.auth

import androidx.compose.runtime.Composable

interface SecureTokenStorage {
    suspend fun readToken(): String?
    suspend fun saveToken(token: String)
    suspend fun clearToken()
}

@Composable
expect fun rememberSecureTokenStorage(): SecureTokenStorage
