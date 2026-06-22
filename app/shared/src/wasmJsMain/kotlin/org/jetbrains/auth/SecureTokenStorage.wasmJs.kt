package org.jetbrains.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

// Compose Web should not persist bearer tokens in browser storage.
// The backend now uses an httpOnly session cookie for web authentication.
@Composable
actual fun rememberSecureTokenStorage(): SecureTokenStorage = remember { WasmSecureTokenStorage() }

private class WasmSecureTokenStorage : SecureTokenStorage {
    override suspend fun readToken(): String? = null

    override suspend fun saveToken(token: String) = Unit

    override suspend fun clearToken() = Unit
}
