package org.jetbrains.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.get
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.realworld.error.GenericErrorModel
import org.jetbrains.realworld.user.User
import org.jetbrains.realworld.user.UserResource
import org.jetbrains.realworld.user.UserResponse

class ProfileViewModel(
    private val client: HttpClient,
) : ViewModel() {
    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun loadProfile() {
        _uiState.value = ProfileUiState.Loading
        viewModelScope.launch {
            try {
                val response = client.get(UserResource())
                val state = if (response.status.isSuccess()) {
                    ProfileUiState.Success(response.body<UserResponse>().user)
                } else {
                    ProfileUiState.Error(response.body<GenericErrorModel>().message())
                }
                _uiState.value = state
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Success(val user: User) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}