package org.jetbrains.home

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
import org.jetbrains.home.ArticleListUiState.Error
import org.jetbrains.home.ArticleListUiState.Loading
import org.jetbrains.home.ArticleListUiState.Success

class ArticleListViewModel(private val client: HttpClient) : ViewModel() {
    private val _uiState = MutableStateFlow<ArticleListUiState>(Loading(emptyList()))
    val uiState: StateFlow<ArticleListUiState> = _uiState.asStateFlow()

    fun loadArticles() {
        _uiState.update { state ->
            when (state) {
                is Error -> Loading(emptyList())
                is Loading -> Loading(state.articles)
                is Success -> Loading(state.articles)
            }
        }
        viewModelScope.launch {
            try {
                val response = client.get(ArticlesResource()) {
                    parameter("limit", 20)
                    parameter("offset", 0)
                }
                val state = if (response.status.isSuccess()) Success(response.body<MultipleArticlesResponse>().articles)
                else Error(response.body<GenericErrorModel>().message())
                _uiState.value = state
            } catch (e: Exception) {
                _uiState.value = Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed interface ArticleListUiState {
    data class Loading(val articles: List<ArticleWithoutBody>) : ArticleListUiState
    data class Success(val articles: List<ArticleWithoutBody>) : ArticleListUiState
    data class Error(val message: String) : ArticleListUiState
}
