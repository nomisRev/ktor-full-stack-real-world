package org.jetbrains.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.browser.window

private const val TOKEN_KEY = "org.jetbrains.realworld.auth.token"

// TODO: This needs to become session based
@Composable
actual fun rememberSecureTokenStorage(): SecureTokenStorage = remember { WasmSecureTokenStorage() }

private class WasmSecureTokenStorage : SecureTokenStorage {
    override suspend fun readToken(): String? = window.localStorage.getItem(TOKEN_KEY)

    override suspend fun saveToken(token: String) {
        window.localStorage.setItem(TOKEN_KEY, token)
    }

    override suspend fun clearToken() {
        window.localStorage.removeItem(TOKEN_KEY)
    }
}
