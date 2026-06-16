package org.jetbrains.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.resources.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.realworld.article.ArticleWithoutBody
import org.jetbrains.realworld.article.ArticlesResource
import org.jetbrains.realworld.article.MultipleArticlesResponse
import org.jetbrains.realworld.error.GenericErrorModel

class FeedViewModel(
    private val client: HttpClient,
) : ViewModel() {
    private val _uiState = MutableStateFlow<FeedUiState>(FeedUiState.Loading(emptyList()))
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    fun loadFeed() {
        _uiState.update { state ->
            when (state) {
                is FeedUiState.Error -> FeedUiState.Loading(emptyList())
                is FeedUiState.Loading -> FeedUiState.Loading(state.articles)
                is FeedUiState.Success -> FeedUiState.Loading(state.articles)
            }
        }
        viewModelScope.launch {
            try {
                val response = client.get(ArticlesResource.Feed(ArticlesResource(), 20, 0))
                val state = if (response.status.isSuccess()) FeedUiState.Success(response.body<MultipleArticlesResponse>().articles)
                else FeedUiState.Error(response.body<GenericErrorModel>().message())
                _uiState.value = state
            } catch (e: Exception) {
                _uiState.value = FeedUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed interface FeedUiState {
    data class Loading(val articles: List<ArticleWithoutBody>) : FeedUiState
    data class Success(val articles: List<ArticleWithoutBody>) : FeedUiState
    data class Error(val message: String) : FeedUiState
}