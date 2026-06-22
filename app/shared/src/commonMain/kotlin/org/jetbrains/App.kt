package org.jetbrains

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.resources.Resources
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.jetbrains.auth.AuthRepository
import org.jetbrains.auth.AuthViewModel
import org.jetbrains.auth.rememberSecureTokenStorage
import org.jetbrains.detail.ArticleDetailViewModel
import org.jetbrains.feed.FeedViewModel
import org.jetbrains.home.ArticleListViewModel
import org.jetbrains.nav.RealWorldNavHost
import org.jetbrains.profile.ProfileViewModel

@Composable
@Preview
fun App() {
    MaterialTheme {
        val secureTokenStorage = rememberSecureTokenStorage()
        val authRepository = remember(secureTokenStorage) { AuthRepository(secureTokenStorage) }

        val client = HttpClient {
            // TODO turn into BuildConfig
            defaultRequest {
                url("http://localhost:8080")
                authRepository.currentToken?.let { header(HttpHeaders.Authorization, "Token $it") }
            }
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            install(Resources)
        }

        val listViewModel = ArticleListViewModel(client)
        val detailViewModel = ArticleDetailViewModel(client)
        val authViewModel = AuthViewModel(client, authRepository)
        val feedViewModel = FeedViewModel(client)
        val profileViewModel = ProfileViewModel(client)

        LaunchedEffect(Unit) {
            authViewModel.refreshAuthState()
        }

        RealWorldNavHost(
            listViewModel = listViewModel,
            detailViewModel = detailViewModel,
            authViewModel = authViewModel,
            feedViewModel = feedViewModel,
            profileViewModel = profileViewModel
        )
    }
}
