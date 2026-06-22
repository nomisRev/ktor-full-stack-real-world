package org.jetbrains.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.realworld.error.GenericErrorModel
import org.jetbrains.realworld.user.UserLogin
import org.jetbrains.realworld.user.UserLoginRequest
import org.jetbrains.realworld.user.UserResponse
import org.jetbrains.realworld.user.UsersResource

class LoginViewModel(
    private val client: HttpClient,
    private val authRepository: AuthRepository,
    private val onLoginSuccess: () -> Unit
) : ViewModel() {
    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Initial)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = LoginUiState.Error("Email and password are required")
            return
        }

        _uiState.value = LoginUiState.Loading

        viewModelScope.launch {
            try {
                val response = client.post(UsersResource.Login(UsersResource())) {
                    contentType(ContentType.Application.Json)
                    setBody(UserLoginRequest(UserLogin(email, password)))
                }

                if (response.status.isSuccess()) {
                    val userResponse = response.body<UserResponse>()
                    val token = requireNotNull(userResponse.user.token)
                    authRepository.saveToken(token)
                    _uiState.value = LoginUiState.Success(userResponse.user.username)
                    onLoginSuccess()
                } else {
                    val errorResponse = response.body<GenericErrorModel>()
                    _uiState.value = LoginUiState.Error(errorResponse.message())
                }
                // TODO we're swallowing CancellationException
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }
}