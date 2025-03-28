package org.jetbrains

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.resources.Resources
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.detail.ArticleDetailViewModel
import org.jetbrains.home.ArticleListViewModel
import org.jetbrains.nav.RealWorldNavHost

@Composable
@Preview
fun App() {
    MaterialTheme {
        val client = HttpClient {
            // TODO turn into BuildConfig
            defaultRequest { url("http://localhost:8080") }
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

        LaunchedEffect(Unit) {
            listViewModel.loadArticles()
        }

        RealWorldNavHost(listViewModel, detailViewModel)
    }
}
