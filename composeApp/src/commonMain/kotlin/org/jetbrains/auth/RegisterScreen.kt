package org.jetbrains.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun RegisterContent(
    uiState: RegisterUiState,
    onRegister: (username: String, email: String, password: String) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    Scaffold(topBar = {
        TopAppBar {
            Text("Create Account")
        }
    }) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (uiState) {
                is RegisterUiState.Loading ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

                is RegisterUiState.Error ->
                    RegisterForm(
                        error = uiState.message,
                        onRegister = onRegister,
                        onNavigateToLogin = onNavigateToLogin
                    )

                is RegisterUiState.Initial ->
                    RegisterForm(
                        error = null,
                        onRegister = onRegister,
                        onNavigateToLogin = onNavigateToLogin
                    )

                is RegisterUiState.Success -> {
                    // Navigate to home screen is handled by the ViewModel
                }
            }
        }
    }
}

@Composable
private fun RegisterForm(
    error: String?,
    onRegister: (username: String, email: String, password: String) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Create your account",
            style = MaterialTheme.typography.h5
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colors.error,
                style = MaterialTheme.typography.body2,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { onRegister(username, email, password) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Account")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateToLogin) {
            Text("Already have an account? Sign in")
        }
    }
}

sealed interface RegisterUiState {
    data object Initial : RegisterUiState
    data object Loading : RegisterUiState
    data class Error(val message: String) : RegisterUiState
    data class Success(val username: String) : RegisterUiState
}