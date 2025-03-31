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
import org.jetbrains.realworld.user.NewUser
import org.jetbrains.realworld.user.NewUserRequest
import org.jetbrains.realworld.user.UserResponse
import org.jetbrains.realworld.user.UsersResource

class RegisterViewModel(
    private val client: HttpClient,
    private val onRegistrationSuccess: () -> Unit
) : ViewModel() {
    private val _uiState = MutableStateFlow<RegisterUiState>(RegisterUiState.Initial)
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun register(username: String, email: String, password: String) {
        // TODO properly report on a per-field basis such that they can properly be highlighted
        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.value = RegisterUiState.Error("All fields are required")
            return
        }

        _uiState.value = RegisterUiState.Loading

        viewModelScope.launch {
            try {
                val response = client.post(UsersResource()) {
                    contentType(ContentType.Application.Json)
                    setBody(NewUserRequest(NewUser(username, email, password)))
                }

                if (response.status.isSuccess()) {
                    val userResponse = response.body<UserResponse>()
                    _uiState.value = RegisterUiState.Success(userResponse.user.username)
                    onRegistrationSuccess()
                } else {
                    val errorResponse = response.body<GenericErrorModel>()
                    _uiState.value = RegisterUiState.Error(errorResponse.message())
                }
            // TODO we're swallowing CancellationException
            } catch (e: Exception) {
                _uiState.value = RegisterUiState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }
}