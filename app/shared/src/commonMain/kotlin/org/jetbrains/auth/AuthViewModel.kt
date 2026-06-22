package org.jetbrains.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.HttpClient
import io.ktor.client.plugins.resources.get
import io.ktor.http.isSuccess
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthViewModel(
    private val client: HttpClient,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    val loginViewModel by lazy {
        LoginViewModel(client, authRepository) {
            _isAuthenticated.value = true
        }
    }

    val registerViewModel by lazy {
        RegisterViewModel(client, authRepository) {
            _isAuthenticated.value = true
        }
    }

    suspend fun refreshAuthState() {
        authRepository.loadPersistedToken()

        _isAuthenticated.value = client.get(org.jetbrains.realworld.user.UserResource()).status.isSuccess()
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.clearToken()
            _isAuthenticated.value = false
        }
    }
}
