package org.jetbrains.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthRepository(private val secureTokenStorage: SecureTokenStorage) {
    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token.asStateFlow()

    val currentToken: String?
        get() = _token.value

    suspend fun loadPersistedToken() {
        _token.value = secureTokenStorage.readToken()
    }

    suspend fun saveToken(token: String) {
        secureTokenStorage.saveToken(token)
        _token.value = token
    }

    suspend fun clearToken() {
        secureTokenStorage.clearToken()
        _token.value = null
    }
}
