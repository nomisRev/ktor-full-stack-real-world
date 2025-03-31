package org.jetbrains.auth

import androidx.lifecycle.ViewModel
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthViewModel(private val client: HttpClient) : ViewModel() {
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    val loginViewModel by lazy {
        LoginViewModel(client) {
            _isAuthenticated.value = true
        }
    }

    val registerViewModel by lazy {
        RegisterViewModel(client) {
            _isAuthenticated.value = true
        }
    }

    fun logout() {
        _isAuthenticated.value = false
    }
}
